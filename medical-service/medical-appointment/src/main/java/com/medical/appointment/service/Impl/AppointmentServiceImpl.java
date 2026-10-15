package com.medical.appointment.service.Impl;

import com.medical.appointment.mapper.AppointmentMapper;
import com.medical.appointment.service.AppointmentService;
import com.medical.feign.department.IDepartmentClient;
import com.medical.feign.doctor.IDoctorClient;
import com.medical.feign.patient.IPatientClient;
import com.medical.model.appointment.Appointment;
import com.medical.model.common.dtos.Result;
import com.medical.model.department.Department;
import com.medical.model.doctor.Doctor;
import com.medical.appointment.config.RabbitMQConfig;
import com.medical.model.patient.Patient;
import com.medical.model.message.AppointmentSuccessMessage;
import com.medical.utils.constance.RedisConstants;
import com.medical.utils.core.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private IPatientClient iPatientClient;

    @Autowired
    private IDoctorClient iDoctorClient;

    @Autowired
    private IDepartmentClient iDepartmentClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // region 今日号源统计缓存（降低高频统计读对 DB 的压力）

    private String todayStatKey(Integer doctorId) {
        return RedisConstants.APPOINTMENT_TODAY_KEY.getStringValue() + doctorId;
    }

    /**
     * 今日已约数：优先读 Redis 缓存，缓存缺失时回源 DB 并短 TTL 回填。
     * 仅用于展示统计（getDoctorTodayStat / available）；下单判号仍走 bookedToday 读库，
     * 保证串行化校验不超卖。
     */
    private long todayBookedCached(Integer doctorId) {
        String key = todayStatKey(doctorId);
        Object cached = RedisUtils.getValue(key);
        if (cached != null) {
            try {
                return Long.parseLong(cached.toString());
            } catch (Exception e) {
                // 缓存值异常视为失效，回源 DB
                RedisUtils.delKey(key);
            }
        }
        long val = bookedToday(doctorId);
        RedisUtils.setValueTimeout(key, val, RedisConstants.APPOINTMENT_TODAY_TTL.getLongValue(), TimeUnit.SECONDS);
        return val;
    }

    /**
     * 号源/预约变更后失效今日统计缓存，保证下次展示读到最新数据
     */
    private void invalidateTodayStat(Integer doctorId) {
        if (doctorId != null) {
            RedisUtils.delKey(todayStatKey(doctorId));
        }
    }
    // endregion

    // region 号源防超卖（Redisson 分布式锁 + 数据库计数）

    /**
     * 今日该医生已约数：以数据库未取消预约数为准（在分布式锁内读取并下单，保证不超卖）
     */
    private long bookedToday(Integer doctorId) {
        return appointmentMapper.countActiveByDoctor(doctorId);
    }

    /**
     * 号源分布式锁 key：appointment:slot:{doctorId}:{yyyyMMdd}:lock
     */
    private String slotLockKey(Integer doctorId) {
        String ymd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return RedisConstants.APPOINTMENT_SLOT_KEY.getStringValue() + doctorId + ":" + ymd;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createAppointment(Appointment appointment, Integer userId) {
        // 根据登录用户找到患者档案
        Patient patientForUser = iPatientClient.findUserId(userId);
        if (patientForUser == null) {
            return Result.error("未找到当前用户的患者档案，请先完善患者信息");
        }
        appointment.setPatientId(patientForUser.getPatientId());

        Patient patient = iPatientClient.findPatientById(appointment.getPatientId());
        if (patient == null) {
            return Result.error("无效的患者ID");
        }

        Department department = iDepartmentClient.findDepartmentById(appointment.getDepartmentId());
        if (department == null) {
            return Result.error("无效的科室ID");
        }

        Doctor doctor = iDoctorClient.findDoctorById(appointment.getDoctorId());
        if (doctor == null) {
            return Result.error("无效的医生ID");
        }
        if (!"在职".equals(doctor.getEmploymentStatus())) {
            return Result.error("该医生当前不在职，暂时无法预约");
        }

        // 防超卖：高并发（节假日/春节等）场景下用 Redisson 分布式锁串行化「校验 + 下单」，
        // 在读库与写入之间加锁，避免同一时刻并发预约超过医生的每日最大号源 maxAppointmentsPerDay
        Integer max = doctor.getMaxAppointmentsPerDay();
        RLock lock = (max != null && max > 0)
                ? redissonClient.getLock(slotLockKey(appointment.getDoctorId())) : null;
        boolean locked = false;
        try {
            if (lock != null) {
                locked = lock.tryLock(3, TimeUnit.SECONDS);
                if (!locked) {
                    return Result.error("系统繁忙，请稍后重试");
                }
                if (bookedToday(appointment.getDoctorId()) >= max) {
                    return Result.error("该医生今日号源已满，请选择其他医生或改期再约");
                }
            }

            appointment.setDepartmentName(department.getName());
            appointment.setCampus(department.getCampus());
            appointment.setAddress(department.getAddress());
            appointment.setCode(generateCode(LocalDateTime.now(), department.getId()));
            appointment.setAppointmentDate(java.sql.Date.valueOf(LocalDate.now()));
            appointment.setCreateTime(LocalDateTime.now());
            appointment.setUpdateTime(LocalDateTime.now());

            appointmentMapper.insert(appointment);
            invalidateTodayStat(appointment.getDoctorId());
            log.info("创建预约：id={}, patientId={}, doctorId={}", appointment.getId(), appointment.getPatientId(), appointment.getDoctorId());

            // 预约成功通知：通过 RabbitMQ 发布，由 message 服务异步消费写入患者站内消息，解耦且不阻塞下单
            publishAppointmentSuccess(appointment, patient, doctor, department);

            return Result.success("预约创建成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取号源分布式锁被中断：doctorId={}", appointment.getDoctorId(), e);
            return Result.error("系统繁忙，请稍后重试");
        } finally {
            if (lock != null && locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 发布「预约成功通知」消息：丢到 RabbitMQ，由 message 服务异步写站内消息。
     */
    private void publishAppointmentSuccess(Appointment appointment, Patient patient, Doctor doctor, Department department) {
        if (patient == null || patient.getUserId() == null) {
            log.warn("患者账号缺失，跳过预约成功通知：patientId={}", appointment.getPatientId());
            return;
        }
        AppointmentSuccessMessage msg = new AppointmentSuccessMessage();
        msg.setUserId(patient.getUserId());
        // 医生通知：医生账号存在性由消息字段承载，消费端若 doctorUserId 为空则不写医生通知
        msg.setDoctorId(doctor == null ? null : doctor.getDoctorId());
        msg.setDoctorUserId(doctor == null ? null : doctor.getUserId());
        msg.setNotifyDoctor(doctor != null && doctor.getUserId() != null);
        msg.setDoctorName(doctor == null ? "" : doctor.getUsername());
        msg.setDepartmentName(department == null ? "" : department.getName());
        msg.setAppointmentDate(appointment.getAppointmentDate() == null
                ? null : toLocalDate(appointment.getAppointmentDate()).toString());
        msg.setCode(appointment.getCode());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.APPOINTMENT_SUCCESS_ROUTING,
                    msg);
            log.info("预约成功消息已发布：patientId={}, userId={}", appointment.getPatientId(), patient.getUserId());
        } catch (Exception e) {
            log.error("发布预约成功消息失败：doctorId={}, patientId={}",
                    appointment.getDoctorId(), appointment.getPatientId(), e);
        }
    }

    private String generateCode(LocalDateTime dateTime, Integer departmentId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = dateTime.format(formatter);
        return dateStr + "-" + departmentId;
    }

    /** java.util.Date → LocalDate（appointmentDate 字段为 java.util.Date） */
    private LocalDate toLocalDate(java.util.Date date) {
        return date == null ? null : new java.sql.Date(date.getTime()).toLocalDate();
    }

    @Override
    public void updateAppointment(Appointment appointment) {
        // 更新时不应篡改创建时间
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
        log.info("更新预约：id={}", appointment.getId());
    }

    @Override
    public Result deleteAppointment(Integer id, Integer userId) {
        Appointment exist = appointmentMapper.selectById(id);
        if (exist == null) {
            return Result.error("预约不存在");
        }
        // 越权防护：非管理员仅能取消本人（预约归属患者账号）的预约
        if (!isAdmin() && !belongsToUser(exist, userId)) {
            return Result.error("无权取消他人的预约，仅可取消本人预约");
        }
        // 就诊当天不允许取消当日预约号：避免号源已排、爽约影响叫号
        if (exist.getAppointmentDate() != null
                && toLocalDate(exist.getAppointmentDate()).equals(LocalDate.now())) {
            return Result.error("就诊当天不能取消当日预约号");
        }
        appointmentMapper.deleteById(id);
        invalidateTodayStat(exist.getDoctorId());
        log.info("删除预约：id={}, userId={}", id, userId);
        return Result.success("预约删除成功");
    }

    /** 预约是否归属当前登录用户（通过预约的 patientId 关联患者档案的 user_id 判断） */
    private boolean belongsToUser(Appointment appointment, Integer userId) {
        if (userId == null || appointment.getPatientId() == null) {
            return false;
        }
        try {
            Patient patient = iPatientClient.findPatientById(appointment.getPatientId());
            return patient != null && userId.equals(patient.getUserId());
        } catch (Exception e) {
            log.warn("校验预约归属失败：appointmentId={}, userId={}", appointment.getId(), userId, e);
            return false;
        }
    }

    /** 当前登录用户是否管理员（LoginInterceptor 已把 JWT role 注入 SecurityContextHolder） */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
    }

    /**
     * 取消某医生所有未取消的预约并释放号源（医生请假时触发），返回受影响的预约数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelByDoctor(Integer doctorId) {
        List<Appointment> list = appointmentMapper.findByDoctorActive(doctorId);
        if (list == null || list.isEmpty()) {
            return Result.success(0);
        }
        LocalDateTime now = LocalDateTime.now();
        for (Appointment a : list) {
            a.setStatus("已取消");
            a.setUpdateTime(now);
            appointmentMapper.updateById(a);
        }
        invalidateTodayStat(doctorId);
        log.info("医生请假取消预约：doctorId={}, 共 {} 条", doctorId, list.size());
        return Result.success(list.size());
    }

    @Override
    public List<Appointment> findByDoctor(Integer doctorId) {
        return appointmentMapper.findByDoctorActive(doctorId);
    }

    /**
     * 医生端预约列表：按当前登录医生账号（userId）反查医生档案，
     * 返回该医生未取消的预约（联表带患者/科室信息），供医生查看自己的接诊安排。
     */
    @Override
    public Result getAppointmentsByDoctorUser(Integer userId) {
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }
        Doctor doctor = iDoctorClient.findUserId(userId);
        if (doctor == null || doctor.getDoctorId() == null) {
            // 非医生账号：返回成功 + 空列表，避免前端误弹「未关联医生档案」错误
            return Result.success(new ArrayList<>());
        }
        List<Appointment> list = appointmentMapper.findDetailByDoctorId(doctor.getDoctorId());
        return Result.success(list == null ? new ArrayList<>() : list);
    }

    /**
     * 医生端今日号源统计：返回当前医生的每日号源上限与今日已约数；
     * 非医生账号返回成功且 doctorId 为 null，前端据此隐藏医生视角区块
     */
    @Override
    public Result getDoctorTodayStat(Integer userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("doctorId", null);
        data.put("doctorName", "");
        data.put("max", null);
        data.put("todayBooked", 0L);
        data.put("remaining", null);
        if (userId == null) {
            return Result.success(data);
        }
        Doctor doctor = iDoctorClient.findUserId(userId);
        if (doctor == null || doctor.getDoctorId() == null) {
            return Result.success(data);
        }
        long todayBooked = todayBookedCached(doctor.getDoctorId());
        data.put("doctorId", doctor.getDoctorId());
        data.put("doctorName", doctor.getUsername());
        data.put("max", doctor.getMaxAppointmentsPerDay());
        data.put("todayBooked", todayBooked);
        data.put("remaining", doctor.getMaxAppointmentsPerDay() == null ? null
                : Math.max(0, doctor.getMaxAppointmentsPerDay() - todayBooked));
        return Result.success(data);
    }

    /**
     * 查询某医生已取消的预约（Feign 内部调用，医生请假通知补偿定时任务用）。
     */
    @Override
    public List<Appointment> findCancelledByDoctor(Integer doctorId) {
        return appointmentMapper.findByDoctorCancelled(doctorId);
    }

    /**
     * 推荐同科室仍有号源的医生（请假补救）：返回在职且今日余号大于0的医生列表
     */
    @Override
    public Result available(Integer departmentId, Integer excludeDoctorId) {
        if (departmentId == null) {
            return Result.error("科室ID不能为空");
        }
        List<Doctor> doctors = iDoctorClient.findByDepartment(departmentId);
        Map<Integer, Boolean> exclude = new java.util.HashMap<>();
        if (excludeDoctorId != null) {
            exclude.put(excludeDoctorId, true);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (doctors != null) {
            for (Doctor d : doctors) {
                if (d == null || !"在职".equals(d.getEmploymentStatus())) {
                    continue;
                }
                if (exclude.containsKey(d.getDoctorId())) {
                    continue;
                }
                Integer max = d.getMaxAppointmentsPerDay();
                if (max == null || max <= 0) {
                    continue;
                }
                long remain = max - todayBookedCached(d.getDoctorId());
                if (remain <= 0) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("doctorId", d.getDoctorId());
                item.put("doctorName", d.getUsername());
                item.put("title", d.getTitle());
                item.put("remaining", remain);
                item.put("max", max);
                result.add(item);
            }
        }
        return Result.success(result);
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        // 删除前先取回各预约的 doctorId，用于失效号源统计缓存
        List<Appointment> deleted = appointmentMapper.selectBatchIds(ids);
        if (deleted != null) {
            deleted.forEach(a -> invalidateTodayStat(a.getDoctorId()));
        }
        int rows = appointmentMapper.deleteBatchIds(ids);
        log.info("批量删除预约：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }

    @Override
    public Result getAppointmentsByUserId(Integer userId) {
        Patient patient = iPatientClient.findUserId(userId);
        if (patient == null) {
            return Result.success("没有找到此预约");
        }
        return getAppointments(patient.getPatientId());
    }

    @Override
    public Result getApUserId(Integer patientId) {
        return getAppointments(patientId);
    }

    /**
     * 联表查询患者预约列表（带患者姓名、医生姓名、科室信息），一次查询替代循环 Feign 调用
     */
    private Result getAppointments(Integer patientId) {
        List<Appointment> appointments = appointmentMapper.findDetailByPatientId(patientId);
        if (appointments != null && !appointments.isEmpty()) {
            return Result.success(appointments);
        }
        return Result.success("没有找到此预约");
    }
}
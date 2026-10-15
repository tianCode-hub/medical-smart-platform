package com.medical.doctor.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.doctor.config.RabbitMQConfig;
import com.medical.doctor.mapper.DoctorMapper;
import com.medical.doctor.service.DoctorService;
import com.medical.feign.appointment.IAppointmentClient;
import com.medical.feign.patient.IPatientClient;
import com.medical.model.appointment.Appointment;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.doctor.Doctor;
import com.medical.model.message.DoctorLeaveMessage;
import com.medical.model.message.DoctorRecommendInfo;
import com.medical.model.patient.Patient;
import com.medical.utils.thread.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IAppointmentClient iAppointmentClient;

    @Autowired
    private IPatientClient iPatientClient;

    @Override
    public PageBean<Doctor> findAll(Integer pageNum, Integer pageSize, String keyword, Integer departmentId) {
        // 联表查询直接带出用户名和科室名，避免循环 Feign 调用的 N+1 问题
        PageHelper.startPage(pageNum, pageSize);
        List<Doctor> doctors = doctorMapper.selectDoctorList(keyword, departmentId);
        Page<Doctor> page = (Page<Doctor>) doctors;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public void deleteUser(Integer id) {
        doctorMapper.deleteById(id);
        log.info("删除医生：id={}", id);
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        int rows = doctorMapper.deleteBatchIds(ids);
        log.info("批量删除医生：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }

    @Override
    public void update(Doctor doctor) {
        doctorMapper.updateById(doctor);
        log.info("更新医生：doctorId={}", doctor.getDoctorId());
    }

    @Override
    public Doctor getById(Integer doctorId) {
        // 联表带出 user.username 登录名，保证预约成功通知/前端展示的医生名与登录名一致
        return doctorMapper.selectByIdWithUser(doctorId);
    }

    @Override
    public Doctor getUserId(Integer userId) {
        // 按 user_id 关联查医生档案（联表带出 user.username 登录名，保证与预约界面医生名一致）
        return doctorMapper.selectByUserId(userId);
    }

    @Override
    public List<Doctor> getByDepartment(Integer departmentId) {
        return doctorMapper.selectDoctorList(null, departmentId);
    }

    @Override
    public Result getdepartment(Integer departmentId) {
        List<Doctor> doctors = doctorMapper.selectDoctorList(null, departmentId);
        if (doctors != null && !doctors.isEmpty()) {
            return Result.success(doctors);
        } else {
            return Result.success("没有找到医生");
        }
    }

    /**
     * 医生紧急请假（节假日/病假等突发缺勤补救）：
     * 1. 置在职状态为请假，前端不再推荐/无法继续挂号；
     * 2. 取消该医生所有未就诊预约并释放号源；
     * 3. 向已挂号患者批量发送站内消息，提示改约同科室其他医生。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result leave(Integer doctorId) {
        // 越权防护：非管理员仅能对本人（doctorId = 当前登录用户 id）请假
        if (!isAdmin()) {
            Integer loginId = currentUserId();
            if (loginId == null || !loginId.equals(doctorId)) {
                return Result.error("无权操作他人请假，仅可对本人请假");
            }
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            return Result.error("医生不存在");
        }

        Doctor upd = new Doctor();
        upd.setDoctorId(doctorId);
        upd.setEmploymentStatus("请假");
        doctorMapper.updateById(upd);

        // 已挂号患者（未取消的预约）
        List<Appointment> list = iAppointmentClient.findByDoctor(doctorId);
        Integer cancelled = iAppointmentClient.cancelByDoctor(doctorId);

        // 聚合去重后的患者用户，发送站内消息通知
        Set<Integer> distinctUserIds = new HashSet<>();
        if (list != null) {
            for (Appointment a : list) {
                if (a.getPatientId() == null) {
                    continue;
                }
                Patient p = iPatientClient.findPatientById(a.getPatientId());
                if (p != null && p.getUserId() != null) {
                    distinctUserIds.add(p.getUserId());
                }
            }
        }

        // 查询同科室仍有余号的可改约医生，随通知一并提供给患者
        List<DoctorRecommendInfo> recommendDoctors = new ArrayList<>();
        try {
            List<Map<String, Object>> recs = iAppointmentClient.available(doctor.getDepartmentId(), doctorId);
            if (recs != null) {
                for (Map<String, Object> r : recs) {
                    if (r == null || r.get("doctorId") == null) {
                        continue;
                    }
                    DoctorRecommendInfo info = new DoctorRecommendInfo();
                    info.setDoctorId(((Number) r.get("doctorId")).intValue());
                    info.setDoctorName((String) r.get("doctorName"));
                    info.setTitle((String) r.get("title"));
                    info.setRemaining(r.get("remaining") instanceof Number ? ((Number) r.get("remaining")).longValue() : null);
                    info.setMax(r.get("max") instanceof Number ? ((Number) r.get("max")).intValue() : null);
                    recommendDoctors.add(info);
                }
            }
        } catch (Exception e) {
            log.warn("查询同科室可改约医生失败：doctorId={}", doctorId, e);
        }

        // 通过 RabbitMQ 发布医生请假通知，由 message 服务异步消费并写入站内消息
        DoctorLeaveMessage msg = new DoctorLeaveMessage();
        msg.setDoctorId(doctorId);
        msg.setDoctorName(doctor.getUsername());
        msg.setUserIds(new ArrayList<>(distinctUserIds));
        msg.setRecommendDoctors(recommendDoctors);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.DOCTOR_LEAVE_ROUTING,
                    msg);
        } catch (Exception e) {
            log.error("发布医生请假消息失败：doctorId={}", doctorId, e);
        }

        log.info("医生请假处理：doctorId={}, 医生={}, 取消预约={}, 待通知患者={}, 已发布RabbitMQ消息",
                doctorId, doctor.getUsername(), cancelled, distinctUserIds.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("doctorName", doctor.getUsername());
        data.put("cancelled", cancelled);
        data.put("notified", distinctUserIds.size());
        return Result.success(data);
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

    /** 当前登录用户 id（从 ThreadLocal claims 获取） */
    private Integer currentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Object uid = claims == null ? null : claims.get("id");
            return uid == null ? null : ((Number) uid).intValue();
        } catch (Exception e) {
            return null;
        }
    }
}

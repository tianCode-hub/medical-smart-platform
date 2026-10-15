package com.medical.patient.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.patient.Patient;
import com.medical.patient.mapper.PatientMapper;
import com.medical.patient.service.PatientService;
import com.medical.utils.thread.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientMapper patientMapper;

    @Override
    public PageBean<Patient> findAll(Integer pageNum, Integer pageSize, String keyword) {
        // 联表查询直接带出用户名、电话、当前用药名称，避免循环 Feign 调用的 N+1 问题
        PageHelper.startPage(pageNum, pageSize);
        List<Patient> patients = patientMapper.selectPatientList(keyword);
        Page<Patient> page = (Page<Patient>) patients;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public Result insert(Patient patient) {
        // 绑定当前登录用户：从 ThreadLocal 取登录 userId/username，确保 patient.user_id 与 user 表关联，
        // 医生端预约列表 / 患者中心才能展示出患者姓名（联表取 user.username）
        bindCurrentUser(patient);
        patientMapper.insert(patient);
        log.info("新增患者：patientId={}", patient.getPatientId());
        return Result.success("患者新增成功");
    }

    @Override
    public Result update(Patient patient, Integer id) {
        // 越权防护：非管理员仅能修改本人档案（目标档案 user_id 必须等于当前登录用户）
        if (!isAdmin()) {
            Patient exist = patientMapper.selectById(id);
            if (exist == null) {
                return Result.error("患者档案不存在");
            }
            Integer loginId = currentUserId();
            if (loginId == null || exist.getUserId() == null || !loginId.equals(exist.getUserId())) {
                return Result.error("无权修改他人信息，仅可修改本人档案");
            }
        }
        patient.setPatientId(id);
        // 更新时同样绑定/校正当前登录用户关联，避免历史档案 user_id 为空导致姓名联不上的问题
        bindCurrentUser(patient);
        patientMapper.updateById(patient);
        log.info("更新患者：patientId={}", id);
        return Result.success("患者更新成功");
    }

    /**
     * 从 ThreadLocal（登录拦截器写入的 JWT claims）取出当前登录用户 id 与 username，
     * 绑定到患者档案的 user_id / username，保证后续联表能查到患者姓名。
     */
    private void bindCurrentUser(Patient patient) {
        if (patient == null) {
            return;
        }
        // 管理员维护档案时保留原有 user_id 关联，不强制覆盖为管理员自己
        if (isAdmin()) {
            return;
        }
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Object uid = claims == null ? null : claims.get("id");
            if (uid != null) {
                patient.setUserId(((Number) uid).intValue());
            }
            // 患者档案未填姓名时，回填登录用户名，保证医生端能展示患者姓名
            if (!StringUtils.hasLength(patient.getUsername()) && claims != null) {
                String loginName = (String) claims.get("username");
                if (StringUtils.hasLength(loginName)) {
                    patient.setUsername(loginName);
                }
            }
        } catch (Exception e) {
            log.warn("绑定当前登录用户到患者档案失败：{}", e.getMessage());
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

    @Override
    public Result delete(int patientId) {
        patientMapper.deleteById(patientId);
        log.info("删除患者：patientId={}", patientId);
        return Result.success("患者删除成功");
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        int rows = patientMapper.deleteBatchIds(ids);
        log.info("批量删除患者：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }

    @Override
    public Patient getById(Integer patientId) {
        return patientMapper.selectById(patientId);
    }

    @Override
    public Patient findUserId(Integer userId) {
        return patientMapper.selectOne(new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, userId));
    }
}

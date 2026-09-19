package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.entity.HrCandidate;
import cn.aiedge.hr.entity.HrRecruitment;
import cn.aiedge.hr.mapper.HrCandidateMapper;
import cn.aiedge.hr.mapper.HrRecruitmentMapper;
import cn.aiedge.hr.service.HrCandidateService;
import cn.aiedge.hr.service.HrEmployeeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 候选人服务实现
 *
 * <p>状态机：0-简历筛选 → 1-初试 → 2-复试 → 3-终面 → 4-待录用 → 5-已录用 → 7-已入职；
 * 6-已拒绝可在任意面试阶段进入。`status = 7`（已入职）由 {@link #hireToEmployee} 写入，
 * 并**同时创建 `hr_employee` 档案**——这正是此前完全断裂的「招聘 → 入职 → 员工档案」闭环。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrCandidateServiceImpl extends ServiceImpl<HrCandidateMapper, HrCandidate>
        implements HrCandidateService {

    private static final int STATUS_HIRED = 5;
    private static final int STATUS_ONBOARD = 7;

    private final HrRecruitmentMapper recruitmentMapper;
    private final HrEmployeeService employeeService;

    @Override
    public Page<HrCandidate> pageList(Page<HrCandidate> page, Long tenantId,
                                       Long recruitmentId, String name, Integer status) {
        LambdaQueryWrapper<HrCandidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrCandidate::getTenantId, tenantId)
               .eq(recruitmentId != null, HrCandidate::getRecruitmentId, recruitmentId)
               .like(StringUtils.hasText(name), HrCandidate::getName, name)
               .eq(status != null, HrCandidate::getStatus, status)
               .orderByDesc(HrCandidate::getCreateTime)
               .orderByDesc(HrCandidate::getId);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCandidate(HrCandidate candidate) {
        if (!StringUtils.hasText(candidate.getName())) {
            throw new BusinessException("候选人姓名不能为空");
        }
        if (candidate.getRecruitmentId() == null) {
            throw new BusinessException("请选择应聘的招聘职位");
        }
        HrRecruitment recruitment = recruitmentMapper.selectById(candidate.getRecruitmentId());
        if (recruitment == null) {
            throw new BusinessException("招聘职位不存在");
        }
        if (candidate.getStatus() == null) {
            candidate.setStatus(0);
        }
        candidate.setTenantId(SecurityUtils.getCurrentTenantId());
        candidate.setCreateTime(LocalDateTime.now());
        candidate.setUpdateTime(LocalDateTime.now());
        save(candidate);
        // 维护招聘职位的应聘人数
        recruitmentMapper.update(null, new LambdaUpdateWrapper<HrRecruitment>()
                .eq(HrRecruitment::getId, recruitment.getId())
                .set(HrRecruitment::getApplicantCount,
                        (recruitment.getApplicantCount() == null ? 0 : recruitment.getApplicantCount()) + 1)
                .set(HrRecruitment::getUpdateTime, LocalDateTime.now()));
        log.info("创建候选人: name={}, recruitmentId={}", candidate.getName(), candidate.getRecruitmentId());
        return candidate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidate(HrCandidate candidate) {
        if (candidate.getId() == null) {
            throw new BusinessException("候选人ID不能为空");
        }
        HrCandidate exists = getById(candidate.getId());
        if (exists == null) {
            throw new BusinessException("候选人不存在");
        }
        if (exists.getStatus() != null && exists.getStatus() == STATUS_ONBOARD) {
            throw new BusinessException("已入职的候选人不能修改");
        }
        candidate.setStatus(null);
        candidate.setRecruitmentId(null);
        candidate.setUpdateTime(LocalDateTime.now());
        updateById(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        HrCandidate candidate = require(id);
        if (status == STATUS_ONBOARD) {
            throw new BusinessException("「已入职」须走「转入职」动作（会同时建档），不能直接改状态");
        }
        if (candidate.getStatus() != null && candidate.getStatus() == STATUS_ONBOARD) {
            throw new BusinessException("已入职的候选人不能变更状态");
        }
        LambdaUpdateWrapper<HrCandidate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrCandidate::getId, id)
               .set(HrCandidate::getStatus, status)
               .set(HrCandidate::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        // 录用（5）时维护招聘职位的录用人数
        if (status == STATUS_HIRED) {
            syncHiredCount(candidate.getRecruitmentId());
        }
        log.info("更新候选人状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordInterview(Long id, Long interviewerId, String interviewerName,
                                String interviewComment, Integer rating) {
        require(id);
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException("面试评分必须在 1 ~ 5 之间");
        }
        LambdaUpdateWrapper<HrCandidate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrCandidate::getId, id)
               .set(HrCandidate::getInterviewerId, interviewerId)
               .set(HrCandidate::getInterviewerName, interviewerName)
               .set(HrCandidate::getInterviewComment, interviewComment)
               .set(HrCandidate::getRating, rating)
               .set(HrCandidate::getInterviewTime, LocalDateTime.now())
               .set(HrCandidate::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("记录面试评价: id={}, interviewer={}, rating={}", id, interviewerName, rating);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long hireToEmployee(Long candidateId, HrEmployee employee) {
        HrCandidate candidate = require(candidateId);
        if (candidate.getStatus() != null && candidate.getStatus() == STATUS_ONBOARD) {
            throw new BusinessException("该候选人已办理入职，不能重复办理");
        }
        HrEmployee payload = employee == null ? new HrEmployee() : employee;
        if (!StringUtils.hasText(payload.getEmployeeName())) {
            payload.setEmployeeName(candidate.getName());
        }
        if (payload.getGender() == null) {
            payload.setGender(candidate.getGender());
        }
        if (!StringUtils.hasText(payload.getPhone())) {
            payload.setPhone(candidate.getPhone());
        }
        if (!StringUtils.hasText(payload.getEmail())) {
            payload.setEmail(candidate.getEmail());
        }
        if (payload.getBirthDate() == null) {
            payload.setBirthDate(candidate.getBirthDate());
        }
        if (payload.getEducation() == null) {
            payload.setEducation(candidate.getEducation());
        }
        if (!StringUtils.hasText(payload.getSchool())) {
            payload.setSchool(candidate.getSchool());
        }
        if (!StringUtils.hasText(payload.getMajor())) {
            payload.setMajor(candidate.getMajor());
        }
        if (payload.getHireDate() == null) {
            payload.setHireDate(LocalDate.now());
        }
        Long employeeId = employeeService.createEmployee(payload);

        LambdaUpdateWrapper<HrCandidate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrCandidate::getId, candidateId)
               .set(HrCandidate::getStatus, STATUS_ONBOARD)
               .set(HrCandidate::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        syncHiredCount(candidate.getRecruitmentId());
        log.info("候选人转入职: candidateId={}, employeeId={}", candidateId, employeeId);
        return employeeId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCandidate(Long candidateId) {
        HrCandidate candidate = require(candidateId);
        if (candidate.getStatus() != null && candidate.getStatus() == STATUS_ONBOARD) {
            throw new BusinessException("已入职的候选人不能删除（其员工档案已生成）");
        }
        removeById(candidateId);
        // 与 createCandidate 的 +1 对称：删除后把应聘人数收回，否则计数会单调膨胀
        if (candidate.getRecruitmentId() != null) {
            HrRecruitment recruitment = recruitmentMapper.selectById(candidate.getRecruitmentId());
            if (recruitment != null) {
                int next = Math.max(0, (recruitment.getApplicantCount() == null ? 0 : recruitment.getApplicantCount()) - 1);
                recruitmentMapper.update(null, new LambdaUpdateWrapper<HrRecruitment>()
                        .eq(HrRecruitment::getId, recruitment.getId())
                        .set(HrRecruitment::getApplicantCount, next)
                        .set(HrRecruitment::getUpdateTime, LocalDateTime.now()));
            }
        }
        syncHiredCount(candidate.getRecruitmentId());
        log.info("删除候选人: id={}, name={}", candidateId, candidate.getName());
    }

    @Override
    public Map<String, Object> statistics(Long recruitmentId) {
        LambdaQueryWrapper<HrCandidate> wrapper = new LambdaQueryWrapper<HrCandidate>()
                .eq(recruitmentId != null, HrCandidate::getRecruitmentId, recruitmentId);
        java.util.List<HrCandidate> rows = list(wrapper);
        Map<String, Object> stage = new LinkedHashMap<>();
        for (int i = 0; i <= 7; i++) {
            final int s = i;
            stage.put(String.valueOf(i), rows.stream().filter(c -> c.getStatus() != null && c.getStatus() == s).count());
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", rows.size());
        stat.put("stageCount", stage);
        return stat;
    }

    private void syncHiredCount(Long recruitmentId) {
        if (recruitmentId == null) {
            return;
        }
        Long hired = baseMapper.selectCount(new LambdaQueryWrapper<HrCandidate>()
                .eq(HrCandidate::getRecruitmentId, recruitmentId)
                .in(HrCandidate::getStatus, STATUS_HIRED, STATUS_ONBOARD));
        recruitmentMapper.update(null, new LambdaUpdateWrapper<HrRecruitment>()
                .eq(HrRecruitment::getId, recruitmentId)
                .set(HrRecruitment::getHiredCount, hired == null ? 0 : hired.intValue())
                .set(HrRecruitment::getUpdateTime, LocalDateTime.now()));
    }

    private HrCandidate require(Long id) {
        HrCandidate candidate = id == null ? null : getById(id);
        if (candidate == null) {
            throw new BusinessException("候选人不存在");
        }
        return candidate;
    }
}

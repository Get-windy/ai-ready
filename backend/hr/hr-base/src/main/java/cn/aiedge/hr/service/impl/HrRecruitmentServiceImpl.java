package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.entity.HrRecruitment;
import cn.aiedge.hr.mapper.HrRecruitmentMapper;
import cn.aiedge.hr.service.HrRecruitmentService;
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
import java.util.List;
import java.util.Map;

/**
 * 招聘职位服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrRecruitmentServiceImpl extends ServiceImpl<HrRecruitmentMapper, HrRecruitment>
        implements HrRecruitmentService {

    private final HrLookupHelper lookupHelper;

    @Override
    public Page<HrRecruitment> pageList(Page<HrRecruitment> page, Long tenantId,
                                         Integer status, String positionName, String channel,
                                         String startDate, String endDate) {
        Page<HrRecruitment> result = page(page, buildWrapper(tenantId, status, positionName, channel, startDate, endDate));
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrRecruitment> listForExport(Integer status, String positionName, String channel,
                                             String startDate, String endDate) {
        List<HrRecruitment> rows = list(buildWrapper(null, status, positionName, channel, startDate, endDate));
        enrich(rows);
        return rows;
    }

    private LambdaQueryWrapper<HrRecruitment> buildWrapper(Long tenantId, Integer status,
                                                           String positionName, String channel,
                                                           String startDate, String endDate) {
        LambdaQueryWrapper<HrRecruitment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrRecruitment::getTenantId, tenantId)
                .eq(status != null, HrRecruitment::getStatus, status)
                .like(StringUtils.hasText(positionName), HrRecruitment::getPositionName, positionName)
                .eq(StringUtils.hasText(channel), HrRecruitment::getChannel, channel);
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(HrRecruitment::getPublishDate, parseLocalDate(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(HrRecruitment::getPublishDate, parseLocalDate(endDate));
        }
        wrapper.orderByDesc(HrRecruitment::getCreateTime).orderByDesc(HrRecruitment::getId);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecruitment(HrRecruitment recruitment) {
        if (!StringUtils.hasText(recruitment.getPositionName())) {
            throw new BusinessException("请填写招聘岗位名称");
        }
        recruitment.setTenantId(SecurityUtils.getCurrentTenantId());
        recruitment.setPublisherId(SecurityUtils.getCurrentUserId());
        recruitment.setPublisherName(SecurityUtils.getCurrentUsername());
        if (recruitment.getPublishDate() == null) {
            recruitment.setPublishDate(LocalDate.now());
        }
        if (recruitment.getStatus() == null) {
            recruitment.setStatus(0);
        }
        recruitment.setApplicantCount(0);
        recruitment.setHiredCount(0);
        recruitment.setCreateTime(LocalDateTime.now());
        recruitment.setUpdateTime(LocalDateTime.now());
        save(recruitment);
        log.info("创建招聘职位: name={}, headcount={}", recruitment.getPositionName(), recruitment.getHeadcount());
        return recruitment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecruitment(HrRecruitment recruitment) {
        if (recruitment.getId() == null) {
            throw new BusinessException("招聘职位ID不能为空");
        }
        HrRecruitment exists = getById(recruitment.getId());
        if (exists == null) {
            throw new BusinessException("招聘职位不存在");
        }
        // 状态与计数为派生字段，只允许经 updateStatus / 候选人流程变更
        recruitment.setStatus(null);
        recruitment.setApplicantCount(null);
        recruitment.setHiredCount(null);
        recruitment.setUpdateTime(LocalDateTime.now());
        updateById(recruitment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        HrRecruitment exists = id == null ? null : getById(id);
        if (exists == null) {
            throw new BusinessException("招聘职位不存在");
        }
        LambdaUpdateWrapper<HrRecruitment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrRecruitment::getId, id)
               .set(HrRecruitment::getStatus, status)
               .set(HrRecruitment::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("更新招聘职位状态: id={}, status={}", id, status);
    }

    @Override
    public Map<String, Object> statistics(Integer status) {
        List<HrRecruitment> rows = list(new LambdaQueryWrapper<HrRecruitment>()
                .eq(status != null, HrRecruitment::getStatus, status));
        long recruiting = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count();
        long finished = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == 3).count();
        long headcount = rows.stream().mapToLong(r -> r.getHeadcount() == null ? 0 : r.getHeadcount()).sum();
        long applicants = rows.stream().mapToLong(r -> r.getApplicantCount() == null ? 0 : r.getApplicantCount()).sum();
        long hired = rows.stream().mapToLong(r -> r.getHiredCount() == null ? 0 : r.getHiredCount()).sum();

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", rows.size());
        stat.put("recruitingCount", recruiting);
        stat.put("finishedCount", finished);
        stat.put("headcountTotal", headcount);
        stat.put("applicantTotal", applicants);
        stat.put("hiredTotal", hired);
        stat.put("vacancyTotal", Math.max(0, headcount - hired));
        return stat;
    }

    /** 回填部门名（部门名可能来自快照列，缺失时按 dept_id 补齐） */
    private void enrich(List<HrRecruitment> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, String> deptNames = lookupHelper.deptNames(rows.stream()
                .map(HrRecruitment::getDeptId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList()));
        for (HrRecruitment r : rows) {
            if (!StringUtils.hasText(r.getDeptName()) && r.getDeptId() != null) {
                r.setDeptName(deptNames.get(r.getDeptId()));
            }
        }
    }

    /**
     * 安全解析日期字符串为 LocalDate，避免 PostgreSQL 类型不匹配
     */
    private LocalDate parseLocalDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            throw new BusinessException("日期格式错误: " + dateString + "，期望格式: yyyy-MM-dd");
        }
    }
}

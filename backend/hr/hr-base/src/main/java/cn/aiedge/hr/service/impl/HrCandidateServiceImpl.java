package cn.aiedge.hr.service.impl;

import cn.aiedge.hr.entity.HrCandidate;
import cn.aiedge.hr.mapper.HrCandidateMapper;
import cn.aiedge.hr.service.HrCandidateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 候选人服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrCandidateServiceImpl extends ServiceImpl<HrCandidateMapper, HrCandidate>
        implements HrCandidateService {

    @Override
    public Page<HrCandidate> pageList(Page<HrCandidate> page, Long tenantId,
                                       Long recruitmentId, String name, Integer status) {
        LambdaQueryWrapper<HrCandidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrCandidate::getTenantId, tenantId)
               .eq(recruitmentId != null, HrCandidate::getRecruitmentId, recruitmentId)
               .like(name != null && !name.isEmpty(), HrCandidate::getName, name)
               .eq(status != null, HrCandidate::getStatus, status)
               .orderByDesc(HrCandidate::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<HrCandidate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrCandidate::getId, id)
               .set(HrCandidate::getStatus, status)
               .set(HrCandidate::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("更新候选人状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional
    public void recordInterview(Long id, Long interviewerId, String interviewerName,
                                String interviewComment, Integer rating) {
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
}

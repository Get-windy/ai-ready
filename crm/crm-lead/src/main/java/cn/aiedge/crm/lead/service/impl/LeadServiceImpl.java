package cn.aiedge.crm.lead.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.lead.entity.Lead;
import cn.aiedge.crm.lead.mapper.LeadMapper;
import cn.aiedge.crm.lead.service.LeadService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadServiceImpl extends ServiceImpl<LeadMapper, Lead> 
        implements LeadService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLead(Lead lead) {
        lead.setStatus(0); // 新线索
        lead.setFollowCount(0);
        lead.setScore(50); // 默认评分
        lead.setCreateTime(LocalDateTime.now());
        lead.setUpdateTime(LocalDateTime.now());
        lead.setCreateBy(StpUtil.getLoginIdAsLong());
        
        save(lead);
        log.info("创建线索成功: leadId={}, name={}", lead.getId(), lead.getName());
        return lead.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLead(Lead lead) {
        lead.setUpdateTime(LocalDateTime.now());
        updateById(lead);
        log.info("更新线索成功: leadId={}", lead.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLead(Long leadId) {
        removeById(leadId);
        log.info("删除线索成功: leadId={}", leadId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignLead(Long leadId, Long ownerId) {
        Lead lead = getById(leadId);
        if (lead == null) {
            throw BusinessException.notFound("线索不存在");
        }
        
        lead.setOwnerId(ownerId);
        lead.setStatus(1); // 跟进中
        lead.setUpdateTime(LocalDateTime.now());
        updateById(lead);
        log.info("分配线索成功: leadId={}, ownerId={}", leadId, ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssignLeads(List<Long> leadIds, Long ownerId) {
        for (Long leadId : leadIds) {
            assignLead(leadId, ownerId);
        }
        log.info("批量分配线索成功: leadIds={}, ownerId={}", leadIds, ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToCustomer(Long leadId) {
        Lead lead = getById(leadId);
        if (lead == null) {
            throw BusinessException.notFound("线索不存在");
        }
        if (lead.getStatus() == 2) {
            throw BusinessException.badRequest("线索已转化");
        }
        
        // TODO: 创建客户记录
        Long customerId = null;
        
        lead.setStatus(2); // 已转化
        lead.setConvertedCustomerId(customerId);
        lead.setConvertedTime(LocalDateTime.now());
        lead.setUpdateTime(LocalDateTime.now());
        updateById(lead);
        
        log.info("线索转化成功: leadId={}, customerId={}", leadId, customerId);
        return customerId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeLead(Long leadId, String reason) {
        Lead lead = getById(leadId);
        if (lead == null) {
            throw BusinessException.notFound("线索不存在");
        }
        
        lead.setStatus(3); // 已关闭
        lead.setRemark(lead.getRemark() + " [关闭原因: " + reason + "]");
        lead.setUpdateTime(LocalDateTime.now());
        updateById(lead);
        log.info("关闭线索成功: leadId={}, reason={}", leadId, reason);
    }

    @Override
    public Page<Lead> pageLeads(Page<Lead> page, Long tenantId, String name, 
                                Integer status, Long ownerId) {
        LambdaQueryWrapper<Lead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lead::getTenantId, tenantId)
                .like(name != null, Lead::getName, name)
                .eq(status != null, Lead::getStatus, status)
                .eq(ownerId != null, Lead::getOwnerId, ownerId)
                .orderByDesc(Lead::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public Lead getLeadDetail(Long leadId) {
        return getById(leadId);
    }

    @Override
    public List<Lead> getMyLeads(Long ownerId) {
        LambdaQueryWrapper<Lead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lead::getOwnerId, ownerId)
                .ne(Lead::getStatus, 3) // 排除已关闭
                .orderByAsc(Lead::getNextFollowTime);
        return list(wrapper);
    }

    @Override
    public List<Lead> getPendingFollowLeads(Long tenantId) {
        LambdaQueryWrapper<Lead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Lead::getTenantId, tenantId)
                .le(Lead::getNextFollowTime, LocalDateTime.now())
                .in(Lead::getStatus, 0, 1); // 新线索或跟进中
        return list(wrapper);
    }
}
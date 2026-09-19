package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.common.CrmDocNo;
import cn.aiedge.crm.customer.dto.CustomerFollowUpQuery;
import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import cn.aiedge.crm.customer.mapper.CustomerFollowUpMapper;
import cn.aiedge.crm.customer.service.CustomerFollowUpService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerFollowUpServiceImpl extends ServiceImpl<CustomerFollowUpMapper, CustomerFollowUp> implements CustomerFollowUpService {

    @Override
    public CustomerFollowUp getByFollowUpCode(String followUpCode) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getFollowUpCode, followUpCode);
        wrapper.eq(CustomerFollowUp::getDeleted, 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Page<CustomerFollowUp> pageList(CustomerFollowUpQuery query) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getDeleted, 0);

        if (query.getCustomerId() != null) {
            wrapper.eq(CustomerFollowUp::getCustomerId, query.getCustomerId());
        }
        if (query.getOpportunityId() != null) {
            wrapper.eq(CustomerFollowUp::getOpportunityId, query.getOpportunityId());
        }
        if (query.getLeadId() != null) {
            wrapper.eq(CustomerFollowUp::getLeadId, query.getLeadId());
        }
        if (query.getSalesPersonId() != null) {
            wrapper.eq(CustomerFollowUp::getSalesPersonId, query.getSalesPersonId());
        }
        if (query.getFollowUpType() != null) {
            wrapper.eq(CustomerFollowUp::getFollowUpType, query.getFollowUpType());
        }
        if (query.getFollowUpResult() != null) {
            wrapper.eq(CustomerFollowUp::getFollowUpResult, query.getFollowUpResult());
        }
        if (query.getFollowUpDateStart() != null) {
            wrapper.ge(CustomerFollowUp::getFollowUpDate, query.getFollowUpDateStart());
        }
        if (query.getFollowUpDateEnd() != null) {
            wrapper.le(CustomerFollowUp::getFollowUpDate, query.getFollowUpDateEnd());
        }
        if (query.getNextFollowUpDateStart() != null) {
            wrapper.ge(CustomerFollowUp::getNextFollowUpDate, query.getNextFollowUpDateStart());
        }
        if (query.getNextFollowUpDateEnd() != null) {
            wrapper.le(CustomerFollowUp::getNextFollowUpDate, query.getNextFollowUpDateEnd());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(CustomerFollowUp::getFollowUpCode, kw)
                    .or().like(CustomerFollowUp::getCustomerName, kw)
                    .or().like(CustomerFollowUp::getContent, kw));
        }

        // 排序：跟进日期倒序，用 id 兜底 —— 原实现只有 follow_up_date 一列，
        // 而 PG 的 DESC 默认 NULLS FIRST，会让「没填日期」的记录排在最前、同日期顺序不确定。
        wrapper.orderByDesc(CustomerFollowUp::getFollowUpDate).orderByDesc(CustomerFollowUp::getId);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<CustomerFollowUp> listByCustomerId(Long customerId) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getDeleted, 0);
        wrapper.eq(CustomerFollowUp::getCustomerId, customerId);
        wrapper.orderByDesc(CustomerFollowUp::getFollowUpDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<CustomerFollowUp> listByOpportunityId(Long opportunityId) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getDeleted, 0);
        wrapper.eq(CustomerFollowUp::getOpportunityId, opportunityId);
        wrapper.orderByDesc(CustomerFollowUp::getFollowUpDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<CustomerFollowUp> listByLeadId(Long leadId) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getDeleted, 0);
        wrapper.eq(CustomerFollowUp::getLeadId, leadId);
        wrapper.orderByDesc(CustomerFollowUp::getFollowUpDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public String generateFollowUpCode() {
        String prefix = CrmDocNo.prefixOf("FUP-");
        return CrmDocNo.next(prefix, baseMapper.selectMaxFollowUpCode(prefix), 4);
    }
}
package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import cn.aiedge.crm.customer.mapper.CustomerFollowUpMapper;
import cn.aiedge.crm.customer.service.CustomerFollowUpService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    public Page<CustomerFollowUp> pageList(Long customerId, Long opportunityId, Long leadId,
                                            Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowUp::getDeleted, 0);
        
        if (customerId != null) {
            wrapper.eq(CustomerFollowUp::getCustomerId, customerId);
        }
        
        if (opportunityId != null) {
            wrapper.eq(CustomerFollowUp::getOpportunityId, opportunityId);
        }
        
        if (leadId != null) {
            wrapper.eq(CustomerFollowUp::getLeadId, leadId);
        }
        
        if (salesPersonId != null) {
            wrapper.eq(CustomerFollowUp::getSalesPersonId, salesPersonId);
        }
        
        wrapper.orderByDesc(CustomerFollowUp::getFollowUpDate);
        
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
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "FUP-" + dateStr + String.format("%04d", count + 1);
    }
}
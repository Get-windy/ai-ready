package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.entity.CustomerLead;
import cn.aiedge.crm.customer.mapper.CustomerLeadMapper;
import cn.aiedge.crm.customer.mapper.CustomerMapper;
import cn.aiedge.crm.customer.service.CustomerLeadService;
import cn.aiedge.crm.customer.service.CustomerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLeadServiceImpl extends ServiceImpl<CustomerLeadMapper, CustomerLead> implements CustomerLeadService {

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;

    @Override
    public CustomerLead getByLeadCode(String leadCode) {
        LambdaQueryWrapper<CustomerLead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLead::getLeadCode, leadCode);
        wrapper.eq(CustomerLead::getDeleted, 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Page<CustomerLead> pageList(String keyword, Integer leadStatus, Integer leadLevel,
                                        Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<CustomerLead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLead::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(CustomerLead::getLeadName, keyword)
                    .or().like(CustomerLead::getCompanyName, keyword)
                    .or().like(CustomerLead::getContactName, keyword));
        }
        
        if (leadStatus != null) {
            wrapper.eq(CustomerLead::getLeadStatus, leadStatus);
        }
        
        if (leadLevel != null) {
            wrapper.eq(CustomerLead::getLeadLevel, leadLevel);
        }
        
        if (salesPersonId != null) {
            wrapper.eq(CustomerLead::getSalesPersonId, salesPersonId);
        }
        
        wrapper.orderByDesc(CustomerLead::getCreatedAt);
        
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<CustomerLead> listBySalesPersonId(Long salesPersonId) {
        LambdaQueryWrapper<CustomerLead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLead::getDeleted, 0);
        wrapper.eq(CustomerLead::getSalesPersonId, salesPersonId);
        wrapper.orderByDesc(CustomerLead::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Customer convertToCustomer(Long leadId) {
        CustomerLead lead = baseMapper.selectById(leadId);
        if (lead == null || lead.getDeleted() == 1) {
            throw new RuntimeException("线索不存在: " + leadId);
        }
        
        if (lead.getConvertedCustomerId() != null) {
            throw new RuntimeException("线索已转化: " + leadId);
        }
        
        Customer customer = new Customer();
        customer.setCustomerCode(customerService.generateCustomerCode());
        customer.setCustomerName(lead.getCompanyName());
        customer.setShortName(lead.getCompanyName());
        customer.setCustomerType(1);
        customer.setCustomerSource(lead.getLeadSource());
        customer.setIndustryType(lead.getIndustryType());
        customer.setAddress(lead.getAddress());
        customer.setProvince(lead.getProvince());
        customer.setCity(lead.getCity());
        customer.setBusinessContact(lead.getContactName());
        customer.setBusinessContactPhone(lead.getContactPhone());
        customer.setEmail(lead.getContactEmail());
        customer.setSalesPersonId(lead.getSalesPersonId());
        customer.setSalesPersonName(lead.getSalesPersonName());
        customer.setDepartmentId(lead.getDepartmentId());
        customer.setDepartmentName(lead.getDepartmentName());
        customer.setPotentialAmount(lead.getEstimatedAmount());
        customer.setStatus(1);
        customer.setCustomerLevel(lead.getLeadLevel());
        customer.setRemark("从线索转化: " + lead.getLeadCode());
        
        customerMapper.insert(customer);
        
        lead.setLeadStatus(3);
        lead.setLeadStatusDesc("已转化");
        lead.setConvertedCustomerId(customer.getId());
        lead.setConvertedTime(LocalDateTime.now());
        lead.setActualCloseDate(LocalDate.now());
        baseMapper.updateById(lead);
        
        log.info("线索转化成功: {} -> {}", lead.getLeadCode(), customer.getCustomerCode());
        return customer;
    }

    @Override
    public String generateLeadCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "LEAD-" + dateStr + String.format("%04d", count + 1);
    }
}
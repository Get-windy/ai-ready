package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLeadServiceImpl extends ServiceImpl<CustomerLeadMapper, CustomerLead> implements CustomerLeadService {

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final TransactionTemplate transactionTemplate;

    /** 系统统一号段服务（biz_number_sequence，行锁 + 按日重置） */
    private final BizNumberGeneratorService bizNumberGeneratorService;

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
        LambdaQueryWrapper<CustomerLead> wrapper = buildQueryWrapper(keyword, leadStatus, leadLevel, salesPersonId);
        wrapper.orderByDesc(CustomerLead::getCreatedAt);
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<CustomerLead> exportList(String keyword, Integer leadStatus, Integer leadLevel,
                                          Long salesPersonId) {
        LambdaQueryWrapper<CustomerLead> wrapper = buildQueryWrapper(keyword, leadStatus, leadLevel, salesPersonId);
        wrapper.orderByDesc(CustomerLead::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 构建公共查询条件
     */
    private LambdaQueryWrapper<CustomerLead> buildQueryWrapper(String keyword, Integer leadStatus,
                                                                Integer leadLevel, Long salesPersonId) {
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
        return wrapper;
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
            throw BusinessException.notFound("线索不存在: " + leadId);
        }
        
        if (lead.getConvertedCustomerId() != null) {
            throw BusinessException.badRequest("线索已转化: " + leadId);
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
    public Map<String, Object> batchConvertToCustomer(List<Long> leadIds) {
        if (leadIds == null || leadIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要转化的线索");
        }

        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (Long leadId : leadIds) {
            try {
                // 逐条独立事务：单条失败不影响已成功的部分（不整批回滚）
                transactionTemplate.executeWithoutResult(status -> convertToCustomer(leadId));
                successIds.add(leadId);
            } catch (Exception e) {
                Map<String, Object> failure = new HashMap<>();
                failure.put("leadId", leadId);
                failure.put("reason", e.getMessage());
                failures.add(failure);
                log.warn("线索批量转化跳过 {}: {}", leadId, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", leadIds.size());
        result.put("successCount", successIds.size());
        result.put("failCount", failures.size());
        result.put("successIds", successIds);
        result.put("failures", failures);
        log.info("线索批量转化完成: 共 {} 条, 成功 {}, 失败 {}", leadIds.size(), successIds.size(), failures.size());
        return result;
    }

    @Override
    public String generateLeadCode() {
        // 按当日已有单号的最大值顺延（含已逻辑删除行），而非 selectCount+1：
        // 后者在「删掉最新一条再新建」时会撞 uk_crm_lead_code 唯一约束（500）。
        // 走系统统一号段（biz_number_sequence + SELECT FOR UPDATE），不再「查最大号 +1」
        return bizNumberGeneratorService.nextNumber("CRM_LEAD");
    }
}
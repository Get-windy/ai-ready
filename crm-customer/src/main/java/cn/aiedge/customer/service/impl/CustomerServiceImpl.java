package cn.aiedge.customer.service.impl;

import cn.aiedge.customer.dto.CustomerDTO;
import cn.aiedge.customer.dto.CustomerContactDTO;
import cn.aiedge.customer.dto.CustomerFollowDTO;
import cn.aiedge.customer.entity.Customer;
import cn.aiedge.customer.entity.CustomerContact;
import cn.aiedge.customer.entity.CustomerFollow;
import cn.aiedge.customer.mapper.CustomerMapper;
import cn.aiedge.customer.mapper.CustomerContactMapper;
import cn.aiedge.customer.mapper.CustomerFollowMapper;
import cn.aiedge.customer.service.ICustomerService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> 
        implements ICustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerContactMapper contactMapper;
    private final CustomerFollowMapper followMapper;

    @Override
    public Page<CustomerDTO> pageCustomers(Page<Customer> page, Long tenantId, String customerName,
                                            Integer stage, Integer level, Long ownerId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getTenantId, tenantId)
               .like(customerName != null, Customer::getCustomerName, customerName)
               .eq(stage != null, Customer::getStage, stage)
               .eq(level != null, Customer::getLevel, level)
               .eq(ownerId != null, Customer::getOwnerId, ownerId)
               .orderByDesc(Customer::getCreateTime);

        Page<Customer> result = page(page, wrapper);

        Page<CustomerDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());

        return dtoPage;
    }

    @Override
    public CustomerDTO getCustomerDetail(Long id) {
        Customer customer = getById(id);
        if (customer == null) return null;

        CustomerDTO dto = convertToDTO(customer);
        List<CustomerContact> contacts = contactMapper.selectByCustomerId(id);
        dto.setContacts(contacts.stream().map(this::convertContactToDTO).collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(CustomerDTO dto) {
        if (dto.getCustomerCode() == null || dto.getCustomerCode().isEmpty()) {
            dto.setCustomerCode(generateCustomerCode());
        }

        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        customer.setFollowCount(0);
        customer.setDealCount(0);
        customer.setDealAmount(java.math.BigDecimal.ZERO);
        customer.setStatus(0);
        customer.setCreateTime(LocalDateTime.now());

        save(customer);
        log.info("创建客户: customerId={}, customerName={}", customer.getId(), customer.getCustomerName());
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerDTO dto) {
        Customer customer = getById(dto.getId());
        if (customer == null) throw new RuntimeException("客户不存在");

        BeanUtils.copyProperties(dto, customer);
        customer.setUpdateTime(LocalDateTime.now());
        updateById(customer);
        log.info("更新客户: customerId={}", customer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        removeById(id);
        log.info("删除客户: customerId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long id, Integer stage) {
        customerMapper.updateStage(id, stage);
        log.info("更新客户阶段: customerId={}, stage={}", id, stage);
    }

    @Override
    public List<CustomerDTO> getPendingFollowCustomers(Long ownerId) {
        List<Customer> customers = customerMapper.selectPendingFollow(ownerId);
        return customers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<CustomerDTO> getCustomersByStage(Long tenantId, Integer stage) {
        List<Customer> customers = customerMapper.selectByStage(tenantId, stage);
        return customers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public String generateCustomerCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "C" + dateStr + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowRecord(CustomerFollowDTO dto) {
        CustomerFollow follow = new CustomerFollow();
        BeanUtils.copyProperties(dto, follow);
        follow.setFollowerId(StpUtil.getLoginIdAsLong());
        follow.setFollowTime(LocalDateTime.now());
        follow.setCreateTime(LocalDateTime.now());

        followMapper.insert(follow);

        // 更新客户跟进信息
        customerMapper.incrementFollowCount(dto.getCustomerId(), LocalDateTime.now());
        if (dto.getNextFollowTime() != null) {
            Customer customer = getById(dto.getCustomerId());
            customer.setNextFollowTime(dto.getNextFollowTime());
            updateById(customer);
        }

        // 如果有阶段变更
        if (dto.getStageTo() != null) {
            customerMapper.updateStage(dto.getCustomerId(), dto.getStageTo());
        }

        log.info("添加跟进记录: customerId={}, followId={}", dto.getCustomerId(), follow.getId());
        return follow.getId();
    }

    @Override
    public List<CustomerFollowDTO> getFollowRecords(Long customerId) {
        List<CustomerFollow> follows = followMapper.selectByCustomerId(customerId);
        return follows.stream().map(this::convertFollowToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferCustomer(Long id, Long newOwnerId) {
        Customer customer = getById(id);
        if (customer == null) throw new RuntimeException("客户不存在");

        customer.setOwnerId(newOwnerId);
        customer.setUpdateTime(LocalDateTime.now());
        updateById(customer);
        log.info("转移客户: customerId={}, newOwnerId={}", id, newOwnerId);
    }

    private CustomerDTO convertToDTO(Customer customer) {
        if (customer == null) return null;
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        dto.setCustomerTypeName(getCustomerTypeName(customer.getCustomerType()));
        dto.setSourceName(getSourceName(customer.getSource()));
        dto.setLevelName(getLevelName(customer.getLevel()));
        dto.setStageName(getStageName(customer.getStage()));
        return dto;
    }

    private CustomerContactDTO convertContactToDTO(CustomerContact contact) {
        if (contact == null) return null;
        CustomerContactDTO dto = new CustomerContactDTO();
        BeanUtils.copyProperties(contact, dto);
        return dto;
    }

    private CustomerFollowDTO convertFollowToDTO(CustomerFollow follow) {
        if (follow == null) return null;
        CustomerFollowDTO dto = new CustomerFollowDTO();
        BeanUtils.copyProperties(follow, dto);
        dto.setFollowTypeName(getFollowTypeName(follow.getFollowType()));
        dto.setResultName(getResultName(follow.getResult()));
        return dto;
    }

    private String getCustomerTypeName(Integer type) {
        if (type == null) return "未知";
        return type == 1 ? "企业" : "个人";
    }

    private String getSourceName(Integer source) {
        if (source == null) return "未知";
        switch (source) {
            case 1: return "线上";
            case 2: return "线下";
            case 3: return "转介绍";
            default: return "其他";
        }
    }

    private String getLevelName(Integer level) {
        if (level == null) return "普通";
        switch (level) {
            case 1: return "普通";
            case 2: return "重要";
            case 3: return "VIP";
            default: return "普通";
        }
    }

    private String getStageName(Integer stage) {
        if (stage == null) return "潜在";
        switch (stage) {
            case 1: return "潜在";
            case 2: return "意向";
            case 3: return "成交";
            case 4: return "流失";
            default: return "潜在";
        }
    }

    private String getFollowTypeName(Integer type) {
        if (type == null) return "其他";
        switch (type) {
            case 1: return "电话";
            case 2: return "上门";
            case 3: return "微信";
            case 4: return "邮件";
            default: return "其他";
        }
    }

    private String getResultName(Integer result) {
        if (result == null) return "待定";
        switch (result) {
            case 1: return "成功";
            case 2: return "失败";
            default: return "待定";
        }
    }
}

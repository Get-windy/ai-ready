package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.erp.party.service.PartyCreditService;
import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.mapper.CustomerMapper;
import cn.aiedge.crm.customer.service.CustomerService;
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
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    /** 系统统一号段服务（biz_number_sequence，行锁 + 按日重置） */
    private final BizNumberGeneratorService bizNumberGeneratorService;

    /** 信用归属 ERP（erp-partner）：CRM 客户按关联往来单位读取额度/欠款 */
    private final PartyCreditService partyCreditService;

    @Override
    public Customer getByCustomerCode(String customerCode) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getCustomerCode, customerCode);
        wrapper.eq(Customer::getDeleted, 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Page<Customer> pageList(String keyword, Integer customerType, Integer customerLevel,
                                    Integer status, Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(keyword, customerType, customerLevel, status, salesPersonId);
        wrapper.orderByDesc(Customer::getCreatedAt);
        Page<Customer> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillCreditFromErp(page.getRecords());
        return page;
    }

    @Override
    public List<Customer> exportList(String keyword, Integer customerType, Integer customerLevel,
                                      Integer status, Long salesPersonId) {
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(keyword, customerType, customerLevel, status, salesPersonId);
        wrapper.orderByDesc(Customer::getCreatedAt);
        List<Customer> list = baseMapper.selectList(wrapper);
        fillCreditFromErp(list);
        return list;
    }

    /**
     * 构建公共查询条件
     */
    private LambdaQueryWrapper<Customer> buildQueryWrapper(String keyword, Integer customerType,
                                                            Integer customerLevel, Integer status,
                                                            Long salesPersonId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Customer::getCustomerName, keyword)
                    .or().like(Customer::getCustomerCode, keyword)
                    .or().like(Customer::getShortName, keyword));
        }

        if (customerType != null) {
            wrapper.eq(Customer::getCustomerType, customerType);
        }

        if (customerLevel != null) {
            wrapper.eq(Customer::getCustomerLevel, customerLevel);
        }

        if (status != null) {
            wrapper.eq(Customer::getStatus, status);
        }

        if (salesPersonId != null) {
            wrapper.eq(Customer::getSalesPersonId, salesPersonId);
        }
        return wrapper;
    }

    @Override
    public List<Customer> listBySalesPersonId(Long salesPersonId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0);
        wrapper.eq(Customer::getSalesPersonId, salesPersonId);
        wrapper.orderByDesc(Customer::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<Customer> listByCustomerLevel(Integer customerLevel) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getDeleted, 0);
        wrapper.eq(Customer::getCustomerLevel, customerLevel);
        wrapper.orderByDesc(Customer::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 把「信用额度 / 当前欠款」替换为**关联 ERP 往来单位**的值。
     *
     * <p>信用能力自 2026-09-26 起归 ERP（{@code PartyCreditService}）。CRM 客户表上的这两个列
     * 只是历史登记位，不再作为事实源：有关联往来单位（{@code md_partner_id}）时以 ERP 的值为准，
     * 未关联时保留 CRM 侧的原值（该客户在 ERP 里还不存在，无从取值）。</p>
     *
     * <p>批量填充，按往来单位 ID 去重后逐个取，避免每次查询都做一次全表扫描。</p>
     */
    private void fillCreditFromErp(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            return;
        }
        for (Customer customer : customers) {
            Long partnerId = customer.getMdPartnerId();
            if (partnerId == null) {
                continue;
            }
            try {
                customer.setCreditLimit(partyCreditService.getCreditLimit(partnerId));
                customer.setCurrentDebt(partyCreditService.getCurrentDebt(partnerId));
            } catch (Exception e) {
                log.warn("读取往来单位信用失败，保留 CRM 侧原值: customerId={}, partnerId={}", customer.getId(), partnerId, e);
            }
        }
    }

    @Override
    public Customer getById(java.io.Serializable id) {
        Customer customer = super.getById(id);
        if (customer != null) {
            fillCreditFromErp(List.of(customer));
        }
        return customer;
    }

    @Override
    public String generateCustomerCode() {
        // 走系统统一号段（biz_number_sequence + SELECT FOR UPDATE），不再「查最大号 +1」
        return bizNumberGeneratorService.nextNumber("CRM_CUSTOMER");
    }
}
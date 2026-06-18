package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.mapper.CustomerMapper;
import cn.aiedge.crm.customer.service.CustomerService;
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
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

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
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Customer> exportList(String keyword, Integer customerType, Integer customerLevel,
                                      Integer status, Long salesPersonId) {
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(keyword, customerType, customerLevel, status, salesPersonId);
        wrapper.orderByDesc(Customer::getCreatedAt);
        return baseMapper.selectList(wrapper);
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

    @Override
    public String generateCustomerCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "CUS-" + dateStr + String.format("%04d", count + 1);
    }
}
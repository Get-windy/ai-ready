package cn.aiedge.erp.customer.service.impl;

import cn.aiedge.erp.customer.entity.CustomerLevel;
import cn.aiedge.erp.customer.mapper.CustomerLevelMapper;
import cn.aiedge.erp.customer.service.CustomerLevelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerLevelServiceImpl extends ServiceImpl<CustomerLevelMapper, CustomerLevel> implements CustomerLevelService {
    
    @Override
    public List<CustomerLevel> listAll(Long tenantId) {
        LambdaQueryWrapper<CustomerLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLevel::getTenantId, tenantId)
               .orderByAsc(CustomerLevel::getSortWeight);
        return this.list(wrapper);
    }
    
    @Override
    public List<CustomerLevel> listEnabled(Long tenantId) {
        LambdaQueryWrapper<CustomerLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLevel::getTenantId, tenantId)
               .eq(CustomerLevel::getEnabled, true)
               .orderByAsc(CustomerLevel::getSortWeight);
        return this.list(wrapper);
    }
    
    @Override
    public CustomerLevel getByCode(Long tenantId, String levelCode) {
        LambdaQueryWrapper<CustomerLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLevel::getTenantId, tenantId)
               .eq(CustomerLevel::getLevelCode, levelCode);
        return this.getOne(wrapper);
    }
    
    @Override
    public Page<CustomerLevel> pageList(Long tenantId, String levelName, String levelCode, Boolean enabled, Page<CustomerLevel> page) {
        LambdaQueryWrapper<CustomerLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerLevel::getTenantId, tenantId);
        if (levelName != null && !levelName.isEmpty()) {
            wrapper.like(CustomerLevel::getLevelName, levelName);
        }
        if (levelCode != null && !levelCode.isEmpty()) {
            wrapper.like(CustomerLevel::getLevelCode, levelCode);
        }
        if (enabled != null) {
            wrapper.eq(CustomerLevel::getEnabled, enabled);
        }
        wrapper.orderByAsc(CustomerLevel::getSortWeight);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createLevel(CustomerLevel level) {
        CustomerLevel existing = this.getByCode(level.getTenantId(), level.getLevelCode());
        if (existing != null) {
            throw new RuntimeException("等级编码已存在");
        }
        level.setEnabled(true);
        if (level.getSortWeight() == null) {
            level.setSortWeight(0);
        }
        return this.save(level);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLevel(CustomerLevel level) {
        CustomerLevel existing = this.getById(level.getId());
        if (existing == null) {
            throw new RuntimeException("等级不存在");
        }
        if (!existing.getLevelCode().equals(level.getLevelCode())) {
            CustomerLevel byCode = this.getByCode(level.getTenantId(), level.getLevelCode());
            if (byCode != null) {
                throw new RuntimeException("等级编码已存在");
            }
        }
        return this.updateById(level);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLevel(Long tenantId, Long levelId) {
        CustomerLevel level = this.getById(levelId);
        if (level == null) {
            throw new RuntimeException("等级不存在");
        }
        return this.removeById(levelId);
    }
    
    @Override
    public boolean enableLevel(Long tenantId, Long levelId) {
        CustomerLevel level = this.getById(levelId);
        if (level == null) {
            throw new RuntimeException("等级不存在");
        }
        level.setEnabled(true);
        return this.updateById(level);
    }
    
    @Override
    public boolean disableLevel(Long tenantId, Long levelId) {
        CustomerLevel level = this.getById(levelId);
        if (level == null) {
            throw new RuntimeException("等级不存在");
        }
        level.setEnabled(false);
        return this.updateById(level);
    }
    
    @Override
    public CustomerLevel calculateCustomerLevel(Long tenantId, Double totalAmount, Integer frequency, Integer paymentRate) {
        List<CustomerLevel> levels = this.listEnabled(tenantId);
        for (CustomerLevel level : levels) {
            boolean amountMatch = totalAmount >= level.getMinAmount();
            boolean frequencyMatch = frequency >= level.getMinFrequency();
            boolean paymentMatch = paymentRate >= level.getMinPaymentRate();
            if (amountMatch && frequencyMatch && paymentMatch) {
                return level;
            }
        }
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignLevelToCustomer(Long tenantId, Long customerId) {
        return true;
    }
    
    @Override
    public List<CustomerLevel> getLevelPriceRules(Long tenantId, Long levelId) {
        return this.listEnabled(tenantId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLevelPriceRule(Long tenantId, Long levelId, Long productId, Double specialPrice) {
        return true;
    }
}
package com.aiready.party.service.impl;

import com.aiready.party.entity.CustomerGrade;
import com.aiready.party.service.CustomerGradeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户等级服务实现类
 */
@Service
public class CustomerGradeServiceImpl extends ServiceImpl<com.aiready.party.mapper.CustomerGradeMapper, CustomerGrade> implements CustomerGradeService {

    @Override
    public CustomerGrade getByGradeCode(String gradeCode) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getGradeCode, gradeCode)
                .eq(CustomerGrade::getDeleted, 0)
        );
    }

    @Override
    public CustomerGrade getByGradeLevel(Integer gradeLevel) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getGradeLevel, gradeLevel)
                .eq(CustomerGrade::getDeleted, 0)
        );
    }

    @Override
    public List<CustomerGrade> listActiveGrades() {
        return this.list(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getStatus, 1)
                .eq(CustomerGrade::getDeleted, 0)
                .orderByAsc(CustomerGrade::getGradeLevel)
        );
    }

    @Override
    public CustomerGrade getGradeByAmount(BigDecimal amount) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGrade>()
                .le(CustomerGrade::getMinAmount, amount)
                .gt(CustomerGrade::getMaxAmount, amount)
                .eq(CustomerGrade::getStatus, 1)
                .eq(CustomerGrade::getDeleted, 0)
                .orderByDesc(CustomerGrade::getGradeLevel)
                .last("LIMIT 1")
        );
    }

    @Override
    public CustomerGrade getNextGrade(Long currentGradeId) {
        CustomerGrade current = this.getById(currentGradeId);
        if (current == null) {
            return null;
        }
        return this.getOne(
            new LambdaQueryWrapper<CustomerGrade>()
                .gt(CustomerGrade::getGradeLevel, current.getGradeLevel())
                .eq(CustomerGrade::getStatus, 1)
                .eq(CustomerGrade::getDeleted, 0)
                .orderByAsc(CustomerGrade::getGradeLevel)
                .last("LIMIT 1")
        );
    }

    @Override
    public BigDecimal calculateAmountToNextGrade(Long currentGradeId, BigDecimal currentAmount) {
        CustomerGrade nextGrade = getNextGrade(currentGradeId);
        if (nextGrade == null || nextGrade.getMinAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = nextGrade.getMinAmount().subtract(currentAmount);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    @Override
    public boolean checkGradeCodeExists(String gradeCode) {
        return this.count(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getGradeCode, gradeCode)
                .eq(CustomerGrade::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean checkGradeCodeExists(String gradeCode, Long excludeId) {
        return this.count(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getGradeCode, gradeCode)
                .ne(CustomerGrade::getId, excludeId)
                .eq(CustomerGrade::getDeleted, 0)
        ) > 0;
    }

    @Override
    @Transactional
    public boolean enableGrade(Long id) {
        CustomerGrade grade = new CustomerGrade();
        grade.setId(id);
        grade.setStatus(1);
        return this.updateById(grade);
    }

    @Override
    @Transactional
    public boolean disableGrade(Long id) {
        CustomerGrade grade = new CustomerGrade();
        grade.setId(id);
        grade.setStatus(0);
        return this.updateById(grade);
    }

    @Override
    public CustomerGrade getDefaultGrade() {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGrade>()
                .eq(CustomerGrade::getStatus, 1)
                .eq(CustomerGrade::getDeleted, 0)
                .orderByAsc(CustomerGrade::getGradeLevel)
                .last("LIMIT 1")
        );
    }

    @Override
    @Transactional
    public boolean setDefaultGrade(Long id) {
        // 这里可以实现更复杂的默认等级逻辑
        // 例如将其他等级设为非默认，将指定等级设为默认
        return true;
    }

    @Override
    public BigDecimal getDiscountRate(Long gradeId) {
        CustomerGrade grade = this.getById(gradeId);
        return grade != null ? grade.getDiscountRate() : BigDecimal.valueOf(100);
    }

    @Override
    public BigDecimal getPointRate(Long gradeId) {
        CustomerGrade grade = this.getById(gradeId);
        return grade != null ? grade.getPointRate() : BigDecimal.ONE;
    }

    @Override
    public BigDecimal getCreditLimit(Long gradeId) {
        CustomerGrade grade = this.getById(gradeId);
        return grade != null ? grade.getCreditLimit() : BigDecimal.ZERO;
    }
}

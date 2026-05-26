package com.aiready.party.service;

import com.aiready.party.entity.CustomerGrade;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户等级服务接口
 */
public interface CustomerGradeService extends IService<CustomerGrade> {

    /**
     * 根据等级编码查询
     */
    CustomerGrade getByGradeCode(String gradeCode);

    /**
     * 根据等级级别查询
     */
    CustomerGrade getByGradeLevel(Integer gradeLevel);

    /**
     * 获取所有启用的等级列表
     */
    List<CustomerGrade> listActiveGrades();

    /**
     * 根据消费金额获取对应等级
     */
    CustomerGrade getGradeByAmount(BigDecimal amount);

    /**
     * 获取下一等级
     */
    CustomerGrade getNextGrade(Long currentGradeId);

    /**
     * 计算升级到下一等级还需金额
     */
    BigDecimal calculateAmountToNextGrade(Long currentGradeId, BigDecimal currentAmount);

    /**
     * 检查等级编码是否已存在
     */
    boolean checkGradeCodeExists(String gradeCode);

    /**
     * 检查等级编码是否已存在（排除指定ID）
     */
    boolean checkGradeCodeExists(String gradeCode, Long excludeId);

    /**
     * 启用等级
     */
    boolean enableGrade(Long id);

    /**
     * 禁用等级
     */
    boolean disableGrade(Long id);

    /**
     * 获取默认等级
     */
    CustomerGrade getDefaultGrade();

    /**
     * 设置默认等级
     */
    boolean setDefaultGrade(Long id);

    /**
     * 获取等级折扣率
     */
    BigDecimal getDiscountRate(Long gradeId);

    /**
     * 获取等级积分倍率
     */
    BigDecimal getPointRate(Long gradeId);

    /**
     * 获取等级信用额度
     */
    BigDecimal getCreditLimit(Long gradeId);
}

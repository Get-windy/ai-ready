package com.aiready.party.service;

import com.aiready.party.entity.CustomerGradePrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户等级价格服务接口
 */
public interface CustomerGradePriceService extends IService<CustomerGradePrice> {

    /**
     * 根据等级ID和产品ID查询价格
     */
    CustomerGradePrice getByGradeAndProduct(Long gradeId, Long productId);

    /**
     * 根据等级ID和SKU ID查询价格
     */
    CustomerGradePrice getByGradeAndSku(Long gradeId, Long skuId);

    /**
     * 获取产品的等级价格列表
     */
    List<CustomerGradePrice> listByProduct(Long productId);

    /**
     * 获取等级的产品价格列表
     */
    List<CustomerGradePrice> listByGrade(Long gradeId);

    /**
     * 计算等级价格
     * @param gradeId 等级ID
     * @param productId 产品ID
     * @param standardPrice 标准售价
     * @return 等级价格
     */
    BigDecimal calculateGradePrice(Long gradeId, Long productId, BigDecimal standardPrice);

    /**
     * 批量设置等级价格
     */
    boolean batchSetGradePrice(Long gradeId, List<Long> productIds, BigDecimal price);

    /**
     * 批量设置等级折扣率
     */
    boolean batchSetDiscountRate(Long gradeId, List<Long> productIds, BigDecimal discountRate);

    /**
     * 复制等级价格设置
     */
    boolean copyGradePrice(Long sourceGradeId, Long targetGradeId);

    /**
     * 同步等级价格（根据标准价格和折扣率重新计算）
     */
    boolean syncGradePrice(Long gradeId);

    /**
     * 检查是否存在价格设置
     */
    boolean existsPriceSetting(Long gradeId, Long productId);

    /**
     * 获取默认价格
     */
    CustomerGradePrice getDefaultPrice(Long productId);

    /**
     * 设置默认价格
     */
    boolean setDefaultPrice(Long id);

    /**
     * 获取生效中的价格列表
     */
    List<CustomerGradePrice> listActivePrices(Long gradeId);

    /**
     * 根据优先级获取价格
     */
    CustomerGradePrice getPriceByPriority(Long gradeId, Long productId);

    /**
     * 验证价格是否在限价范围内
     */
    boolean validatePriceRange(Long gradeId, Long productId, BigDecimal price);

    /**
     * 检查价格是否存在
     */
    boolean checkPriceExists(Long gradeId, Long productId);

    /**
     * 检查价格是否存在（排除指定ID）
     */
    boolean checkPriceExists(Long gradeId, Long productId, Long excludeId);

    /**
     * 更新价格状态
     */
    boolean updatePriceStatus(Long id, Integer status);

    /**
     * 获取生效中的价格列表（按产品）
     */
    List<CustomerGradePrice> getActivePrices(Long productId);
}

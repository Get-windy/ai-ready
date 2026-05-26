package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.PriceCalculationRequest;
import cn.aiedge.erp.sale.dto.PriceCalculationResult;

import java.util.List;

/**
 * 价格计算服务接口
 * 核心价格计算引擎，支持多维度价格策略和促销计算
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface IPriceCalculationService {

    /**
     * 计算订单价格
     * 根据客户、产品、区域、时间等维度应用价格策略和促销
     *
     * @param request 计算请求
     * @return 计算结果
     */
    PriceCalculationResult calculatePrice(PriceCalculationRequest request);

    /**
     * 模拟价格计算（不改变实际数据）
     *
     * @param request 计算请求
     * @return 计算结果
     */
    PriceCalculationResult simulatePrice(PriceCalculationRequest request);

    /**
     * 应用指定促销活动计算价格
     *
     * @param request 计算请求
     * @param promotionIds 促销ID列表
     * @return 计算结果
     */
    PriceCalculationResult calculateWithPromotions(PriceCalculationRequest request, List<Long> promotionIds);

    /**
     * 获取客户适用的价格策略
     *
     * @param customerId 客户ID
     * @param customerLevel 客户等级
     * @return 策略信息列表
     */
    List<PriceCalculationResult.AppliedStrategyInfo> getCustomerApplicableStrategies(Long customerId, String customerLevel);

    /**
     * 验证价格策略配置是否有效
     *
     * @param strategyId 策略ID
     * @return 验证结果
     */
    boolean validateStrategy(Long strategyId);
}

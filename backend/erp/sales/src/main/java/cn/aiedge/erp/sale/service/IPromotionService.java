package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.PromotionActivityDTO;
import cn.aiedge.erp.sale.entity.PromotionActivity;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 促销活动服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface IPromotionService extends IService<PromotionActivity> {

    /**
     * 创建促销活动
     *
     * @param dto 活动DTO
     * @return 活动ID
     */
    Long createPromotion(PromotionActivityDTO dto);

    /**
     * 更新促销活动
     *
     * @param id 活动ID
     * @param dto 活动DTO
     */
    void updatePromotion(Long id, PromotionActivityDTO dto);

    /**
     * 删除促销活动
     *
     * @param id 活动ID
     */
    void deletePromotion(Long id);

    /**
     * 获取活动详情
     *
     * @param id 活动ID
     * @return 活动DTO
     */
    PromotionActivityDTO getPromotionDetail(Long id);

    /**
     * 分页查询活动列表
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param name 活动名称
     * @param status 状态
     * @param type 类型
     * @return 分页结果
     */
    Page<PromotionActivityDTO> pagePromotions(Page<PromotionActivityDTO> page, Long tenantId,
                                              String name, String status, String type);

    /**
     * 发布促销
     *
     * @param id 活动ID
     */
    void publishPromotion(Long id);

    /**
     * 取消促销
     *
     * @param id 活动ID
     */
    void cancelPromotion(Long id);

    /**
     * 获取当前生效的促销列表
     *
     * @param tenantId 租户ID
     * @return 促销列表
     */
    List<PromotionActivityDTO> getActivePromotions(Long tenantId);

    /**
     * 获取适用于指定产品和客户的促销
     *
     * @param tenantId 租户ID
     * @param productId 产品ID
     * @param customerLevel 客户等级
     * @return 促销列表
     */
    List<PromotionActivityDTO> getApplicablePromotions(Long tenantId, Long productId, String customerLevel);
}

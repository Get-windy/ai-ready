package cn.aiedge.erp.purchase.saledriven.service;

import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenPurchaseResult;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenQueryDTO;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenRowDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 以销定购 Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SalesDrivenService {

    /** 采购模式：采购成品 */
    String MODE_FINISHED = "FINISHED";

    /** 采购模式：采购原料 */
    String MODE_MATERIAL = "MATERIAL";

    /**
     * 以销定购-销售订单列表分页查询
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<SalesDrivenRowDTO> page(SalesDrivenQueryDTO query);

    /**
     * 按销售订单执行采购（采购成品/采购原料），自动按商品默认供应商生成采购订单并提交。
     *
     * @param orderId 销售订单ID
     * @param mode    采购模式（FINISHED/MATERIAL）
     * @return 执行结果
     */
    SalesDrivenPurchaseResult purchase(Long orderId, String mode);
}

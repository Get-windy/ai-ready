package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocQueryDTO;
import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOutbound;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.service.ISaleOutboundService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
import cn.aiedge.erp.sale.service.ISaleExchangeService;
import cn.aiedge.erp.sale.service.UnifiedSalesDocQueryService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 销售单据查询控制器
 * 提供统一的销售单据查询接口
 */
@RestController
@RequestMapping("/api/sales/doc-query")
@SaCheckPermission("sales:doc-query")
public class SalesDocQueryController {

    @Autowired
    private UnifiedSalesDocQueryService unifiedSalesDocQueryService;

    @Autowired
    private ISaleOrderService saleOrderService;

    @Autowired
    private ISaleOutboundService saleOutboundService;

    @Autowired
    private ISaleReturnDocService saleReturnDocService;

    @Autowired
    private ISaleExchangeService saleExchangeService;

    /**
     * 统一销售单据分页查询接口
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<UnifiedSalesDocumentDTO> unifiedPage(UnifiedSalesDocQueryDTO queryDTO) {
        // 设置默认分页参数
        if (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) {
            queryDTO.setCurrent(1L);
        }
        if (queryDTO.getSize() == null || queryDTO.getSize() <= 0) {
            queryDTO.setSize(10L);
        }

        return unifiedSalesDocQueryService.unifiedPage(queryDTO);
    }

    /**
     * 更新单据整单备注
     * @param docType 单据类型 (SALE_ORDER/OUTBOUND/RETURN/EXCHANGE)
     * @param id 单据ID
     * @param body 请求体 { remark: "备注内容" }
     */
    @PutMapping("/{docType}/{id}/remark")
    public Map<String, Object> updateRemark(
            @PathVariable String docType,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String remark = body.get("remark");
        boolean updated = false;

        switch (docType) {
            case "SALE_ORDER": {
                SaleOrder entity = saleOrderService.getById(id);
                if (entity != null) {
                    entity.setRemark(remark);
                    updated = saleOrderService.updateById(entity);
                }
                break;
            }
            case "OUTBOUND": {
                SaleOutbound entity = saleOutboundService.getById(id);
                if (entity != null) {
                    entity.setRemark(remark);
                    updated = saleOutboundService.updateById(entity);
                }
                break;
            }
            case "RETURN": {
                SaleReturnDoc entity = saleReturnDocService.getById(id);
                if (entity != null) {
                    entity.setRemark(remark);
                    updated = saleReturnDocService.updateById(entity);
                }
                break;
            }
            case "EXCHANGE": {
                SaleExchange entity = saleExchangeService.getById(id);
                if (entity != null) {
                    entity.setRemark(remark);
                    updated = saleExchangeService.updateById(entity);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的单据类型: " + docType);
        }

        return Map.of("success", updated, "message", updated ? "更新成功" : "更新失败");
    }
}
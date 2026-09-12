package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocQueryDTO;
import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.aiedge.erp.sale.service.ISaleReturnDocService;
import cn.aiedge.erp.sale.service.ISaleExchangeService;
import cn.aiedge.erp.sale.service.UnifiedSalesDocQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
@SaCheckLogin
public class SalesDocQueryController {

    @Autowired
    private UnifiedSalesDocQueryService unifiedSalesDocQueryService;

    @Autowired
    private ISaleOrderService saleOrderService;

    @Autowired
    private SaleOutboundService saleOutboundService;

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

    /** 整单备注最大长度（erp_sale_order / erp_sale_outbound / erp_sale_exchange.remark 为 varchar(500)） */
    private static final int REMARK_MAX_LENGTH = 500;

    /**
     * 更新单据整单备注
     *
     * <p>只更新 remark 列（不整行覆盖），避免把并发的其它字段修改回滚。</p>
     *
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
        if (remark == null) {
            remark = "";
        }
        if (remark.length() > REMARK_MAX_LENGTH) {
            return Map.of("success", false,
                    "message", "整单备注超长，最多 " + REMARK_MAX_LENGTH + " 个字符");
        }

        boolean updated;
        switch (docType) {
            case "SALE_ORDER":
                if (saleOrderService.getById(id) == null) {
                    return Map.of("success", false, "message", "单据不存在或已删除");
                }
                updated = saleOrderService.update(new UpdateWrapper<SaleOrder>()
                        .eq("id", id).set("remark", remark));
                break;
            case "OUTBOUND":
                if (saleOutboundService.getById(id) == null) {
                    return Map.of("success", false, "message", "单据不存在或已删除");
                }
                updated = saleOutboundService.update(new UpdateWrapper<SaleOutbound>()
                        .eq("id", id).set("remark", remark));
                break;
            case "RETURN":
                if (saleReturnDocService.getById(id) == null) {
                    return Map.of("success", false, "message", "单据不存在或已删除");
                }
                updated = saleReturnDocService.update(new UpdateWrapper<SaleReturnDoc>()
                        .eq("id", id).set("remark", remark));
                break;
            case "EXCHANGE":
                if (saleExchangeService.getById(id) == null) {
                    return Map.of("success", false, "message", "单据不存在或已删除");
                }
                updated = saleExchangeService.update(new UpdateWrapper<SaleExchange>()
                        .eq("id", id).set("remark", remark));
                break;
            default:
                throw new IllegalArgumentException("不支持的单据类型: " + docType);
        }

        return Map.of("success", updated, "message", updated ? "更新成功" : "更新失败");
    }
}
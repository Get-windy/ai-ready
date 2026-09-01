package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import cn.aiedge.erp.stock.service.StockBomService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生产模板（BOM）薄封装控制器
 *
 * 前端 wh/production-template 页面与组装单/拆卸单均调用 /wh/production-template/*，
 * 本控制器适配到已实现的 StockBomService，保持前端契约不变。
 */
@Slf4j
@RestController
@RequestMapping("/api/wh/production-template")
@RequiredArgsConstructor
@Tag(name = "生产模板管理", description = "成品物料清单（BOM）模板创建、维护、发布/停用")
public class ProductionTemplateController {

    private final StockBomService bomService;

    @lombok.Data
    public static class TemplateComponent {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private String spec;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal wastageRate;
        private BigDecimal unitCost;
    }

    @lombok.Data
    public static class TemplateRequest {
        private String bomNo;
        private String bomName;
        private Long productId;
        private Integer bomType;
        private String version;
        private BigDecimal outputQuantity;
        private Integer status;
        private String remark;
        private List<TemplateComponent> components;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询生产模板")
    public Result<Page<StockBom>> page(
            @Parameter(description = "模板名称关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "成品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "类型 1-组装 2-拆卸") @RequestParam(required = false) Integer bomType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(bomService.pageList(keyword, null, productName, bomType, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取生产模板详情")
    public Result<StockBom> getById(@PathVariable Long id) {
        StockBom bom = bomService.getById(id);
        if (bom == null) {
            throw BusinessException.notFound("生产模板不存在");
        }
        return Result.ok(bom);
    }

    @GetMapping("/{id}/components")
    @Operation(summary = "获取生产模板原料构成明细")
    public Result<List<StockBomItem>> getComponents(@PathVariable Long id) {
        return Result.ok(bomService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建生产模板")
    public Result<StockBom> create(@RequestBody TemplateRequest request) {
        StockBom bom = new StockBom();
        bom.setBomName(request.getBomName());
        bom.setProductId(request.getProductId());
        bom.setBomType(request.getBomType() != null ? request.getBomType() : 1);
        bom.setOutputQuantity(request.getOutputQuantity());
        bom.setStatus(request.getStatus() != null && request.getStatus() == 1 ? 1 : 0);
        bom.setRemark(request.getRemark());
        return Result.ok(bomService.createBom(bom, toItems(request.getComponents())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新生产模板")
    public Result<StockBom> update(@PathVariable Long id, @RequestBody TemplateRequest request) {
        StockBom bom = new StockBom();
        bom.setId(id);
        bom.setBomName(request.getBomName());
        bom.setProductId(request.getProductId());
        bom.setBomType(request.getBomType() != null ? request.getBomType() : 1);
        bom.setOutputQuantity(request.getOutputQuantity());
        bom.setStatus(request.getStatus() != null && request.getStatus() == 1 ? 1 : 0);
        bom.setRemark(request.getRemark());
        return Result.ok(bomService.updateBom(id, bom, toItems(request.getComponents())));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布生产模板")
    public Result<StockBom> publish(@PathVariable Long id) {
        return Result.ok(bomService.enableBom(id));
    }

    @PostMapping("/{id}/obsolete")
    @Operation(summary = "停用生产模板")
    public Result<StockBom> obsolete(@PathVariable Long id) {
        return Result.ok(bomService.disableBom(id));
    }

    private List<StockBomItem> toItems(List<TemplateComponent> components) {
        if (components == null) {
            return null;
        }
        return components.stream().map(c -> {
            StockBomItem item = new StockBomItem();
            item.setId(c.getId());
            item.setProductId(c.getProductId());
            item.setProductCode(c.getProductCode());
            item.setProductName(c.getProductName());
            item.setProductSpec(c.getSpec());
            item.setProductUnit(c.getUnit());
            item.setQuantity(c.getQuantity());
            item.setWastageRate(c.getWastageRate());
            item.setUnitCost(c.getUnitCost());
            return item;
        }).collect(Collectors.toList());
    }
}

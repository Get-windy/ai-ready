package cn.aiedge.erp.supplier.controller;

import cn.aiedge.erp.supplier.model.entity.InquiryQuotationEntity;
import cn.aiedge.erp.supplier.model.entity.SupplierPointsRecordEntity;
import cn.aiedge.erp.supplier.service.SupplierPortalService;
import cn.aiedge.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商门户查询控制器
 * 提供前端 /supplier-portal/** 路径的查询接口
 */
@RestController
@RequestMapping("/api/supplier-portal")
@Tag(name = "供应商门户查询", description = "供应商询价单、积分记录查询接口")
@RequiredArgsConstructor
public class SupplierPortalInquiryController {

    private final SupplierPortalService supplierPortalService;

    @GetMapping("/inquiries/supplier/{supplierId}")
    @Operation(summary = "获取供应商询价单")
    public R<List<InquiryQuotationEntity>> getSupplierInquiries(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getSupplierInquiries(supplierId));
    }

    @GetMapping("/points/{supplierId}/records")
    @Operation(summary = "获取供应商积分记录")
    public R<List<SupplierPointsRecordEntity>> getPointsRecords(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getPointsRecords(supplierId));
    }
}

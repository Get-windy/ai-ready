package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 业务记账集成Controller (内部接口)
 * 供采购、销售、费用等业务模块调用，自动生成会计凭证
 */
@Tag(name = "业务记账集成", description = "业务模块调用的内部记账接口，无需权限校验")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/integration")
@RequiredArgsConstructor
public class BusinessAccountingController {

    private final BusinessAccountingService businessAccountingService;

    @Operation(summary = "从业务创建凭证")
    @PostMapping("/voucher")
    @OperationLog(module = "业务记账集成", type = "CREATE", desc = "从业务创建凭证")
    public Result<VoucherDTO> createVoucherFromBusiness(@Valid @RequestBody BusinessAccountingRequest request) {
        VoucherDTO result = businessAccountingService.createVoucherFromBusiness(request);
        return Result.success("凭证创建成功", result);
    }

    @Operation(summary = "从业务创建应收款")
    @PostMapping("/receivable")
    @OperationLog(module = "业务记账集成", type = "CREATE", desc = "从业务创建应收款")
    public Result<ReceivableDTO> createReceivableFromBusiness(@Valid @RequestBody BusinessAccountingRequest request) {
        ReceivableDTO result = businessAccountingService.createReceivableFromBusiness(request);
        return Result.success("应收款创建成功", result);
    }

    @Operation(summary = "从业务创建应付款")
    @PostMapping("/payable")
    @OperationLog(module = "业务记账集成", type = "CREATE", desc = "从业务创建应付款")
    public Result<PayableDTO> createPayableFromBusiness(@Valid @RequestBody BusinessAccountingRequest request) {
        PayableDTO result = businessAccountingService.createPayableFromBusiness(request);
        return Result.success("应付款创建成功", result);
    }
}

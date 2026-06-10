package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.PayableCreateRequest;
import cn.aiedge.finance.dto.PayableUpdateRequest;
import cn.aiedge.finance.dto.PayableQueryRequest;
import cn.aiedge.finance.dto.PayableVO;
import cn.aiedge.finance.service.IPayableService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应付账款控制器
 */
@RestController
@RequestMapping("/api/finance/payable")
@Tag(name = "应付账款管理", description = "应付账款相关操作接口")
@SaCheckLogin
public class PayableController {

    private final IPayableService payableService;

    public PayableController(IPayableService payableService) {
        this.payableService = payableService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建应付账款")
    public ApiResponse<Long> createPayable(@Valid @RequestBody PayableCreateRequest request) {
        Long id = payableService.createPayable(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新应付账款")
    public ApiResponse<Void> updatePayable(@Valid @RequestBody PayableUpdateRequest request) {
        payableService.updatePayable(request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取应付账款详情")
    public ApiResponse<PayableVO> getPayableById(@PathVariable Long id) {
        PayableVO payableVO = payableService.getPayableById(id);
        return ApiResponse.success(payableVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除应付账款")
    public ApiResponse<Void> deletePayable(@PathVariable Long id) {
        payableService.deletePayable(id);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询应付账款")
    public ApiResponse<PageResult<PayableVO>> pagePayables(@RequestBody PayableQueryRequest request) {
        Page<PayableVO> pageResult = payableService.pagePayables(request);
        PageResult<PayableVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除应付账款")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        payableService.batchDelete(ids);
        return ApiResponse.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出应付账款")
    public ApiResponse<List<PayableVO>> export(PayableQueryRequest request) {
        return ApiResponse.success(payableService.exportList(request));
    }
}

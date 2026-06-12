package cn.aiedge.dms.sign.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.sign.entity.DmsSign;
import cn.aiedge.dms.sign.service.SignService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 签收管理控制器
 *
 * 提供签收提交、签收记录查询等 REST 接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "签收管理")
@RestController
@RequestMapping("/api/dms/sign")
@RequiredArgsConstructor
@SaCheckLogin
public class SignController {

    private final SignService signService;

    @Operation(summary = "提交签收")
    @PostMapping("/submit")
    @SaCheckPermission("dms:sign:submit")
    public ApiResponse<Void> submit(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "签收类型：1-正常 2-部分 3-拒收") @RequestParam Integer signType,
            @Parameter(description = "照片URL列表(JSON)") @RequestParam(required = false) String photoUrls,
            @Parameter(description = "手写签名图片URL") @RequestParam(required = false) String signatureUrl,
            @Parameter(description = "签收纬度") @RequestParam BigDecimal signLat,
            @Parameter(description = "签收经度") @RequestParam BigDecimal signLng,
            @Parameter(description = "客户纬度") @RequestParam BigDecimal customerLat,
            @Parameter(description = "客户经度") @RequestParam BigDecimal customerLng,
            @Parameter(description = "偏差阈值(米)") @RequestParam(defaultValue = "100") double deviationThresh) {
        signService.submit(taskId, signType, photoUrls, signatureUrl, signLat, signLng,
                customerLat, customerLng, deviationThresh);
        return ApiResponse.success();
    }

    @Operation(summary = "根据任务ID获取签收记录")
    @GetMapping("/{taskId}")
    public ApiResponse<DmsSign> getByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        DmsSign sign = signService.getByTaskId(taskId);
        return ApiResponse.success(sign);
    }
}

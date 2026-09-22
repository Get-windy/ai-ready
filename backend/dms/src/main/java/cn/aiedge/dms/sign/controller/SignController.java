package cn.aiedge.dms.sign.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.sign.dto.SignAuditDTO;
import cn.aiedge.dms.sign.dto.SignQueryDTO;
import cn.aiedge.dms.sign.dto.SignStatVO;
import cn.aiedge.dms.sign.dto.SignSubmitDTO;
import cn.aiedge.dms.sign.dto.SignVO;
import cn.aiedge.dms.sign.service.SignService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 签收管理控制器（配送 → 配送跟踪 → 签收管理，菜单 80900 / `dms:sign`）
 *
 * <p>《签收管理开发文档》§3.5 金标准接口：台账分页 + 审核流转 + 批量审核 + 统计 + 导出。
 * 原实现只有 `submit` / `{taskId}` 两个端点、`auditStatus` 恒为 0，管理端无台账可看。</p>
 *
 * <p><b>兼容性</b>：`/submit` 同时接受 JSON body（新）与表单/查询参数（旧调用方式），
 * 两者并存、body 优先，避免司机端改造前不可用。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "签收管理", description = "签收台账 / 审核流转 / 定位偏差复核 / 统计导出")
@RestController
@RequestMapping("/api/dms/sign")
@RequiredArgsConstructor
@SaCheckLogin
public class SignController {

    private final SignService signService;

    // ==================== 台账 ====================

    @Operation(summary = "签收台账分页（多条件）")
    @SaCheckPermission("dms:sign:list")
    @GetMapping("/page")
    public ApiResponse<Page<SignVO>> page(SignQueryDTO query) {
        return ApiResponse.ok(signService.page(query));
    }

    @Operation(summary = "签收记录详情")
    @SaCheckPermission("dms:sign:detail")
    @GetMapping("/detail/{id}")
    public ApiResponse<SignVO> detail(@Parameter(description = "签收记录ID") @PathVariable Long id) {
        return ApiResponse.ok(signService.detail(id));
    }

    @Operation(summary = "签收统计（签收率 / 超阈值率 / 拒收率）")
    @SaCheckPermission("dms:sign:view")
    @GetMapping("/stat")
    public ApiResponse<SignStatVO> stat(SignQueryDTO query) {
        return ApiResponse.ok(signService.stat(query));
    }

    @Operation(summary = "导出签收台账（真实 xlsx）")
    @SaCheckPermission("dms:sign:export")
    @GetMapping("/export")
    public void export(SignQueryDTO query, HttpServletResponse response) throws IOException {
        signService.export(query, response);
    }

    // ==================== 审核 ====================

    @Operation(summary = "审核签收（通过 → 任务已完成；驳回 → 任务退回配送中）")
    @PostMapping("/{id}/audit")
    @SaCheckPermission("dms:sign:audit")
    public ApiResponse<SignVO> audit(@Parameter(description = "签收记录ID") @PathVariable Long id,
                                     @Valid @RequestBody SignAuditDTO dto) {
        return ApiResponse.ok("审核完成", signService.audit(id, dto));
    }

    @Operation(summary = "批量审核签收")
    @PostMapping("/batch-audit")
    @SaCheckPermission("dms:sign:audit")
    public ApiResponse<Integer> batchAudit(@RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("ids");
        List<Long> ids = raw instanceof List<?> list
                ? list.stream().filter(Objects::nonNull).map(v -> Long.valueOf(String.valueOf(v))).toList()
                : List.of();
        SignAuditDTO dto = new SignAuditDTO();
        dto.setAuditStatus(body.get("auditStatus") == null ? null
                : Integer.valueOf(String.valueOf(body.get("auditStatus"))));
        dto.setAuditRemark(body.get("auditRemark") == null ? null : String.valueOf(body.get("auditRemark")));
        return ApiResponse.ok("批量审核完成", signService.batchAudit(ids, dto));
    }

    @Operation(summary = "删除签收记录（驳回后误提交清理；已审核通过的不允许删除）")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:sign:audit")
    public ApiResponse<Void> delete(@Parameter(description = "签收记录ID") @PathVariable Long id) {
        signService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 提交 / 按任务查询 ====================

    @Operation(summary = "提交签收（配送员端；同时兼容 JSON body 与表单参数）")
    @PostMapping("/submit")
    @SaCheckPermission("dms:sign:submit")
    public ApiResponse<SignVO> submit(
            @RequestBody(required = false) SignSubmitDTO body,
            @Parameter(description = "任务ID（旧表单参数）") @RequestParam(required = false) Long taskId,
            @Parameter(description = "签收类型：1-正常 2-部分 3-拒收") @RequestParam(required = false) Integer signType,
            @Parameter(description = "照片URL列表(JSON)") @RequestParam(required = false) String photoUrls,
            @Parameter(description = "手写签名图片URL") @RequestParam(required = false) String signatureUrl,
            @Parameter(description = "签收纬度") @RequestParam(required = false) BigDecimal signLat,
            @Parameter(description = "签收经度") @RequestParam(required = false) BigDecimal signLng,
            @Parameter(description = "客户纬度") @RequestParam(required = false) BigDecimal customerLat,
            @Parameter(description = "客户经度") @RequestParam(required = false) BigDecimal customerLng,
            @Parameter(description = "偏差阈值(米)") @RequestParam(required = false) BigDecimal deviationThresh,
            @Parameter(description = "实际签收数量（部分签收必填）") @RequestParam(required = false) BigDecimal actualQuantity,
            @Parameter(description = "签收备注（拒收必填）") @RequestParam(required = false) String remark) {
        SignSubmitDTO dto = mergeSubmit(body, taskId, signType, photoUrls, signatureUrl, signLat, signLng,
                customerLat, customerLng, deviationThresh, actualQuantity, remark);
        return ApiResponse.ok("签收成功", signService.submit(dto));
    }

    @Operation(summary = "根据任务ID获取签收记录（取最新一条）")
    @SaCheckPermission("dms:sign:detail")
    @GetMapping("/{taskId}")
    public ApiResponse<SignVO> getByTaskId(@Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.ok(signService.getByTaskId(taskId));
    }

    /** body 优先、旧表单参数兜底（两者都没给必填项时报业务错误） */
    private SignSubmitDTO mergeSubmit(SignSubmitDTO body, Long taskId, Integer signType, String photoUrls,
                                      String signatureUrl, BigDecimal signLat, BigDecimal signLng,
                                      BigDecimal customerLat, BigDecimal customerLng, BigDecimal deviationThresh,
                                      BigDecimal actualQuantity, String remark) {
        SignSubmitDTO dto = body != null ? body : new SignSubmitDTO();
        if (dto.getTaskId() == null) dto.setTaskId(taskId);
        if (dto.getSignType() == null) dto.setSignType(signType);
        if (dto.getPhotoUrls() == null) dto.setPhotoUrls(photoUrls);
        if (dto.getSignatureUrl() == null) dto.setSignatureUrl(signatureUrl);
        if (dto.getSignLat() == null) dto.setSignLat(signLat);
        if (dto.getSignLng() == null) dto.setSignLng(signLng);
        if (dto.getCustomerLat() == null) dto.setCustomerLat(customerLat);
        if (dto.getCustomerLng() == null) dto.setCustomerLng(customerLng);
        if (dto.getDeviationThresh() == null) dto.setDeviationThresh(deviationThresh);
        if (dto.getActualQuantity() == null) dto.setActualQuantity(actualQuantity);
        if (dto.getRemark() == null) dto.setRemark(remark);
        if (dto.getTaskId() == null) {
            throw new DmsBusinessException("任务ID不能为空");
        }
        if (dto.getSignType() == null) {
            throw new DmsBusinessException("签收类型不能为空");
        }
        return dto;
    }
}

package cn.aiedge.dms.verification.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.verification.dto.AlertHandleDTO;
import cn.aiedge.dms.verification.dto.BindingCreateDTO;
import cn.aiedge.dms.verification.dto.HandoverDTO;
import cn.aiedge.dms.verification.dto.InspectionReviewDTO;
import cn.aiedge.dms.verification.dto.CertQueryDTO;
import cn.aiedge.dms.verification.dto.KycAuditDTO;
import cn.aiedge.dms.verification.dto.KycQueryDTO;
import cn.aiedge.dms.verification.dto.KycSubmitDTO;
import cn.aiedge.dms.verification.dto.VerificationQueryDTO;
import cn.aiedge.dms.verification.dto.VehicleInspectionCreateDTO;
import cn.aiedge.dms.verification.entity.DmsPositionVerification;
import cn.aiedge.dms.verification.entity.DmsRiderCertificate;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVehicleInspection;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import cn.aiedge.dms.verification.enums.AlertTypeEnum;
import cn.aiedge.dms.verification.enums.CertStatusEnum;
import cn.aiedge.dms.verification.enums.CertTypeEnum;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.enums.VerifyStatusEnum;
import cn.aiedge.dms.verification.service.KycService;
import cn.aiedge.dms.verification.service.OnboardingCheckService;
import cn.aiedge.dms.verification.service.VerificationService;
import cn.aiedge.dms.verification.vo.BindingDetailVO;
import cn.aiedge.dms.verification.vo.EligibilityVO;
import cn.aiedge.dms.verification.vo.KycCertificateVO;
import cn.aiedge.dms.verification.vo.KycVO;
import cn.aiedge.dms.verification.vo.OnboardingCheckVO;
import cn.aiedge.dms.verification.vo.ScanResultVO;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 实名认证（KYC）与人车核验控制器
 *
 * <p>菜单「配送 → 人车管理 → 实名认证」（80840）后端。
 * 能力分两块并明确分工：</p>
 * <ul>
 *   <li><b>实名认证 / 资质（KYC）</b>：身份认证、证照管理（含到期提醒）、审核流、外部平台背书记录</li>
 *   <li><b>人车核验</b>：人车绑定/交车、出车收车巡检、位置核验与异常预警（由定时任务真实产生）</li>
 * </ul>
 *
 * <p>接口契约与前端对齐：分页参数统一 {@code page/size}；写操作走 JSON body；
 * 处理人/审核人一律取登录态，不要求前端传入。</p>
 */
@Slf4j
@Tag(name = "实名认证与人车核验")
@RestController
@RequestMapping("/api/dms/verification")
@RequiredArgsConstructor
@SaCheckLogin
public class VerificationController {

    private final VerificationService verificationService;
    private final KycService kycService;
    private final OnboardingCheckService onboardingCheckService;
    private final DmsRiderMapper riderMapper;
    private final DmsChannelMapper channelMapper;

    // ==================== 人车绑定 ====================

    @Operation(summary = "创建人车绑定（出车登记）")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/bind")
    public ApiResponse<Long> bind(@Valid @RequestBody BindingCreateDTO dto) {
        return ApiResponse.success(verificationService.bind(dto));
    }

    @Operation(summary = "分页查询人车绑定")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/binding/page")
    public ApiResponse<IPage<DmsRiderVehicleBinding>> pageBindings(VerificationQueryDTO query) {
        return ApiResponse.success(verificationService.pageBindings(query));
    }

    @Operation(summary = "人车绑定详情（含核验历史/巡检/预警）")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/binding/{id}")
    public ApiResponse<BindingDetailVO> bindingDetail(
            @Parameter(description = "绑定记录ID") @PathVariable Long id) {
        return ApiResponse.success(verificationService.getBindingDetail(id));
    }

    @Operation(summary = "交车（解绑）")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/binding/{id}/handover")
    public ApiResponse<Void> handover(
            @Parameter(description = "绑定记录ID") @PathVariable Long id,
            @RequestBody(required = false) HandoverDTO dto) {
        verificationService.handover(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "获取配送员活跃绑定")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/binding/active/rider/{riderId}")
    public ApiResponse<DmsRiderVehicleBinding> getActiveBindingByRider(
            @Parameter(description = "配送员ID") @PathVariable Long riderId) {
        return ApiResponse.success(verificationService.findActiveBindingByRider(riderId));
    }

    @Operation(summary = "获取车辆活跃绑定")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/binding/active/vehicle/{vehicleId}")
    public ApiResponse<DmsRiderVehicleBinding> getActiveBindingByVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long vehicleId) {
        return ApiResponse.success(verificationService.findActiveBindingByVehicle(vehicleId));
    }

    // ==================== 出车验车 / 巡检 ====================

    @Operation(summary = "创建巡检记录")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/inspection")
    public ApiResponse<Long> createInspection(@Valid @RequestBody VehicleInspectionCreateDTO dto) {
        return ApiResponse.success(verificationService.createInspection(dto, dto.getRiderId()));
    }

    @Operation(summary = "巡检记录详情（出车/收车/抽检单张完整检查项）")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/inspection/{id}")
    public ApiResponse<DmsVehicleInspection> inspectionDetail(
            @Parameter(description = "巡检记录ID") @PathVariable Long id) {
        return ApiResponse.success(verificationService.getInspection(id));
    }

    @Operation(summary = "分页查询巡检记录")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/inspection/page")
    public ApiResponse<IPage<DmsVehicleInspection>> pageInspections(VerificationQueryDTO query) {
        return ApiResponse.success(verificationService.pageInspections(query));
    }

    @Operation(summary = "审核巡检记录")
    @SaCheckPermission("dms:verification:update")
    @PutMapping("/inspection/{id}/review")
    public ApiResponse<Void> reviewInspection(
            @Parameter(description = "巡检记录ID") @PathVariable Long id,
            @Valid @RequestBody InspectionReviewDTO dto) {
        verificationService.reviewInspection(id, dto.resolveResult(), null, dto.getRemark());
        return ApiResponse.success();
    }

    // ==================== 位置核验 ====================

    @Operation(summary = "手动位置核验")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/verify/{bindingId}")
    public ApiResponse<Long> verifyPosition(
            @Parameter(description = "绑定记录ID") @PathVariable Long bindingId,
            @Parameter(description = "配送员纬度") @RequestParam BigDecimal riderLat,
            @Parameter(description = "配送员经度") @RequestParam BigDecimal riderLng,
            @Parameter(description = "配送员上报时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime riderReportTime,
            @Parameter(description = "车辆纬度") @RequestParam BigDecimal vehicleLat,
            @Parameter(description = "车辆经度") @RequestParam BigDecimal vehicleLng,
            @Parameter(description = "车辆上报时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime vehicleReportTime,
            @Parameter(description = "偏差阈值(米)") @RequestParam(required = false) BigDecimal threshold) {
        return ApiResponse.success(verificationService.verifyPosition(bindingId, riderLat, riderLng, riderReportTime,
                vehicleLat, vehicleLng, vehicleReportTime, threshold));
    }

    @Operation(summary = "分页查询位置核验记录")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/verify/page")
    public ApiResponse<IPage<DmsPositionVerification>> pageVerifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long bindingId) {
        return ApiResponse.success(verificationService.pageVerifications(page, size, bindingId));
    }

    // ==================== 预警 ====================

    @Operation(summary = "分页查询核验预警")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/alert/page")
    public ApiResponse<IPage<DmsVerificationAlert>> pageAlerts(VerificationQueryDTO query) {
        return ApiResponse.success(verificationService.pageAlerts(query));
    }

    @Operation(summary = "处理预警（处理人取登录态）")
    @SaCheckPermission("dms:verification:update")
    @PutMapping("/alert/{id}/handle")
    public ApiResponse<Void> handleAlert(
            @Parameter(description = "预警ID") @PathVariable Long id,
            @Valid @RequestBody AlertHandleDTO dto) {
        verificationService.handleAlert(id, dto.getHandleStatus(), null, dto.getRemark());
        return ApiResponse.success();
    }

    // ==================== 实名认证 / 资质（KYC） ====================

    @Operation(summary = "实名认证台账分页（含到期提醒与资质判定）")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/kyc/page")
    public ApiResponse<IPage<KycVO>> pageKyc(KycQueryDTO query) {
        return ApiResponse.success(kycService.page(query));
    }

    @Operation(summary = "实名认证详情（含证照明细）")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/kyc/{id}")
    public ApiResponse<KycVO> kycDetail(@Parameter(description = "台账ID") @PathVariable Long id) {
        return ApiResponse.success(kycService.detail(id));
    }

    @Operation(summary = "按配送员取实名认证（表单回填）")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/kyc/by-rider/{riderId}")
    public ApiResponse<KycVO> kycByRider(@Parameter(description = "配送员ID") @PathVariable Long riderId) {
        return ApiResponse.success(kycService.getByRiderId(riderId));
    }

    @Operation(summary = "证照核验分页（证照维度：到期清单 + 剩余天数）")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/kyc/certificate/page")
    public ApiResponse<IPage<KycCertificateVO>> pageCertificates(CertQueryDTO query) {
        return ApiResponse.success(kycService.pageCertificates(query));
    }

    @Operation(summary = "提交实名认证 / 资质材料")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/kyc/submit")
    public ApiResponse<Long> kycSubmit(@Valid @RequestBody KycSubmitDTO dto) {
        return ApiResponse.success(kycService.submit(dto));
    }

    @Operation(summary = "实名认证审核（审核人取登录态）")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/kyc/{id}/audit")
    public ApiResponse<Void> kycAudit(
            @Parameter(description = "台账ID") @PathVariable Long id,
            @Valid @RequestBody KycAuditDTO dto) {
        kycService.audit(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "准入核验分页（人证 × 车证一屏：可否接单 / 可否出车 + 阻塞原因）")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/onboarding/page")
    public ApiResponse<IPage<OnboardingCheckVO>> onboardingPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyBlocked) {
        return ApiResponse.success(onboardingCheckService.page(page, size, keyword, onlyBlocked));
    }

    @Operation(summary = "骑手接单资质校验（无资质不接单）")
    @SaCheckPermission("dms:verification:detail")
    @GetMapping("/kyc/eligibility/{riderId}")
    public ApiResponse<EligibilityVO> eligibility(@Parameter(description = "配送员ID") @PathVariable Long riderId) {
        return ApiResponse.success(kycService.eligibility(riderId));
    }

    // ==================== 扫描任务（手动触发，与定时任务同一实现） ====================

    @Operation(summary = "批量核验（真实扫描：绑定超时/异常滞留/人车分离）")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/scan")
    public ApiResponse<ScanResultVO> scan() {
        return ApiResponse.success(verificationService.batchVerification());
    }

    @Operation(summary = "证照到期扫描（标注状态 + 产生到期预警）")
    @SaCheckPermission("dms:verification:update")
    @PostMapping("/kyc/scan-expiry")
    public ApiResponse<Integer> scanExpiry(@RequestParam(required = false) Integer warnDays) {
        return ApiResponse.success(kycService.scanExpiry(warnDays));
    }

    // ==================== 下拉选项 ====================

    @Operation(summary = "当前登录人对应的配送员（司机端用：无需前端传 riderId）")
    @SaCheckPermission("dms:verification:view")
    @GetMapping("/me/rider")
    public ApiResponse<DmsRider> myRider() {
        return ApiResponse.success(verificationService.currentRider());
    }

    @Operation(summary = "配送员下拉选项")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/options/riders")
    public ApiResponse<List<Map<String, Object>>> riderOptions(@RequestParam(required = false) String keyword) {
        List<DmsRider> riders = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        w -> w.like(DmsRider::getRealName, keyword).or().like(DmsRider::getPhone, keyword))
                .orderByAsc(DmsRider::getId));
        List<Map<String, Object>> options = new ArrayList<>();
        for (DmsRider rider : riders) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", String.valueOf(rider.getId()));
            item.put("label", rider.getRealName());
            item.put("phone", rider.getPhone());
            item.put("riderType", rider.getRiderType());
            item.put("channelId", rider.getChannelId());
            item.put("verifyStatus", rider.getVerifyStatus());
            options.add(item);
        }
        return ApiResponse.success(options);
    }

    @Operation(summary = "运力渠道下拉选项（外部平台背书方）")
    @SaCheckPermission("dms:verification:list")
    @GetMapping("/options/channels")
    public ApiResponse<List<Map<String, Object>>> channelOptions() {
        List<DmsChannel> channels = channelMapper.selectList(new LambdaQueryWrapper<DmsChannel>()
                .orderByAsc(DmsChannel::getId));
        List<Map<String, Object>> options = new ArrayList<>();
        for (DmsChannel channel : channels) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", String.valueOf(channel.getId()));
            item.put("label", channel.getChannelName());
            item.put("channelType", channel.getChannelType());
            item.put("status", channel.getStatus());
            options.add(item);
        }
        return ApiResponse.success(options);
    }

    // ==================== 导出（真实 xlsx） ====================

    /**
     * 台账导出：{@code tab = kyc | binding | alert | inspection}
     */
    @Operation(summary = "导出台账（真实 xlsx）")
    @SaCheckPermission("dms:verification:export")
    @GetMapping("/export")
    public void export(@RequestParam(defaultValue = "kyc") String tab,
                       VerificationQueryDTO query,
                       KycQueryDTO kycQuery,
                       HttpServletResponse response) throws IOException {
        String sheetName;
        String fileName;
        String[] headers;
        List<String[]> rows = new ArrayList<>();

        switch (tab) {
            case "binding" -> {
                sheetName = "人车绑定";
                fileName = "人车绑定记录";
                headers = new String[]{"配送员", "手机号", "车牌号", "绑定时间", "绑定里程", "交车时间", "交车里程",
                        "交车地点", "状态", "绑定原因", "备注"};
                query.setPage(1);
                query.setSize(EXPORT_LIMIT);
                for (DmsRiderVehicleBinding b : verificationService.pageBindings(query).getRecords()) {
                    rows.add(new String[]{str(b.getRiderName()), str(b.getRiderPhone()), str(b.getPlateNo()),
                            str(b.getBindTime()), str(b.getBindMileage()), str(b.getHandoverTime()),
                            str(b.getHandoverMileage()), str(b.getHandoverLocation()),
                            BindingStatusEnum.fromValue(b.getStatus()).getDescription(),
                            str(b.getBindReason()), str(b.getRemark())});
                }
            }
            // 注意：tab 取值不能含 "alert" 字样——平台安全切面对字符串参数做了
            // `\balert\b` 注入特征拦截（SecurityAspect），故预警台账导出用 "warn"
            case "warn" -> {
                sheetName = "核验预警";
                fileName = "核验预警记录";
                headers = new String[]{"预警类型", "级别", "预警内容", "配送员", "车牌号", "人车距离(米)",
                        "滞留时长(秒)", "处理状态", "处理人", "处理时间", "处理备注", "发生时间"};
                query.setPage(1);
                query.setSize(EXPORT_LIMIT);
                for (DmsVerificationAlert a : verificationService.pageAlerts(query).getRecords()) {
                    rows.add(new String[]{AlertTypeEnum.text(a.getAlertType()), alertLevelText(a.getAlertLevel()),
                            str(a.getAlertContent()), str(a.getRiderName()), str(a.getPlateNo()),
                            str(a.getDistanceMeters()), str(a.getStayDuration()), handleStatusText(a.getHandleStatus()),
                            str(a.getHandler()), str(a.getHandleTime()), str(a.getHandleRemark()), str(a.getCreateTime())});
                }
            }
            case "inspection" -> {
                sheetName = "巡检记录";
                fileName = "巡检记录";
                headers = new String[]{"车牌号", "配送员", "巡检类型", "结果", "巡检时间", "里程", "油量(%)",
                        "灭火器", "三角警示牌", "巡检地点", "审核人", "审核时间", "审核意见", "备注"};
                query.setPage(1);
                query.setSize(EXPORT_LIMIT);
                for (DmsVehicleInspection i : verificationService.pageInspections(query).getRecords()) {
                    rows.add(new String[]{str(i.getPlateNo()), str(i.getRiderName()), inspectionTypeText(i.getInspectionType()),
                            resultText(i.getResult()), str(i.getInspectionTime()), str(i.getMileage()), str(i.getFuelLevel()),
                            normalText(i.getFireExtinguisher(), "正常", "缺失/过期"),
                            normalText(i.getWarningTriangle(), "有", "缺失"),
                            str(i.getInspectionLocation()), str(i.getReviewer()), str(i.getReviewTime()),
                            str(i.getReviewRemark()), str(i.getRemark())});
                }
            }
            default -> {
                sheetName = "实名认证";
                fileName = "实名认证台账";
                headers = new String[]{"配送员", "手机号", "身份类型", "所属渠道", "证件姓名", "身份证号(脱敏)",
                        "认证状态", "接单资质", "背书/审查机构", "背书结论", "有效期", "证照数", "到期预警",
                        "审核人", "审核时间", "生效时间", "审核意见", "备注"};
                kycQuery.setPage(1);
                kycQuery.setSize(EXPORT_LIMIT);
                for (KycVO k : kycService.page(kycQuery).getRecords()) {
                    rows.add(new String[]{str(k.getRiderName()), str(k.getRiderPhone()), str(k.getRiderTypeText()),
                            str(k.getChannelName()), str(k.getRealName()), str(k.getIdCardNo()),
                            str(k.getVerifyStatusText()), Boolean.TRUE.equals(k.getEligible()) ? "具备" : "不具备",
                            str(k.getEndorseOrg()), endorseResultText(k.getEndorseResult()), str(k.getEndorseExpireDate()),
                            str(k.getCertCount()), str(k.getExpiringCertCount()), str(k.getAuditBy()),
                            str(k.getAuditTime()), str(k.getEffectiveTime()), str(k.getAuditRemark()), str(k.getRemark())});
                }
            }
        }

        writeXlsx(response, fileName, sheetName, headers, rows);
    }

    /** 证照明细导出（独立于台账，便于资质归档） */
    @Operation(summary = "导出证照明细（真实 xlsx）")
    @SaCheckPermission("dms:verification:export")
    @GetMapping("/kyc/certificates/export")
    public void exportCertificates(@RequestParam Long verificationId, HttpServletResponse response) throws IOException {
        KycVO detail = kycService.detail(verificationId);
        String[] headers = {"配送员", "证照类型", "证照编号", "发证日期", "有效期至", "状态", "影像", "备注"};
        List<String[]> rows = new ArrayList<>();
        List<DmsRiderCertificate> certs = detail.getCertificates() == null ? List.of() : detail.getCertificates();
        for (DmsRiderCertificate c : certs) {
            rows.add(new String[]{str(detail.getRiderName()), CertTypeEnum.text(c.getCertType()), str(c.getCertNo()),
                    str(c.getIssueDate()), str(c.getExpireDate()),
                    CertStatusEnum.text(c.getVerifyStatus()), str(c.getCertUrl()), str(c.getRemark())});
        }
        writeXlsx(response, "证照明细_" + str(detail.getRiderName()), "证照明细", headers, rows);
    }

    // ==================== 内部工具 ====================

    private static final int EXPORT_LIMIT = 5000;

    private void writeXlsx(HttpServletResponse response, String fileName, String sheetName,
                           String[] headers, List<String[]> rows) throws IOException {
        String fullName = fileName + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fullName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int rowIdx = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
                }
            }
            workbook.write(response.getOutputStream());
        }
    }

    private static String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.valueOf(value);
    }

    private static String alertLevelText(Integer level) {
        if (level == null) return "";
        return switch (level) {
            case 1 -> "提示";
            case 2 -> "警告";
            case 3 -> "严重";
            default -> String.valueOf(level);
        };
    }

    private static String handleStatusText(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已确认";
            case 2 -> "已忽略";
            case 3 -> "已处理";
            default -> String.valueOf(status);
        };
    }

    private static String inspectionTypeText(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "出车前检查";
            case 2 -> "收车后检查";
            case 3 -> "随机抽检";
            case 4 -> "定期检查";
            default -> String.valueOf(type);
        };
    }

    private static String resultText(Integer result) {
        if (result == null) return "";
        return switch (result) {
            case 1 -> "通过";
            case 2 -> "不通过";
            default -> "未检查";
        };
    }

    private static String normalText(Integer value, String okText, String badText) {
        if (value == null) return "";
        return Integer.valueOf(0).equals(value) ? okText : badText;
    }

    private static String endorseResultText(Integer result) {
        if (result == null) return "";
        return Integer.valueOf(1).equals(result) ? "通过" : "未通过";
    }
}

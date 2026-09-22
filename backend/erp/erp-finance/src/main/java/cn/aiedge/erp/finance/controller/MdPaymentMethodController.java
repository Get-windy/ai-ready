package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.model.dto.PaymentMethodBatchDTO;
import cn.aiedge.erp.finance.model.dto.PaymentMethodQuery;
import cn.aiedge.erp.finance.model.dto.PaymentMethodStatusDTO;
import cn.aiedge.erp.finance.model.dto.PaymentMethodVO;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import cn.aiedge.erp.finance.service.PaymentMethodService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 支付方式主数据Controller（资料 → 支付管理 → 支付方式，菜单 80550）
 *
 * 对应表 md_payment_method（V11.29.0 新增）。
 * 单一口径：收款/付款/预收/预付单据「支付方式」字段统一引用本字典（GET /list 仅返回启用项），
 * 严禁另建重复支付方式字典（《支付方式开发文档》最终裁决 P0 红线）。
 *
 * ⚠️ 鉴权口径同 FinanceAccountController：本系统走 sa-token 登录拦截，
 *    勿加 Spring Security @PreAuthorize（sa-token 不填充 SecurityContext，会恒抛 AccessDeniedException）。
 */
@Tag(name = "支付方式管理", description = "支付方式字典增删改查、启停、批量操作、导出")
@Slf4j
@RestController
@RequestMapping("/api/erp/md/payment-method")
@RequiredArgsConstructor
public class MdPaymentMethodController {

    private static final Map<String, String> METHOD_TYPE_TEXT = Map.of(
            "CASH", "现金",
            "BANK", "银行转账",
            "WECHAT", "微信",
            "ALIPAY", "支付宝",
            "CHECK", "支票",
            "OTHER", "其他");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentMethodService paymentMethodService;

    @Operation(summary = "分页查询支付方式")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "分页查询支付方式")
    @SaCheckPermission("md:payment-method:list")
    @GetMapping("/page")
    public Result<Page<PaymentMethodVO>> page(PaymentMethodQuery query) {
        return Result.success(paymentMethodService.pageQuery(query));
    }

    @Operation(summary = "查询支付方式详情")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "查询支付方式详情")
    @SaCheckPermission("md:payment-method:detail")
    @GetMapping("/{id}")
    public Result<PaymentMethodVO> getById(@PathVariable Long id) {
        return Result.success(paymentMethodService.getDetail(id));
    }

    @Operation(summary = "查询所有启用的支付方式（下拉选择）")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "查询下拉列表")
    @SaCheckPermission("md:payment-method:list")
    @GetMapping("/list")
    public Result<List<PaymentMethodVO>> list() {
        return Result.success(paymentMethodService.listEnabled());
    }

    @Operation(summary = "新增支付方式")
    @OperationLog(module = "支付方式管理", type = "CREATE", desc = "新增支付方式")
    @SaCheckPermission("md:payment-method:create")
    @PostMapping
    public Result<PaymentMethodVO> create(@RequestBody PaymentMethod entity) {
        return Result.success("新增成功", paymentMethodService.create(entity));
    }

    @Operation(summary = "修改支付方式")
    @OperationLog(module = "支付方式管理", type = "UPDATE", desc = "修改支付方式")
    @SaCheckPermission("md:payment-method:update")
    @PutMapping("/{id}")
    public Result<PaymentMethodVO> update(@PathVariable Long id, @RequestBody PaymentMethod entity) {
        return Result.success("修改成功", paymentMethodService.updateMethod(id, entity));
    }

    @Operation(summary = "启用/停用支付方式")
    @OperationLog(module = "支付方式管理", type = "UPDATE", desc = "更新支付方式状态")
    @SaCheckPermission("md:payment-method:status")
    @PutMapping("/{id}/status")
    public Result<PaymentMethodVO> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "状态(0-停用,1-启用)") @RequestBody PaymentMethodStatusDTO body) {
        return Result.success(paymentMethodService.updateStatus(id, body == null ? null : body.getStatus()));
    }

    @Operation(summary = "批量启用/停用支付方式")
    @OperationLog(module = "支付方式管理", type = "UPDATE", desc = "批量更新支付方式状态")
    @SaCheckPermission("md:payment-method:update")
    @PutMapping("/batch-status")
    public Result<Integer> batchStatus(@RequestBody PaymentMethodBatchDTO body) {
        return Result.success("操作成功", paymentMethodService.batchStatus(body.getIds(), body.getStatus()));
    }

    @Operation(summary = "删除支付方式（引用保护）")
    @OperationLog(module = "支付方式管理", type = "DELETE", desc = "删除支付方式")
    @SaCheckPermission("md:payment-method:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "导出支付方式（真实 xlsx）")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "导出支付方式")
    @SaCheckPermission("md:payment-method:export")
    @GetMapping("/export")
    public void export(PaymentMethodQuery query, HttpServletResponse response) throws IOException {
        List<PaymentMethodVO> rows = paymentMethodService.listByQuery(query);

        String fileName = "支付方式_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"编码", "名称", "类型", "默认入账账户", "手续费率", "默认", "排序", "状态", "备注", "创建时间"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("支付方式");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (PaymentMethodVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(nullSafe(vo.getMethodCode()));
                row.createCell(c++).setCellValue(nullSafe(vo.getMethodName()));
                row.createCell(c++).setCellValue(METHOD_TYPE_TEXT.getOrDefault(vo.getMethodType(), ""));
                row.createCell(c++).setCellValue(nullSafe(vo.getAccountName()));
                row.createCell(c++).setCellValue(feeRatePercent(vo.getFeeRate()));
                row.createCell(c++).setCellValue(vo.getIsDefault() != null && vo.getIsDefault() == 1 ? "是" : "否");
                row.createCell(c++).setCellValue(vo.getSort() == null ? 0 : vo.getSort());
                row.createCell(c++).setCellValue(vo.getStatus() != null && vo.getStatus() == 1 ? "启用" : "停用");
                row.createCell(c++).setCellValue(nullSafe(vo.getRemark()));
                row.createCell(c).setCellValue(vo.getCreateTime() == null ? "" : vo.getCreateTime().format(DATE_TIME));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    /** 导出用：小数费率 → 百分比文本（0.0060 → 0.60%） */
    private String feeRatePercent(BigDecimal feeRate) {
        if (feeRate == null) {
            return "0.00%";
        }
        return feeRate.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

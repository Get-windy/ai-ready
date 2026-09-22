package cn.aiedge.erp.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.PaymentChannelDTO;
import cn.aiedge.erp.finance.dto.PaymentChannelQuery;
import cn.aiedge.erp.finance.dto.PaymentChannelVO;
import cn.aiedge.erp.finance.service.PaymentChannelService;
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
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 支付渠道主数据Controller（资料 → 支付管理 → 支付渠道）
 *
 * 对应表 md_payment_channel（V11.29.0 建表，V11.33.0 补 remark）。
 * 红线：全系统唯一渠道主数据，收付款单资金路由统一引用，严禁另建重复渠道表。
 */
@Tag(name = "支付渠道管理", description = "支付渠道主数据增删改查、启停、导入导出")
@Slf4j
@RestController
@RequestMapping("/api/erp/md/payment-channel")
@RequiredArgsConstructor
public class MdPaymentChannelController {

    private static final String[] EXPORT_HEADERS =
            {"渠道编码", "渠道名称", "支付方式", "商户号", "排序", "状态", "备注", "创建时间"};

    private static final String[] TEMPLATE_HEADERS =
            {"导入结果", "渠道编码(必填)", "渠道名称(必填)", "支付方式(必填)", "商户号", "排序", "备注"};

    private static final String[] TEMPLATE_COMMENTS = {
            "由系统在导入后回写结果，请勿填写",
            "唯一编码，如 WECHAT_MP / ALIPAY_WEB；重号将被拒绝",
            "必填，如 微信-公众号支付",
            "填支付方式编码或名称（如 WECHAT / 微信支付），不存在则整行拒绝",
            "微信商户号 / 支付宝PID 等",
            "数字，越小越靠前，留空按 0",
            "备注信息"
    };

    private final PaymentChannelService paymentChannelService;

    @Operation(summary = "分页查询支付渠道")
    @OperationLog(module = "支付渠道管理", type = "QUERY", desc = "分页查询支付渠道")
    @SaCheckPermission("md:payment-channel:view")
    @GetMapping("/page")
    public Result<Page<PaymentChannelVO>> page(PaymentChannelQuery query) {
        return Result.success(paymentChannelService.page(query));
    }

    @Operation(summary = "查询支付渠道详情")
    @OperationLog(module = "支付渠道管理", type = "QUERY", desc = "查询支付渠道详情")
    @SaCheckPermission("md:payment-channel:view")
    @GetMapping("/{id}")
    public Result<PaymentChannelVO> getById(@PathVariable Long id) {
        return Result.success(paymentChannelService.detail(id));
    }

    @Operation(summary = "查询启用的支付渠道（收付款单资金路由下拉）")
    @SaCheckPermission("md:payment-channel:list")
    @GetMapping("/list")
    public Result<List<PaymentChannelVO>> list(
            @Parameter(description = "支付方式ID筛选") @RequestParam(required = false) Long methodId) {
        PaymentChannelQuery query = new PaymentChannelQuery();
        query.setMethodId(methodId);
        query.setStatus(1);
        return Result.success(paymentChannelService.list(query));
    }

    @Operation(summary = "新增支付渠道")
    @OperationLog(module = "支付渠道管理", type = "CREATE", desc = "新增支付渠道")
    @SaCheckPermission("md:payment-channel:edit")
    @PostMapping
    public Result<PaymentChannelVO> create(@RequestBody PaymentChannelDTO dto) {
        return Result.success("新增成功", paymentChannelService.create(dto));
    }

    @Operation(summary = "更新支付渠道")
    @OperationLog(module = "支付渠道管理", type = "UPDATE", desc = "更新支付渠道")
    @SaCheckPermission("md:payment-channel:edit")
    @PutMapping("/{id}")
    public Result<PaymentChannelVO> update(@PathVariable Long id, @RequestBody PaymentChannelDTO dto) {
        return Result.success("修改成功", paymentChannelService.update(id, dto));
    }

    @Operation(summary = "删除支付渠道")
    @OperationLog(module = "支付渠道管理", type = "DELETE", desc = "删除支付渠道")
    @SaCheckPermission("md:payment-channel:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paymentChannelService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用支付渠道")
    @OperationLog(module = "支付渠道管理", type = "UPDATE", desc = "更新支付渠道状态")
    @SaCheckPermission("md:payment-channel:edit")
    @PutMapping("/{id}/status")
    public Result<PaymentChannelVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(paymentChannelService.updateStatus(id, parseInt(body == null ? null : body.get("status"))));
    }

    @Operation(summary = "下载支付渠道导入模板（三步向导第 1 步）")
    @SaCheckPermission("md:payment-channel:view")
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "支付渠道导入模板_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        prepareXlsxResponse(response, fileName);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("支付渠道");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            XSSFDrawing drawing = ((XSSFSheet) sheet).createDrawingPatriarch();
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headStyle);
                XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, i, 0, i + 2, 3);
                XSSFComment comment = drawing.createCellComment(anchor);
                comment.setString(new XSSFRichTextString(TEMPLATE_COMMENTS[i]));
                comment.setAuthor("系统");
                cell.setCellComment(comment);
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    @Operation(summary = "Excel 导入支付渠道（真实落库）")
    @PostMapping("/import-excel")
    @OperationLog(module = "支付渠道管理", type = "CREATE", desc = "Excel 导入支付渠道")
    @SaCheckPermission("md:payment-channel:edit")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success("导入完成", paymentChannelService.importExcel(file));
    }

    @Operation(summary = "导出支付渠道（真实 xlsx）")
    @SaCheckPermission("md:payment-channel:export")
    @GetMapping("/export")
    @OperationLog(module = "支付渠道管理", type = "QUERY", desc = "导出支付渠道")
    public void export(PaymentChannelQuery query, HttpServletResponse response) throws IOException {
        List<PaymentChannelVO> rows = paymentChannelService.list(query);
        String fileName = "支付渠道_" + LocalDate.now() + ".xlsx";
        prepareXlsxResponse(response, fileName);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("支付渠道");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headStyle);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIdx = 1;
            for (PaymentChannelVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(vo.getChannelCode()),
                        nullSafe(vo.getChannelName()),
                        methodText(vo),
                        nullSafe(vo.getMerchantNo()),
                        vo.getSort() == null ? "0" : String.valueOf(vo.getSort()),
                        nullSafe(vo.getStatusText()),
                        nullSafe(vo.getRemark()),
                        vo.getCreateTime() == null ? "" : vo.getCreateTime().format(fmt)
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String methodText(PaymentChannelVO vo) {
        if (vo.getMethodCode() == null && vo.getMethodName() == null) {
            return "";
        }
        return "[" + nullSafe(vo.getMethodCode()) + "] " + nullSafe(vo.getMethodName());
    }

    private void prepareXlsxResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("状态取值非法，仅支持 1-启用 / 0-停用");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

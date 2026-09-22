package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import cn.aiedge.erp.sale.service.SalesDetailQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 销售明细查询控制器
 * 提供销售出库明细的综合查询接口（96列）与真实 Excel 导出
 */
@Slf4j
@RestController
@RequestMapping("/api/sales/detail-query")
@RequiredArgsConstructor
@Tag(name = "销售明细查询", description = "销售出库明细综合查询（96列）")
public class SalesDetailQueryController {

    /** 导出单次最多行数（防止超大结果集打爆内存） */
    private static final int EXPORT_MAX_ROWS = 50000;

    /** 导出列（与页面 96 数据列一一对应，顺序一致） */
    private static final String[][] EXPORT_COLUMNS = {
            {"docDate", "单据日期"}, {"docNo", "单据编号"}, {"docType", "单据类型"}, {"warehouseName", "仓库"},
            {"customerName", "客户"}, {"customerCode", "客户编号"}, {"customerLevel", "客户级别"},
            {"receiverName", "收货人"}, {"receiverPhone", "联系电话"}, {"shippingAddress", "收货地址"},
            {"custExtText1", "客户自定义字段1"}, {"custExtText2", "客户自定义字段2"}, {"custExtText3", "客户自定义字段3"},
            {"custExtText4", "客户自定义字段4"}, {"custExtText5", "客户自定义字段5"},
            {"logisticsCompany", "物流公司"}, {"trackingNumber", "运单号"}, {"region", "区域"},
            {"buyerRemark", "买家备注"}, {"customerRemark", "客户备注"}, {"sourceOrder", "来源订单"},
            {"sourceOrderDate", "来源订单日期"}, {"generationMethod", "产生方式"}, {"handlerName", "经手人"},
            {"departmentName", "部门"}, {"defaultHandlerName", "默认经手人"}, {"settlementStatus", "结算状态"},
            {"productName", "商品名称"}, {"productCode", "货号"}, {"barcode", "条码"},
            {"smallUnitBarcode", "小单位条码"}, {"specification", "规格"}, {"model", "型号"}, {"origin", "产地"},
            {"deliveryMethod", "配送方式"}, {"brand", "品牌"},
            {"itemExtNum1", "表体自定义1"}, {"itemExtNum2", "表体自定义2"}, {"itemExtNum3", "表体自定义3"},
            {"itemExtText1", "表体自定义4(文本)"}, {"itemExtText2", "表体自定义5(文本)"},
            {"itemExtNum6", "表体自定义6"}, {"itemExtNum7", "表体自定义7"},
            {"itemExtPartner", "表体自定义8(往来单位)"}, {"itemExtStaff", "表体自定义9(职员)"},
            {"itemExtDept", "表体自定义10(部门)"},
            {"salesQuantity", "销售数量"}, {"salesQuantityUnit", "销售数量单位"},
            {"commonUnitQuantity", "销售常用单位数量"}, {"commonUnit", "销售常用单位"},
            {"batchBarcode", "批次条码"}, {"productionDate", "生产日期"}, {"expiryDate", "到期日期"},
            {"bigPack", "大包装"}, {"midPack", "中包装"}, {"smallPack", "小包装"},
            {"smallUnit", "小单位"}, {"smallUnitQuantity", "小单位数量"},
            {"priceLevel1", "餐饮店"}, {"priceLevel2", "食堂团餐"}, {"priceLevel3", "自助vip"},
            {"priceLevel4", "大团餐"}, {"priceLevel5", "特价客户"}, {"priceLevel6", "外围餐饮店"},
            {"priceLevel7", "重点vip01"}, {"priceLevel8", "连锁vip"},
            {"unitPrice", "单价"}, {"smallUnitPrice", "小单位单价"}, {"amount", "金额"},
            {"discountRate", "折扣(%)"}, {"discountedPrice", "折后单价"}, {"discountedAmount", "折后金额"},
            {"favorableDiscountRate", "优惠折扣"}, {"favorableUnitPrice", "优惠后单价"},
            {"favorableAmount", "优惠后金额"}, {"salesRevenue", "销售收入"},
            {"costPrice", "成本单价"}, {"costAmount", "成本金额"}, {"grossProfit", "毛利"},
            {"grossProfitRate", "毛利率(%)"},
            {"wholesalePrice", "批发价"}, {"retailPrice", "零售价"}, {"minSalePrice", "最低售价"},
            {"weight", "重量(kg)"}, {"volume", "体积(m³)"},
            {"salesType", "销售类型"}, {"productAttribute", "商品行属性"}, {"itemRemark", "明细备注"},
            {"remark", "单据备注"}, {"bookkeeperName", "记账人"}, {"creatorName", "制单人"},
            {"bookkeepingTime", "记账时间"}, {"createTime", "制单时间"}, {"settlementCompleteTime", "结算完成时间"},
            {"printCount", "打印次数"}, {"deliveryDriver", "配送司机"},
    };

    private final SalesDetailQueryService salesDetailQueryService;

    @SaCheckPermission("sale:detail-query:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询销售明细（96列）")
    public Page<Map<String, Object>> page(SalesDetailQueryDTO queryDTO) {
        if (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) {
            queryDTO.setCurrent(1L);
        }
        if (queryDTO.getSize() == null || queryDTO.getSize() <= 0) {
            queryDTO.setSize(20L);
        }
        return salesDetailQueryService.pageDetail(queryDTO);
    }

    @SaCheckPermission("sale:detail-query:export")
    @GetMapping("/export")
    @Operation(summary = "导出销售明细（真实 Excel 流，96 列，与查询同一过滤口径）")
    public void export(SalesDetailQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = salesDetailQueryService.listDetail(queryDTO);
        if (rows.size() > EXPORT_MAX_ROWS) {
            log.warn("销售明细导出结果超上限，已截断: total={}, max={}", rows.size(), EXPORT_MAX_ROWS);
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "销售明细查询_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("销售明细查询");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_COLUMNS[i][1]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                    writeCell(excelRow.createCell(i), row.get(EXPORT_COLUMNS[i][0]));
                }
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool ? "是" : "否");
        } else {
            cell.setCellValue(value.toString());
        }
    }
}

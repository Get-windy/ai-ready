package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.dto.PurchaseDetailListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseDetailQueryDTO;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.service.PurchaseDetailQueryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 按明细Tab查询服务实现
 * 通过自定义SQL JOIN实现59列数据返回+名称解析
 * 以明细行为主表，每条记录是一个明细行
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PurchaseDetailQueryServiceImpl implements PurchaseDetailQueryService {

    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public Page<PurchaseDetailListDTO> pageByDetail(PurchaseDetailQueryDTO query) {
        Page<PurchaseDetailListDTO> page = new Page<>(query.getCurrent(), query.getSize());

        // 使用 QueryWrapper + 带表别名的列名
        // SQL里各表别名: i=明细, o=主表, ps=供应商快照, ei=扩展信息,
        // w=仓库, u_purchaser=经手人, d=部门, u_creator=制单人
        QueryWrapper<cn.aiedge.erp.purchase.entity.PurchaseOrder> wrapper = new QueryWrapper<>();

        // === 单据级过滤条件 ===
        // 日期范围（结束日期必须转为 LocalDateTime，否则 JDBC 参数化为 varchar 导致类型不匹配）
        wrapper.ge(query.getDateStart() != null, "o.order_date", parseDateStart(query.getDateStart()))
               .le(query.getDateEnd() != null, "o.order_date", parseDateEnd(query.getDateEnd()));

        // 单据编号
        wrapper.like(query.getOrderNo() != null, "o.order_no", query.getOrderNo());

        // 来源订单(源单编号)
        wrapper.like(query.getSourceBillNo() != null, "o.source_bill_no", query.getSourceBillNo());

        // 单据状态
        wrapper.eq(query.getStatus() != null, "o.status", query.getStatus());

        // 单据备注
        if (query.getRemark() != null) {
            wrapper.like("o.remark", query.getRemark());
        }

        // 供应商名称
        if (query.getSupplierName() != null) {
            wrapper.like("ps.supplier_name", query.getSupplierName());
        }

        // 经手人姓名
        if (query.getPurchaserName() != null) {
            wrapper.like("u_purchaser.nickname", query.getPurchaserName());
        }

        // 部门名称
        if (query.getDeptName() != null) {
            wrapper.like("d.dept_name", query.getDeptName());
        }

        // 制单人
        if (query.getCreateByName() != null) {
            wrapper.like("u_creator.nickname", query.getCreateByName());
        }

        // 仓库名称
        if (query.getWarehouseName() != null) {
            wrapper.like("w.warehouse_name", query.getWarehouseName());
        }

        // 审核人（通过子查询匹配审核流水表）
        if (query.getAuditorName() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_purchase_order_audit_trail at_filter " +
                          "WHERE at_filter.order_id = o.id AND at_filter.action = 'approve' " +
                          "AND at_filter.operator_name LIKE {0})",
                          "%" + query.getAuditorName() + "%");
        }

        // 单价状态（0=未定价，1=已定价）
        if (query.getPriceStatus() != null) {
            if (query.getPriceStatus() == 0) {
                wrapper.isNull("i.unit_price");
            } else {
                wrapper.isNotNull("i.unit_price");
            }
        }

        // === 明细级过滤条件 ===
        // 商品名称
        wrapper.like(query.getProductName() != null, "i.product_name", query.getProductName());

        // 明细备注
        wrapper.like(query.getItemRemark() != null, "i.remark", query.getItemRemark());

        // 是否赠品
        if (query.getIsGift() != null) {
            wrapper.eq("i.gift", query.getIsGift() == 1);
        }

        return purchaseOrderMapper.selectDetailListWithNames(page, wrapper);
    }

    private LocalDateTime parseDateStart(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try { return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
        catch (Exception e) { return null; }
    }
    private LocalDateTime parseDateEnd(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try { return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59); }
        catch (Exception e) { return null; }
    }
}

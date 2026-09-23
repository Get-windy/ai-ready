package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.dto.PurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.service.PurchaseDocQueryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 按单据Tab查询服务实现
 * 通过自定义SQL JOIN实现39列数据返回+名称解析+聚合计算
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PurchaseDocQueryServiceImpl implements PurchaseDocQueryService {

    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public Page<PurchaseOrderListDTO> pageByDoc(PurchaseDocQueryDTO query) {
        Page<PurchaseOrderListDTO> page = new Page<>(query.getCurrent(), query.getSize());

        // 使用 QueryWrapper + 列名字符串（不带表别名，因为 selectDocListWithNames SQL 里列名无前缀）
        // 注意：PurchaseOrder 实体字段映射的 DB 列名与 o. 前缀列不冲突（INNER JOIN 的表没有同名列冲突）
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();

        // ⚠️ 必须显式写逻辑删除条件：`@TableLogic` 只对 BaseMapper 生成的 SQL 生效，
        // 本查询是自定义 SQL + ${ew.customSqlSegment}，MyBatis-Plus 不会自动补 `deleted = 0`
        // → 不加这行会把已逻辑删除的订单一并列出（devdb 实测有 1 张已删单会被带出）。
        wrapper.eq("o.deleted", 0);

        // 日期范围
        wrapper.ge(query.getDateStart() != null, "o.order_date", parseDateStart(query.getDateStart()))
               .le(query.getDateEnd() != null, "o.order_date", parseDateEnd(query.getDateEnd()));

        // 单据编号
        wrapper.like(query.getOrderNo() != null, "o.order_no", query.getOrderNo());

        // 单据状态
        wrapper.eq(query.getStatus() != null, "o.status", query.getStatus());

        // 供应商名称（通过供应商快照表模糊匹配）
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

        // 单据备注
        if (query.getRemark() != null) {
            wrapper.like("o.remark", query.getRemark());
        }

        // 提交人
        if (query.getSubmitterName() != null) {
            wrapper.like("u_submitter.nickname", query.getSubmitterName());
        }

        // 审核人（通过子查询匹配审核流水表）
        if (query.getAuditorName() != null) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_purchase_order_audit_trail at_filter " +
                          "WHERE at_filter.order_id = o.id AND at_filter.action = 'approve' " +
                          "AND at_filter.operator_name LIKE {0})",
                          "%" + query.getAuditorName() + "%");
        }

        // 自定义字段
        wrapper.ge(query.getExtNum1Start() != null, "ei.ext_num_1", query.getExtNum1Start())
               .le(query.getExtNum1End() != null, "ei.ext_num_1", query.getExtNum1End());
        wrapper.ge(query.getExtNum2Start() != null, "ei.ext_num_2", query.getExtNum2Start())
               .le(query.getExtNum2End() != null, "ei.ext_num_2", query.getExtNum2End());
        wrapper.like(query.getExtText1() != null, "ei.ext_text_1", query.getExtText1());
        wrapper.like(query.getExtText2() != null, "ei.ext_text_2", query.getExtText2());
        wrapper.like(query.getExtText3() != null, "ei.ext_text_3", query.getExtText3());

        // 打印次数
        wrapper.ge(query.getPrintCountStart() != null, "ei.print_count", query.getPrintCountStart())
               .le(query.getPrintCountEnd() != null, "ei.print_count", query.getPrintCountEnd());

        // 排序：按创建时间倒序
        wrapper.orderByDesc("o.create_time");

        return purchaseOrderMapper.selectDocListWithNames(page, wrapper);
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

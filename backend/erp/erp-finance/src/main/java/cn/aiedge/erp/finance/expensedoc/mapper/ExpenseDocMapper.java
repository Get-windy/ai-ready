package cn.aiedge.erp.finance.expensedoc.mapper;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocItemVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 费用单主表 Mapper
 */
@Mapper
public interface ExpenseDocMapper extends BaseMapper<ExpenseDoc> {

    /**
     * 按明细多条件分页查询（联查主表头字段）
     */
    @Select("<script>" +
            "SELECT i.id, i.expense_doc_id, i.line_no, " +
            "       h.doc_date, h.doc_no, h.status, h.expense_type, " +
            "       h.approval_status, h.approval_level, h.current_approver_name, " +
            "       h.partner_id, h.partner_code, h.partner_name, " +
            "       h.handler_name, h.dept_name, h.creator_name, " +
            "       NULL AS auditor_name, h.bookkeeper_name, " +
            "       h.bookkeeping_time, h.summary, h.remark AS remark, h.create_time, h.update_time, h.print_count, " +
            "       h.total_amount, h.pay_amount, h.pay_account_name, " +
            "       i.expense_code, i.expense_name, i.subject_code, i.subject_name, i.amount, i.remark AS itemRemark " +
            "FROM erp_expense_item i " +
            "LEFT JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " +
            "<if test='q.docNo != null and q.docNo != \"\"'> AND h.doc_no LIKE CONCAT('%', #{q.docNo}, '%') </if>" +
            "<if test='q.partnerName != null and q.partnerName != \"\"'> AND h.partner_name LIKE CONCAT('%', #{q.partnerName}, '%') </if>" +
            "<if test='q.handlerName != null and q.handlerName != \"\"'> AND h.handler_name LIKE CONCAT('%', #{q.handlerName}, '%') </if>" +
            "<if test='q.deptName != null and q.deptName != \"\"'> AND h.dept_name LIKE CONCAT('%', #{q.deptName}, '%') </if>" +
            "<if test='q.creatorName != null and q.creatorName != \"\"'> AND h.creator_name LIKE CONCAT('%', #{q.creatorName}, '%') </if>" +
            "<if test='q.bookkeeperName != null and q.bookkeeperName != \"\"'> AND h.bookkeeper_name LIKE CONCAT('%', #{q.bookkeeperName}, '%') </if>" +
            "<if test='q.payAccountName != null and q.payAccountName != \"\"'> AND h.pay_account_name LIKE CONCAT('%', #{q.payAccountName}, '%') </if>" +
            "<if test='q.expenseName != null and q.expenseName != \"\"'> AND i.expense_name LIKE CONCAT('%', #{q.expenseName}, '%') </if>" +
            "<if test='q.subjectCode != null and q.subjectCode != \"\"'> AND i.subject_code LIKE CONCAT('%', #{q.subjectCode}, '%') </if>" +
            "<if test='q.remark != null and q.remark != \"\"'> AND h.remark LIKE CONCAT('%', #{q.remark}, '%') </if>" +
            "<if test='q.summary != null and q.summary != \"\"'> AND h.summary LIKE CONCAT('%', #{q.summary}, '%') </if>" +
            "<if test='q.itemRemark != null and q.itemRemark != \"\"'> AND i.remark LIKE CONCAT('%', #{q.itemRemark}, '%') </if>" +
            "<if test='q.status != null'> AND h.status = #{q.status} </if>" +
            "<if test='q.expenseType != null'> AND h.expense_type = #{q.expenseType} </if>" +
            "<if test='q.approvalStatus != null'> AND h.approval_status = #{q.approvalStatus} </if>" +
            "<if test='q.dateStart != null'> AND h.doc_date &gt;= #{q.dateStart} </if>" +
            "<if test='q.dateEnd != null'> AND h.doc_date &lt;= #{q.dateEnd} </if>" +
            "<if test='q.showRed != null and q.showRed == false'> AND (h.red_flag IS NULL OR h.red_flag = 0) </if>" +
            "ORDER BY h.doc_date DESC, h.doc_no DESC, i.line_no ASC" +
            "</script>")
    Page<ExpenseDocItemVO> pageDetail(Page<ExpenseDocItemVO> page, @Param("q") ExpenseDocQuery q);
}

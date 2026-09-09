package cn.aiedge.erp.finance.cashtransfer.mapper;

import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferItemVO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferQuery;
import cn.aiedge.erp.finance.cashtransfer.entity.CashTransfer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 提存主表 Mapper
 */
@Mapper
public interface CashTransferMapper extends BaseMapper<CashTransfer> {

    /**
     * 按明细多条件分页查询（联查主表头字段）
     */
    @Select("<script>" +
            "SELECT i.id, i.transfer_id, i.line_no, " +
            "       h.doc_date, h.doc_no, h.status, " +
            "       h.from_account_id, h.from_account_name, h.from_amount, h.to_amount, h.fee, " +
            "       h.handler_name, h.dept_name, h.creator_name, h.bookkeeper_name, " +
            "       h.bookkeeping_time, h.summary, h.remark AS remark, h.create_time, h.print_count, " +
            "       i.to_account_id, i.to_account_no, i.to_account_name, i.to_account_type, " +
            "       i.to_subject_code, i.amount, i.remark AS itemRemark " +
            "FROM erp_cash_transfer_item i " +
            "LEFT JOIN erp_cash_transfer h ON h.id = i.transfer_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " +
            "<if test='q.docNo != null and q.docNo != \"\"'> AND h.doc_no LIKE CONCAT('%', #{q.docNo}, '%') </if>" +
            "<if test='q.handlerName != null and q.handlerName != \"\"'> AND h.handler_name LIKE CONCAT('%', #{q.handlerName}, '%') </if>" +
            "<if test='q.deptName != null and q.deptName != \"\"'> AND h.dept_name LIKE CONCAT('%', #{q.deptName}, '%') </if>" +
            "<if test='q.creatorName != null and q.creatorName != \"\"'> AND h.creator_name LIKE CONCAT('%', #{q.creatorName}, '%') </if>" +
            "<if test='q.bookkeeperName != null and q.bookkeeperName != \"\"'> AND h.bookkeeper_name LIKE CONCAT('%', #{q.bookkeeperName}, '%') </if>" +
            "<if test='q.fromAccountName != null and q.fromAccountName != \"\"'> AND h.from_account_name LIKE CONCAT('%', #{q.fromAccountName}, '%') </if>" +
            "<if test='q.toAccountName != null and q.toAccountName != \"\"'> AND i.to_account_name LIKE CONCAT('%', #{q.toAccountName}, '%') </if>" +
            "<if test='q.remark != null and q.remark != \"\"'> AND h.remark LIKE CONCAT('%', #{q.remark}, '%') </if>" +
            "<if test='q.itemRemark != null and q.itemRemark != \"\"'> AND i.remark LIKE CONCAT('%', #{q.itemRemark}, '%') </if>" +
            "<if test='q.status != null'> AND h.status = #{q.status} </if>" +
            "<if test='q.dateStart != null'> AND h.doc_date &gt;= #{q.dateStart} </if>" +
            "<if test='q.dateEnd != null'> AND h.doc_date &lt;= #{q.dateEnd} </if>" +
            "<if test='q.showRed != null and q.showRed == false'> AND (h.red_flag IS NULL OR h.red_flag = 0) </if>" +
            "ORDER BY h.doc_date DESC, h.doc_no DESC" +
            "</script>")
    Page<CashTransferItemVO> pageDetail(Page<CashTransferItemVO> page, @Param("q") CashTransferQuery q);
}

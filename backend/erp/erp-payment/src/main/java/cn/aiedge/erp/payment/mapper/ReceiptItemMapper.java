package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.ReceiptItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReceiptItemMapper extends BaseMapper<ReceiptItem> {

    @Select("SELECT * FROM erp_receipt_item WHERE receipt_id = #{receiptId} AND deleted = 0 ORDER BY line_no ASC")
    List<ReceiptItem> selectByReceiptId(@Param("receiptId") Long receiptId);

    @Select("SELECT SUM(verify_amount) FROM erp_receipt_item WHERE receipt_id = #{receiptId} AND deleted = 0")
    java.math.BigDecimal sumVerifyAmountByReceiptId(@Param("receiptId") Long receiptId);

    @Select("SELECT SUM(verified_amount) FROM erp_receipt_item WHERE receipt_id = #{receiptId} AND deleted = 0")
    java.math.BigDecimal sumVerifiedAmountByReceiptId(@Param("receiptId") Long receiptId);
}
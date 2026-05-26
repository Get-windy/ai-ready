package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.VoucherEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VoucherEntryMapper extends BaseMapper<VoucherEntry> {
    
    @Select("SELECT * FROM finance_voucher_entry WHERE voucher_id = #{voucherId} AND deleted = 0 ORDER BY entry_no")
    List<VoucherEntry> listByVoucherId(@Param("voucherId") Long voucherId);
    
    @Select("SELECT COUNT(*) FROM finance_voucher_entry WHERE voucher_id = #{voucherId} AND deleted = 0")
    Integer countByVoucherId(@Param("voucherId") Long voucherId);
    
    @Select("SELECT SUM(debit_amount) FROM finance_voucher_entry WHERE voucher_id = #{voucherId} AND deleted = 0")
    java.math.BigDecimal sumDebitByVoucherId(@Param("voucherId") Long voucherId);
    
    @Select("SELECT SUM(credit_amount) FROM finance_voucher_entry WHERE voucher_id = #{voucherId} AND deleted = 0")
    java.math.BigDecimal sumCreditByVoucherId(@Param("voucherId") Long voucherId);
}
package com.aiready.finance.mapper;

import com.aiready.finance.entity.Voucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 会计凭证Mapper接口
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {
    
    /**
     * 根据凭证字号查询凭证
     */
    @Select("SELECT * FROM fin_voucher WHERE voucher_no = #{voucherNo} AND deleted = 0")
    Voucher selectByVoucherNo(@Param("voucherNo") String voucherNo);
    
    /**
     * 生成凭证字号
     */
    @Select("SELECT CONCAT('记-', LPAD(COALESCE(MAX(CAST(SUBSTRING(voucher_no FROM 3) AS INTEGER)), 0) + 1, 4, '0')) " +
            "FROM fin_voucher WHERE voucher_date = #{voucherDate} AND deleted = 0")
    String generateVoucherNo(@Param("voucherDate") LocalDate voucherDate);
    
    /**
     * 更新凭证状态
     */
    @Update("UPDATE fin_voucher SET status = #{status}, update_by = #{operatorId}, update_time = NOW() " +
            "WHERE id = #{voucherId} AND deleted = 0")
    int updateStatus(@Param("voucherId") Long voucherId, @Param("status") Integer status, @Param("operatorId") Long operatorId);
    
    /**
     * 审核凭证
     */
    @Update("UPDATE fin_voucher SET status = 2, reviewer_id = #{operatorId}, review_time = NOW(), " +
            "update_by = #{operatorId}, update_time = NOW() WHERE id = #{voucherId} AND deleted = 0")
    int reviewVoucher(@Param("voucherId") Long voucherId, @Param("operatorId") Long operatorId);
    
    /**
     * 记账
     */
    @Update("UPDATE fin_voucher SET status = 3, bookkeeper_id = #{operatorId}, bookkeeping_time = NOW(), " +
            "update_by = #{operatorId}, update_time = NOW() WHERE id = #{voucherId} AND deleted = 0")
    int bookkeeping(@Param("voucherId") Long voucherId, @Param("operatorId") Long operatorId);
    
    /**
     * 作废凭证
     */
    @Update("UPDATE fin_voucher SET status = 4, update_by = #{operatorId}, update_time = NOW() " +
            "WHERE id = #{voucherId} AND deleted = 0")
    int cancelVoucher(@Param("voucherId") Long voucherId, @Param("operatorId") Long operatorId);
}

package com.aiready.finance.mapper;

import com.aiready.finance.entity.Ledger;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账簿Mapper接口
 */
@Mapper
public interface LedgerMapper extends BaseMapper<Ledger> {
    
    /**
     * 根据科目ID和日期范围查询明细账
     */
    List<Ledger> selectDetailLedger(@Param("subjectId") Long subjectId, 
                                    @Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate);
    
    /**
     * 查询总账
     */
    List<Ledger> selectGeneralLedger(@Param("accountingPeriod") String accountingPeriod);
    
    /**
     * 获取科目期初余额
     */
    @Select("SELECT COALESCE(opening_balance, 0) FROM fin_account_subject WHERE id = #{subjectId} AND deleted = 0")
    BigDecimal getOpeningBalance(@Param("subjectId") Long subjectId);
    
    /**
     * 获取科目在日期之前的累计发生额
     */
    @Select("SELECT COALESCE(SUM(debit_amount), 0) - COALESCE(SUM(credit_amount), 0) " +
            "FROM fin_ledger WHERE subject_id = #{subjectId} AND business_date < #{date} AND deleted = 0")
    BigDecimal getAccumulatedAmountBeforeDate(@Param("subjectId") Long subjectId, @Param("date") LocalDate date);
    
    /**
     * 批量插入账簿记录
     */
    int batchInsert(@Param("records") List<Ledger> records);
    
    /**
     * 根据凭证ID删除账簿记录
     */
    @Select("DELETE FROM fin_ledger WHERE voucher_id = #{voucherId}")
    int deleteByVoucherId(@Param("voucherId") Long voucherId);
}

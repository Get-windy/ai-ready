package com.aiready.finance.mapper;

import com.aiready.finance.entity.VoucherItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 凭证分录Mapper接口
 */
@Mapper
public interface VoucherItemMapper extends BaseMapper<VoucherItem> {
    
    /**
     * 根据凭证ID查询分录列表
     */
    @Select("SELECT * FROM fin_voucher_item WHERE voucher_id = #{voucherId} AND deleted = 0 ORDER BY item_no")
    List<VoucherItem> selectByVoucherId(@Param("voucherId") Long voucherId);
    
    /**
     * 批量插入分录
     */
    int batchInsert(@Param("items") List<VoucherItem> items);
    
    /**
     * 根据凭证ID删除分录
     */
    @Select("DELETE FROM fin_voucher_item WHERE voucher_id = #{voucherId}")
    int deleteByVoucherId(@Param("voucherId") Long voucherId);
}

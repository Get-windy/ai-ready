package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Voucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {
    
    @Select("SELECT * FROM finance_voucher WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} ORDER BY voucher_no")
    List<Voucher> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_voucher WHERE tenant_id = #{tenantId} AND deleted = 0 AND voucher_no = #{voucherNo}")
    Voucher getByVoucherNo(@Param("tenantId") Long tenantId, @Param("voucherNo") String voucherNo);
    
    @Select("SELECT MAX(word_no) FROM finance_voucher WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND word = #{word}")
    Integer getMaxWordNo(@Param("tenantId") Long tenantId, @Param("period") String period, @Param("word") String word);
    
    @Select("SELECT COUNT(*) FROM finance_voucher WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND status IN (0, 1)")
    Integer countUnpostedByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_voucher WHERE tenant_id = #{tenantId} AND deleted = 0 AND source_type = #{sourceType} AND source_id = #{sourceId}")
    Voucher getBySource(@Param("tenantId") Long tenantId, @Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);
    
    @Update("UPDATE finance_voucher SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
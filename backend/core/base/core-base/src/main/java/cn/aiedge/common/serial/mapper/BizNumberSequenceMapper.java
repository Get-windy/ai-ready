package cn.aiedge.common.serial.mapper;

import cn.aiedge.common.serial.BizNumberSequence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BizNumberSequenceMapper extends BaseMapper<BizNumberSequence> {

    /**
     * 原子递增序列号（SELECT FOR UPDATE + UPDATE）
     * 如果日期已变化，先重置为0再递增
     */
    @Select("SELECT id, biz_type, locale, current_seq, seq_date, prefix, seq_length, max_seq, update_time FROM biz_number_sequence WHERE biz_type = #{bizType} AND locale = #{locale} AND tenant_id = #{tenantId} FOR UPDATE")
    BizNumberSequence selectForUpdateWithLocale(@Param("bizType") String bizType, @Param("locale") String locale, @Param("tenantId") Long tenantId);

    /**
     * 原子递增序列号（针对中文环境，使用默认zh_CN）
     */
    @Select("SELECT id, biz_type, locale, current_seq, seq_date, prefix, seq_length, max_seq, update_time FROM biz_number_sequence WHERE biz_type = #{bizType} AND locale = 'zh_CN' AND tenant_id = #{tenantId} FOR UPDATE")
    BizNumberSequence selectForUpdate(@Param("bizType") String bizType, @Param("tenantId") Long tenantId);

    @Update("UPDATE biz_number_sequence SET current_seq = current_seq + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int incrementSeq(@Param("id") Long id);

    @Update("UPDATE biz_number_sequence SET current_seq = 1, seq_date = #{today}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int resetAndIncrement(@Param("id") Long id, @Param("today") String today);
}

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
    @Select("SELECT id, current_seq FROM biz_number_sequence WHERE biz_type = #{bizType} FOR UPDATE")
    BizNumberSequence selectForUpdate(@Param("bizType") String bizType);

    @Update("UPDATE biz_number_sequence SET current_seq = current_seq + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int incrementSeq(@Param("id") Long id);

    @Update("UPDATE biz_number_sequence SET current_seq = 1, seq_date = #{today}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int resetAndIncrement(@Param("id") Long id, @Param("today") String today);
}

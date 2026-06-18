package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.OffsetItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OffsetItemMapper extends BaseMapper<OffsetItem> {

    @Select("SELECT * FROM erp_offset_item WHERE offset_id = #{offsetId} AND deleted = 0 ORDER BY line_no")
    List<OffsetItem> selectByOffsetId(@Param("offsetId") Long offsetId);
}

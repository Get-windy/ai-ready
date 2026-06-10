package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.Offset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OffsetMapper extends BaseMapper<Offset> {

    @Select("SELECT * FROM erp_offset WHERE party_type = #{partyType} AND party_id = #{partyId} AND deleted = 0 ORDER BY create_time DESC")
    List<Offset> selectByParty(@Param("partyType") String partyType, @Param("partyId") Long partyId);

    @Select("SELECT * FROM erp_offset WHERE status = #{status} AND deleted = 0")
    List<Offset> selectByStatus(@Param("status") String status);
}

package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.CapitalFlow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CapitalFlowMapper extends BaseMapper<CapitalFlow> {

    @Select("SELECT * FROM erp_capital_flow WHERE flow_type = #{flowType} AND deleted = 0 ORDER BY occur_date DESC")
    List<CapitalFlow> selectByFlowType(@Param("flowType") String flowType);

    @Select("SELECT * FROM erp_capital_flow WHERE party_type = #{partyType} AND party_id = #{partyId} AND deleted = 0 ORDER BY occur_date DESC")
    List<CapitalFlow> selectByParty(@Param("partyType") String partyType, @Param("partyId") Long partyId);

    @Select("SELECT * FROM erp_capital_flow WHERE ref_type = #{refType} AND ref_id = #{refId} AND deleted = 0")
    List<CapitalFlow> selectByRef(@Param("refType") String refType, @Param("refId") Long refId);
}

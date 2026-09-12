package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.PartyAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PartyAttachmentMapper extends BaseMapper<PartyAttachment> {

    /**
     * 按往来单位批量统计附件数量（列表「附件」列，避免逐行 N+1）
     */
    @Select("<script>" +
            "SELECT partner_id AS \"partnerId\", COUNT(*) AS \"cnt\" FROM erp_partner_attachment " +
            "WHERE deleted = 0 AND partner_id IN " +
            "<foreach collection='partyIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY partner_id" +
            "</script>")
    List<Map<String, Object>> countByPartyIds(@Param("partyIds") List<Long> partyIds);
}

package cn.aiedge.wms.borrow.mapper;

import cn.aiedge.wms.entity.WmsBorrowOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WmsBorrowOrderMapper extends BaseMapper<WmsBorrowOrder> {

    /** 按往来单位ID集合批量取主数据（客户级别/客户备注） */
    @Select("<script>SELECT id, party_level AS partyLevel, remark AS customerRemark FROM biz_party " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Map<String, Object>> selectPartiesByIds(@Param("ids") List<Long> ids);

    /** 按往来单位ID集合批量取主要联系人（biz_party_contact，优先主联系人） */
    @Select("<script>SELECT party_id AS partyId, contact_name AS contact FROM biz_party_contact " +
            "WHERE party_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY is_primary DESC, id ASC</script>")
    List<Map<String, Object>> selectContactsByIds(@Param("ids") List<Long> ids);
}

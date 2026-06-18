package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.Party;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PartyMapper extends BaseMapper<Party> {

    @Select("SELECT COUNT(*) FROM biz_party_transaction WHERE party_id = #{partyId} AND deleted = 0")
    Long hasTransactions(@Param("partyId") Long partyId);
}

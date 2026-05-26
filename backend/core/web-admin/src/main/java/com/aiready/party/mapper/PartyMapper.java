package com.aiready.party.mapper;

import com.aiready.party.entity.Party;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 往来单位Mapper接口
 */
@Mapper
public interface PartyMapper extends BaseMapper<Party> {

    /**
     * 检查往来单位是否有交易记录
     */
    @Select("SELECT COUNT(*) FROM biz_party_transaction WHERE party_id = #{partyId} AND deleted = 0")
    Long hasTransactions(@Param("partyId") Long partyId);
}
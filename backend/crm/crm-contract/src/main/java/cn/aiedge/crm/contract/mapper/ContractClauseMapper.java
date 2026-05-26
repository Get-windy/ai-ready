package cn.aiedge.crm.contract.mapper;

import cn.aiedge.crm.contract.entity.ContractClause;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractClauseMapper extends BaseMapper<ContractClause> {

    @Select("SELECT * FROM crm_contract_clause WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY sort_order ASC")
    List<ContractClause> selectByContractId(@Param("contractId") Long contractId);
}
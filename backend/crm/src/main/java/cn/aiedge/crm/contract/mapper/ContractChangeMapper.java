package cn.aiedge.crm.contract.mapper;

import cn.aiedge.crm.contract.entity.ContractChange;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractChangeMapper extends BaseMapper<ContractChange> {

    @Select("SELECT * FROM crm_contract_change WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY create_time DESC")
    List<ContractChange> selectByContractId(@Param("contractId") Long contractId);

    @Select("SELECT * FROM crm_contract_change WHERE contract_id = #{contractId} AND status = #{status} AND deleted = 0")
    List<ContractChange> selectByContractIdAndStatus(@Param("contractId") Long contractId, @Param("status") Integer status);
}
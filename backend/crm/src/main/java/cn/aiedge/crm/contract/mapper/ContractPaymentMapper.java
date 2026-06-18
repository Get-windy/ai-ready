package cn.aiedge.crm.contract.mapper;

import cn.aiedge.crm.contract.entity.ContractPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractPaymentMapper extends BaseMapper<ContractPayment> {

    @Select("SELECT * FROM crm_contract_payment WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY payment_no ASC")
    List<ContractPayment> selectByContractId(@Param("contractId") Long contractId);

    @Select("SELECT SUM(plan_amount) FROM crm_contract_payment WHERE contract_id = #{contractId} AND deleted = 0")
    java.math.BigDecimal sumPlanAmountByContractId(@Param("contractId") Long contractId);

    @Select("SELECT SUM(actual_amount) FROM crm_contract_payment WHERE contract_id = #{contractId} AND status = 2 AND deleted = 0")
    java.math.BigDecimal sumActualAmountByContractId(@Param("contractId") Long contractId);

    @Select("SELECT * FROM crm_contract_payment WHERE contract_id = #{contractId} AND status = 0 AND plan_date <= #{date} AND deleted = 0")
    List<ContractPayment> selectDuePayments(@Param("contractId") Long contractId, @Param("date") java.time.LocalDateTime date);
}
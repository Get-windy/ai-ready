package cn.aiedge.crm.contract.mapper;

import cn.aiedge.crm.contract.entity.ContractAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractAttachmentMapper extends BaseMapper<ContractAttachment> {

    @Select("SELECT * FROM crm_contract_attachment WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY create_time DESC")
    List<ContractAttachment> selectByContractId(@Param("contractId") Long contractId);

    @Select("SELECT * FROM crm_contract_attachment WHERE contract_id = #{contractId} AND attachment_type = #{attachmentType} AND deleted = 0")
    List<ContractAttachment> selectByContractIdAndType(@Param("contractId") Long contractId, @Param("attachmentType") Integer attachmentType);
}
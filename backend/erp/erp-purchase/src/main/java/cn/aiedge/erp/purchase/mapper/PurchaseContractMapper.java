package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购合同Mapper接口
 */
@Mapper
public interface PurchaseContractMapper {

    @Select("SELECT * FROM purchase_contract WHERE id = #{id}")
    PurchaseContract findById(Long id);

    @Select("SELECT * FROM purchase_contract WHERE contract_no = #{contractNo}")
    PurchaseContract findByContractNo(String contractNo);

    @Select("SELECT * FROM purchase_contract WHERE supplier_id = #{supplierId} ORDER BY created_at DESC")
    List<PurchaseContract> findBySupplierId(Long supplierId);

    @Select("SELECT * FROM purchase_contract WHERE contract_status = #{status} ORDER BY created_at DESC")
    List<PurchaseContract> findByStatus(ContractStatus status);

    @Select("SELECT * FROM purchase_contract WHERE inquiry_id = #{inquiryId} ORDER BY created_at DESC")
    List<PurchaseContract> findByInquiryId(Long inquiryId);

    @Select("SELECT * FROM purchase_contract WHERE end_date <= #{date} AND contract_status = 'ACTIVE'")
    List<PurchaseContract> findExpiringContracts(int days);

    @Insert("INSERT INTO purchase_contract (contract_no, inquiry_id, quote_id, supplier_id, " +
            "supplier_name, contract_title, contract_type, contract_status, total_amount, " +
            "start_date, end_date, payment_terms, delivery_terms, quality_standard, " +
            "warranty_period, created_by, created_at) VALUES " +
            "(#{contractNo}, #{inquiryId}, #{quoteId}, #{supplierId}, #{supplierName}, " +
            "#{contractTitle}, #{contractType}, #{contractStatus}, #{totalAmount}, " +
            "#{startDate}, #{endDate}, #{paymentTerms}, #{deliveryTerms}, #{qualityStandard}, " +
            "#{warrantyPeriod}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseContract contract);

    @Update("UPDATE purchase_contract SET contract_title=#{contractTitle}, contract_type=#{contractType}, " +
            "total_amount=#{totalAmount}, start_date=#{startDate}, end_date=#{endDate}, " +
            "payment_terms=#{paymentTerms}, delivery_terms=#{deliveryTerms}, remark=#{remark} WHERE id=#{id}")
    int update(PurchaseContract contract);

    @Update("UPDATE purchase_contract SET contract_status=#{status}, updated_at=#{updateTime} WHERE id=#{id}")
    int updateStatus(Long id, String status, LocalDateTime updateTime);

    @Update("UPDATE purchase_contract SET approver_id=#{approverId}, approval_comment=#{approvalComment}, " +
            "approval_time=#{approvalTime}, updated_at=NOW() WHERE id=#{id}")
    int updateApprovalInfo(Long id, Long approverId, String approvalComment, LocalDateTime approvalTime);

    @Update("UPDATE purchase_contract SET activation_time=#{activationTime}, updated_at=#{updateTime} WHERE id=#{id}")
    int updateActivationTime(Long id, LocalDateTime activationTime);

    @Update("UPDATE purchase_contract SET completion_time=#{completionTime}, updated_at=#{updateTime} WHERE id=#{id}")
    int updateCompletionTime(Long id, LocalDateTime completionTime);

    @Update("UPDATE purchase_contract SET termination_time=#{terminationTime}, termination_reason=#{terminationReason}, " +
            "updated_at=#{updateTime} WHERE id=#{id}")
    int updateTerminationInfo(Long id, LocalDateTime terminationTime, String terminationReason);

    @Update("UPDATE purchase_contract SET archive_no=#{archiveNo}, archive_time=#{archiveTime}, updated_at=#{updateTime} WHERE id=#{id}")
    int updateArchiveInfo(Long id, String archiveNo, LocalDateTime archiveTime);

    @Update("UPDATE purchase_contract SET executed_amount=#{executedAmount}, executed_percent=#{executedPercent}, " +
            "updated_at=NOW() WHERE id=#{id}")
    int updateExecutionProgress(Long id, BigDecimal executedAmount, BigDecimal executedPercent);
    
    int updateExecutionProgress(Long id, PurchaseContract contract);

    @Insert("INSERT INTO purchase_contract_modification (contract_id, modification_no, modification_reason, created_at) " +
            "VALUES (#{contractId}, #{modificationNo}, #{modificationReason}, #{createdAt})")
    int insertModification(@Param("contractId") Long contractId, @Param("modificationNo") String modificationNo, 
                          @Param("modificationReason") String modificationReason, @Param("createdAt") LocalDateTime createdAt);

    @Select("SELECT COUNT(*) FROM purchase_contract WHERE contract_status = #{status}")
    Long countByStatus(ContractStatus status);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM purchase_contract WHERE contract_status = #{status}")
    BigDecimal sumAmountByStatus(ContractStatus status);

    @Delete("DELETE FROM purchase_contract WHERE id = #{id}")
    int deleteById(Long id);
}
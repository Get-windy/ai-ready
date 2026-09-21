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

    /**
     * 即将到期的合同。
     *
     * <p>⚠️ 原实现是 `end_date <= #{date}` 而参数是 `int days`（天数），
     * PostgreSQL 会抛 `operator does not exist: timestamp <= integer` ——
     * 该查询从未成功执行过。改为「距今 days 天内到期」的正确语义。</p>
     */
    @Select("SELECT * FROM purchase_contract WHERE end_date <= (CURRENT_DATE + #{days}) "
            + "AND contract_status = 'ACTIVE' ORDER BY end_date ASC")
    List<PurchaseContract> findExpiringContracts(int days);

    @Insert("INSERT INTO purchase_contract (tenant_id, contract_no, inquiry_id, quote_id, supplier_id, " +
            "supplier_name, contract_title, contract_type, contract_status, total_amount, " +
            "start_date, end_date, payment_terms, delivery_terms, quality_standard, " +
            "warranty_period, contract_file_url, remark, created_by, created_at) VALUES " +
            "(#{tenantId}, #{contractNo}, #{inquiryId}, #{quoteId}, #{supplierId}, " +
            "#{supplierName}, #{contractTitle}, #{contractType}, #{contractStatus}, #{totalAmount}, " +
            "#{startDate}, #{endDate}, #{paymentTerms}, #{deliveryTerms}, #{qualityStandard}, " +
            "#{warrantyPeriod}, #{contractFileUrl}, #{remark}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseContract contract);

    /**
     * 合同分页查询（对应前端 `GET /erp/purchase/contract/page`）。
     *
     * <p>租户条件**不在此处写**：`purchase_contract` 带 `tenant_id` 且不在多租户忽略清单里，
     * 插件会自动注入当前租户条件。手写反而会与插件重复。</p>
     */
    @Select("<script>"
            + "SELECT * FROM purchase_contract "
            + "<where>"
            + "  <if test='contractNo != null and contractNo != \"\"'> AND contract_no LIKE CONCAT('%', #{contractNo}, '%')</if>"
            + "  <if test='contractTitle != null and contractTitle != \"\"'> AND contract_title LIKE CONCAT('%', #{contractTitle}, '%')</if>"
            + "  <if test='supplierName != null and supplierName != \"\"'> AND supplier_name LIKE CONCAT('%', #{supplierName}, '%')</if>"
            + "  <if test='supplierId != null'> AND supplier_id = #{supplierId}</if>"
            + "  <if test='contractStatus != null and contractStatus != \"\"'> AND contract_status = #{contractStatus}</if>"
            + "  <if test='dateStart != null and dateStart != \"\"'> AND created_at &gt;= CAST(#{dateStart} AS TIMESTAMP)</if>"
            + "  <if test='dateEnd != null and dateEnd != \"\"'> AND created_at &lt;= CAST(#{dateEnd} AS TIMESTAMP) + INTERVAL '1 day'</if>"
            + "</where>"
            + " ORDER BY created_at DESC"
            + "</script>")
    List<PurchaseContract> selectPageList(@Param("contractNo") String contractNo,
                                          @Param("contractTitle") String contractTitle,
                                          @Param("supplierName") String supplierName,
                                          @Param("supplierId") Long supplierId,
                                          @Param("contractStatus") String contractStatus,
                                          @Param("dateStart") String dateStart,
                                          @Param("dateEnd") String dateEnd,
                                          @Param("offset") long offset,
                                          @Param("limit") long limit);

    /** 与 {@link #selectPageList} 同条件的总数（分页用）。 */
    @Select("<script>"
            + "SELECT COUNT(*) FROM purchase_contract "
            + "<where>"
            + "  <if test='contractNo != null and contractNo != \"\"'> AND contract_no LIKE CONCAT('%', #{contractNo}, '%')</if>"
            + "  <if test='contractTitle != null and contractTitle != \"\"'> AND contract_title LIKE CONCAT('%', #{contractTitle}, '%')</if>"
            + "  <if test='supplierName != null and supplierName != \"\"'> AND supplier_name LIKE CONCAT('%', #{supplierName}, '%')</if>"
            + "  <if test='supplierId != null'> AND supplier_id = #{supplierId}</if>"
            + "  <if test='contractStatus != null and contractStatus != \"\"'> AND contract_status = #{contractStatus}</if>"
            + "  <if test='dateStart != null and dateStart != \"\"'> AND created_at &gt;= CAST(#{dateStart} AS TIMESTAMP)</if>"
            + "  <if test='dateEnd != null and dateEnd != \"\"'> AND created_at &lt;= CAST(#{dateEnd} AS TIMESTAMP) + INTERVAL '1 day'</if>"
            + "</where>"
            + "</script>")
    long countPageList(@Param("contractNo") String contractNo,
                       @Param("contractTitle") String contractTitle,
                       @Param("supplierName") String supplierName,
                       @Param("supplierId") Long supplierId,
                       @Param("contractStatus") String contractStatus,
                       @Param("dateStart") String dateStart,
                       @Param("dateEnd") String dateEnd);

    /**
     * 合同编辑。
     *
     * <p>补齐了供应商 / 质保期 / 合同文件 / 执行金额等前端表单会改、原语句却漏写的列 ——
     * 原实现只更新 7 个字段，用户改「供应商名称」「合同附件」保存后不生效（静默丢改动）。</p>
     */
    @Update("UPDATE purchase_contract SET contract_title=#{contractTitle}, contract_type=#{contractType}, " +
            "supplier_id=#{supplierId}, supplier_name=#{supplierName}, total_amount=#{totalAmount}, " +
            "start_date=#{startDate}, end_date=#{endDate}, payment_terms=#{paymentTerms}, " +
            "delivery_terms=#{deliveryTerms}, quality_standard=#{qualityStandard}, " +
            "warranty_period=#{warrantyPeriod}, contract_file_url=#{contractFileUrl}, " +
            "remark=#{remark}, updated_at=now() WHERE id=#{id}")
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

    /** 合同总数（统计卡片用；租户条件由多租户插件注入） */
    @Select("SELECT COUNT(*) FROM purchase_contract")
    Long countAll();

    /** 全部合同金额合计 */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM purchase_contract")
    BigDecimal sumAllAmount();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM purchase_contract WHERE contract_status = #{status}")
    BigDecimal sumAmountByStatus(ContractStatus status);

    @Delete("DELETE FROM purchase_contract WHERE id = #{id}")
    int deleteById(Long id);
}
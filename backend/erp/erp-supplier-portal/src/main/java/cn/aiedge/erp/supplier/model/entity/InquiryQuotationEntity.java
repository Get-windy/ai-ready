package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_inquiry_quotation")
public class InquiryQuotationEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("inquiry_no")
    private String inquiryNo;

    @TableField("inquiry_title")
    private String inquiryTitle;

    @TableField("inquiry_description")
    private String inquiryDescription;

    @TableField("inquirer_id")
    private String inquirerId;

    @TableField("inquirer_name")
    private String inquirerName;

    @TableField("inquiry_department")
    private String inquiryDepartment;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("product_id")
    private Long productId;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("product_spec")
    private String productSpec;

    @TableField("product_unit")
    private String productUnit;

    @TableField("inquiry_quantity")
    private BigDecimal inquiryQuantity;

    @TableField("inquiry_price")
    private BigDecimal inquiryPrice;

    @TableField("currency")
    private String currency;

    @TableField("expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @TableField("quotation_no")
    private String quotationNo;

    @TableField("quoter_id")
    private String quoterId;

    @TableField("quoter_name")
    private String quoterName;

    @TableField("quotation_date")
    private LocalDateTime quotationDate;

    @TableField("quotation_price")
    private BigDecimal quotationPrice;

    @TableField("quotation_quantity")
    private BigDecimal quotationQuantity;

    @TableField("quotation_total_amount")
    private BigDecimal quotationTotalAmount;

    @TableField("quotation_validity_period")
    private Integer quotationValidityPeriod;

    @TableField("quotation_status")
    private Integer quotationStatus;

    @TableField("quotation_remark")
    private String quotationRemark;

    @TableField("inquiry_status")
    private Integer inquiryStatus;

    @TableField("inquiry_source")
    private Integer inquirySource;

    @TableField("inquiry_priority")
    private Integer inquiryPriority;

    @TableField("inquiry_deadline")
    private LocalDateTime inquiryDeadline;

    @TableField("inquiry_sent_date")
    private LocalDateTime inquirySentDate;

    @TableField("inquiry_received_date")
    private LocalDateTime inquiryReceivedDate;

    @TableField("inquiry_confirmed_date")
    private LocalDateTime inquiryConfirmedDate;

    @TableField("inquiry_attachments")
    private String inquiryAttachments;

    @TableField("quotation_attachments")
    private String quotationAttachments;

    @TableField("communication_records")
    private String communicationRecords;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}
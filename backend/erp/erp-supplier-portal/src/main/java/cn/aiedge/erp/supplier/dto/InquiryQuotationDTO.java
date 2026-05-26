package cn.aiedge.erp.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 询价/报价数据传输对象
 */
@Data
@Schema(description = "询价/报价数据传输对象")
public class InquiryQuotationDTO {
    
    @Schema(description = "询价ID", example = "1")
    private Long id;
    
    @NotBlank(message = "询价单号不能为空")
    @Size(max = 50, message = "询价单号长度不能超过50个字符")
    @Schema(description = "询价单号", example = "IQ202400001", required = true)
    private String inquiryNo;
    
    @NotBlank(message = "询价标题不能为空")
    @Size(max = 200, message = "询价标题长度不能超过200个字符")
    @Schema(description = "询价标题", example = "2024年第一季度服务器采购询价", required = true)
    private String inquiryTitle;
    
    @Schema(description = "询价描述")
    private String inquiryDescription;
    
    @NotBlank(message = "询价发起人ID不能为空")
    @Size(max = 50, message = "询价发起人ID长度不能超过50个字符")
    @Schema(description = "询价发起人ID", example = "user001", required = true)
    private String inquirerId;
    
    @Size(max = 50, message = "询价发起人姓名长度不能超过50个字符")
    @Schema(description = "询价发起人姓名", example = "张三")
    private String inquirerName;
    
    @Size(max = 100, message = "询价部门长度不能超过100个字符")
    @Schema(description = "询价部门", example = "采购部")
    private String inquiryDepartment;
    
    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", example = "1", required = true)
    private Long supplierId;
    
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 50, message = "供应商编码长度不能超过50个字符")
    @Schema(description = "供应商编码", example = "SUP20240001", required = true)
    private String supplierCode;
    
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    @Schema(description = "供应商名称", example = "北京科技有限公司", required = true)
    private String supplierName;
    
    @Schema(description = "产品ID", example = "1001")
    private Long productId;
    
    @Size(max = 50, message = "产品编码长度不能超过50个字符")
    @Schema(description = "产品编码", example = "PROD001")
    private String productCode;
    
    @Size(max = 200, message = "产品名称长度不能超过200个字符")
    @Schema(description = "产品名称", example = "服务器主机")
    private String productName;
    
    @Size(max = 500, message = "产品规格长度不能超过500个字符")
    @Schema(description = "产品规格", example = "CPU: Intel Xeon Gold 6348, 内存: 256GB DDR4")
    private String productSpec;
    
    @Size(max = 20, message = "产品单位长度不能超过20个字符")
    @Schema(description = "产品单位", example = "台")
    private String productUnit;
    
    @Schema(description = "询价数量", example = "10")
    private BigDecimal inquiryQuantity;
    
    @Schema(description = "询价单价", example = "50000.00")
    private BigDecimal inquiryPrice;
    
    @Size(max = 10, message = "询价币种长度不能超过10个字符")
    @Schema(description = "询价币种", example = "CNY")
    private String currency;
    
    @Schema(description = "期望交付日期", example = "2024-03-31T00:00:00")
    private LocalDateTime expectedDeliveryDate;
    
    @Size(max = 50, message = "报价单号长度不能超过50个字符")
    @Schema(description = "报价单号", example = "QT202400001")
    private String quotationNo;
    
    @Size(max = 50, message = "报价人ID长度不能超过50个字符")
    @Schema(description = "报价人ID", example = "supplier001")
    private String quoterId;
    
    @Size(max = 50, message = "报价人姓名长度不能超过50个字符")
    @Schema(description = "报价人姓名", example = "王五")
    private String quoterName;
    
    @Schema(description = "报价日期", example = "2024-01-15T00:00:00")
    private LocalDateTime quotationDate;
    
    @Schema(description = "报价单价", example = "48000.00")
    private BigDecimal quotationPrice;
    
    @Schema(description = "报价数量", example = "10")
    private BigDecimal quotationQuantity;
    
    @Schema(description = "报价总金额", example = "480000.00")
    private BigDecimal quotationTotalAmount;
    
    @Schema(description = "报价有效期（天数）", example = "30")
    private Integer quotationValidityPeriod;
    
    @Schema(description = "报价状态：1-已报价，2-报价确认中，3-报价已确认，4-报价已拒绝", example = "1")
    private Integer quotationStatus;
    
    @Schema(description = "报价备注")
    private String quotationRemark;
    
    @NotNull(message = "询价状态不能为空")
    @Schema(description = "询价状态：1-待询价，2-已询价，3-询价完成，4-已取消", example = "1", required = true)
    private Integer inquiryStatus;
    
    @Schema(description = "询价来源：1-采购申请，2-库存补货，3-销售订单，4-其他", example = "1")
    private Integer inquirySource;
    
    @Schema(description = "询价优先级：1-低，2-中，3-高，4-紧急", example = "3")
    private Integer inquiryPriority;
    
    @Schema(description = "询价截止日期", example = "2024-01-20T00:00:00")
    private LocalDateTime inquiryDeadline;
    
    @Schema(description = "询价发送日期", example = "2024-01-10T00:00:00")
    private LocalDateTime inquirySentDate;
    
    @Schema(description = "询价接收日期", example = "2024-01-11T00:00:00")
    private LocalDateTime inquiryReceivedDate;
    
    @Schema(description = "询价确认日期", example = "2024-01-12T00:00:00")
    private LocalDateTime inquiryConfirmedDate;
    
    @Schema(description = "询价附件信息")
    private List<Map<String, Object>> inquiryAttachments;
    
    @Schema(description = "报价附件信息")
    private List<Map<String, Object>> quotationAttachments;
    
    @Schema(description = "沟通记录")
    private List<Map<String, Object>> communicationRecords;
    
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    
    @Schema(description = "创建时间", example = "2024-01-01T00:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    
    @Schema(description = "更新时间", example = "2024-01-01T00:00:00")
    private LocalDateTime updateTime;
}
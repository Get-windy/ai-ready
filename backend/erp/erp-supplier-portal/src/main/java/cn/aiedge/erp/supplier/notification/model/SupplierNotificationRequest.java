package cn.aiedge.erp.supplier.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 供应商通知请求模型
 * 用于发送通知到供应商
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "供应商通知请求")
public class SupplierNotificationRequest {
    
    @Schema(description = "供应商ID", required = true)
    private Long supplierId;
    
    @Schema(description = "模板编码", required = true, 
            example = "SUPPLIER_PURCHASE_ORDER_CREATED")
    private String templateCode;
    
    @Schema(description = "通知标题（如果为空则使用模板标题）")
    private String title;
    
    @Schema(description = "通知内容（如果为空则使用模板内容）")
    private String content;
    
    @Schema(description = "模板变量")
    private Map<String, Object> variables;
    
    @Schema(description = "业务上下文")
    private BusinessContext businessContext;
    
    @Schema(description = "渠道列表，逗号分隔（如果为空则使用供应商配置）")
    private String channels;
    
    @Schema(description = "通知优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急", 
            defaultValue = "NORMAL")
    private String priority = "NORMAL";
    
    @Schema(description = "是否需要回执")
    private Boolean requireReceipt = false;
    
    @Schema(description = "是否异步发送（建议开启）", defaultValue = "true")
    private Boolean async = true;
    
    @Schema(description = "定时发送时间（如果为空则立即发送）")
    private LocalDateTime scheduledTime;
    
    @Schema(description = "标签，逗号分隔")
    private String tags;
    
    @Schema(description = "语言", defaultValue = "zh-CN")
    private String language = "zh-CN";
    
    @Schema(description = "发送人ID")
    private Long senderId;
    
    @Schema(description = "发送人姓名")
    private String senderName;
    
    @Schema(description = "发送IP地址")
    private String senderIp;
    
    @Schema(description = "是否强制发送（忽略静默时段和限制）")
    private Boolean forceSend = false;
    
    @Schema(description = "是否仅记录不实际发送（测试模式）")
    private Boolean dryRun = false;
    
    @Schema(description = "扩展数据")
    private Map<String, Object> extraData;
    
    @Schema(description = "租户ID")
    private Long tenantId;
    
    /**
     * 验证请求是否有效
     */
    public boolean isValid() {
        if (supplierId == null || supplierId <= 0) {
            return false;
        }
        
        if (templateCode == null || templateCode.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取简化的日志信息
     */
    public String toLogString() {
        return String.format("SupplierNotificationRequest{supplierId=%d, templateCode=%s, priority=%s}", 
                supplierId, templateCode, priority);
    }
    
    /**
     * 业务上下文模型
     */
    @Data
    @Schema(description = "业务上下文")
    public static class BusinessContext {
        
        @Schema(description = "业务类型，如：PURCHASE_ORDER, PAYMENT, QUALITY, PERFORMANCE")
        private String type;
        
        @Schema(description = "业务ID")
        private String id;
        
        @Schema(description = "业务名称")
        private String name;
        
        @Schema(description = "业务关联时间")
        private LocalDateTime businessTime;
        
        @Schema(description = "业务金额（分）")
        private Long amount;
        
        @Schema(description = "业务状态")
        private String status;
        
        @Schema(description = "业务额外信息")
        private Map<String, Object> extra;
        
        /**
         * 验证业务上下文
         */
        public boolean isValid() {
            return type != null && id != null;
        }
        
        /**
         * 获取简化的日志信息
         */
        public String toLogString() {
            return String.format("BusinessContext{type=%s, id=%s, name=%s}", type, id, name);
        }
    }
}
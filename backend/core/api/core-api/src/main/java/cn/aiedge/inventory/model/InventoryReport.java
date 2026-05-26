package cn.aiedge.inventory.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存报告模型类
 * 用于记录库存盘点、统计和分析报告
 */
@Entity
@Table(name = "inventory_reports")
public class InventoryReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 报告编号（唯一）
     */
    @Column(name = "report_no", nullable = false, unique = true, length = 50)
    private String reportNo;
    
    /**
     * 报告标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    /**
     * 报告描述
     */
    @Column(name = "description", length = 1000)
    private String description;
    
    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;
    
    /**
     * 报告类型：1-日报 2-周报 3-月报 4-年报 5-临时报告
     */
    @Column(name = "report_type", nullable = false)
    private Integer reportType;
    
    /**
     * 报告时间（报告对应的日期/周期）
     */
    @Column(name = "report_time", nullable = false)
    private LocalDateTime reportTime;
    
    /**
     * 报告周期开始时间
     */
    @Column(name = "period_start_time", nullable = false)
    private LocalDateTime periodStartTime;
    
    /**
     * 报告周期结束时间
     */
    @Column(name = "period_end_time", nullable = false)
    private LocalDateTime periodEndTime;
    
    /**
     * 总商品种类数
     */
    @Column(name = "total_product_types")
    private Integer totalProductTypes;
    
    /**
     * 总SKU数量
     */
    @Column(name = "total_sku_count")
    private Integer totalSkuCount;
    
    /**
     * 总库存数量
     */
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    /**
     * 总库存价值
     */
    @Column(name = "total_value", precision = 19, scale = 4)
    private BigDecimal totalValue;
    
    /**
     * 可用库存价值
     */
    @Column(name = "available_value", precision = 19, scale = 4)
    private BigDecimal availableValue;
    
    /**
     * 安全库存比例
     */
    @Column(name = "safety_stock_ratio", precision = 5, scale = 2)
    private BigDecimal safetyStockRatio;
    
    /**
     * 周转天数
     */
    @Column(name = "turnover_days", precision = 8, scale = 2)
    private BigDecimal turnoverDays;
    
    /**
     * 缺货率
     */
    @Column(name = "stockout_rate", precision = 5, scale = 2)
    private BigDecimal stockoutRate;
    
    /**
     * 过期商品数量
     */
    @Column(name = "expired_product_count")
    private Integer expiredProductCount;
    
    /**
     * 临期商品数量
     */
    @Column(name = "near_expiry_product_count")
    private Integer nearExpiryProductCount;
    
    /**
     * 状态：1-待审核 2-已审核 3-已发布 4-已归档 5-已作废
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
    
    /**
     * 审核人ID
     */
    @Column(name = "auditor_id")
    private Long auditorId;
    
    /**
     * 审核人姓名
     */
    @Column(name = "auditor_name", length = 50)
    private String auditorName;
    
    /**
     * 审核时间
     */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;
    
    /**
     * 审核意见
     */
    @Column(name = "audit_opinion", length = 500)
    private String auditOpinion;
    
    /**
     * 发布人ID
     */
    @Column(name = "publisher_id")
    private Long publisherId;
    
    /**
     * 发布人姓名
     */
    @Column(name = "publisher_name", length = 50)
    private String publisherName;
    
    /**
     * 发布时间
     */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;
    
    /**
     * 归档人ID
     */
    @Column(name = "archiver_id")
    private Long archiverId;
    
    /**
     * 归档人姓名
     */
    @Column(name = "archiver_name", length = 50)
    private String archiverName;
    
    /**
     * 归档时间
     */
    @Column(name = "archive_time")
    private LocalDateTime archiveTime;
    
    /**
     * 数据最后更新时间（用于判断是否需要重新计算）
     */
    @Column(name = "last_data_time")
    private LocalDateTime lastDataTime;
    
    /**
     * 报表文件路径
     */
    @Column(name = "report_file_path", length = 500)
    private String reportFilePath;
    
    /**
     * 报表文件大小（字节）
     */
    @Column(name = "report_file_size")
    private Long reportFileSize;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 1000)
    private String remark;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    @Column(name = "create_user_id")
    private Long createUserId;
    
    /**
     * 创建人姓名
     */
    @Column(name = "create_user_name", length = 50)
    private String createUserName;
    
    /**
     * 更新人ID
     */
    @Column(name = "update_user_id")
    private Long updateUserId;
    
    /**
     * 更新人姓名
     */
    @Column(name = "update_user_name", length = 50)
    private String updateUserName;
    
    // 报告明细项（一对多关系）
    @OneToMany(mappedBy = "inventoryReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryReportItem> inventoryReportItems;
    
    // 构造函数
    public InventoryReport() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    public InventoryReport(String reportNo, String title, Long warehouseId, String warehouseName, 
                          Integer reportType, LocalDateTime reportTime, 
                          LocalDateTime periodStartTime, LocalDateTime periodEndTime) {
        this();
        this.reportNo = reportNo;
        this.title = title;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.reportType = reportType;
        this.reportTime = reportTime;
        this.periodStartTime = periodStartTime;
        this.periodEndTime = periodEndTime;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getReportNo() {
        return reportNo;
    }
    
    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public String getWarehouseName() {
        return warehouseName;
    }
    
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
    
    public Integer getReportType() {
        return reportType;
    }
    
    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }
    
    public LocalDateTime getReportTime() {
        return reportTime;
    }
    
    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }
    
    public LocalDateTime getPeriodStartTime() {
        return periodStartTime;
    }
    
    public void setPeriodStartTime(LocalDateTime periodStartTime) {
        this.periodStartTime = periodStartTime;
    }
    
    public LocalDateTime getPeriodEndTime() {
        return periodEndTime;
    }
    
    public void setPeriodEndTime(LocalDateTime periodEndTime) {
        this.periodEndTime = periodEndTime;
    }
    
    public Integer getTotalProductTypes() {
        return totalProductTypes;
    }
    
    public void setTotalProductTypes(Integer totalProductTypes) {
        this.totalProductTypes = totalProductTypes;
    }
    
    public Integer getTotalSkuCount() {
        return totalSkuCount;
    }
    
    public void setTotalSkuCount(Integer totalSkuCount) {
        this.totalSkuCount = totalSkuCount;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public BigDecimal getAvailableValue() {
        return availableValue;
    }
    
    public void setAvailableValue(BigDecimal availableValue) {
        this.availableValue = availableValue;
    }
    
    public BigDecimal getSafetyStockRatio() {
        return safetyStockRatio;
    }
    
    public void setSafetyStockRatio(BigDecimal safetyStockRatio) {
        this.safetyStockRatio = safetyStockRatio;
    }
    
    public BigDecimal getTurnoverDays() {
        return turnoverDays;
    }
    
    public void setTurnoverDays(BigDecimal turnoverDays) {
        this.turnoverDays = turnoverDays;
    }
    
    public BigDecimal getStockoutRate() {
        return stockoutRate;
    }
    
    public void setStockoutRate(BigDecimal stockoutRate) {
        this.stockoutRate = stockoutRate;
    }
    
    public Integer getExpiredProductCount() {
        return expiredProductCount;
    }
    
    public void setExpiredProductCount(Integer expiredProductCount) {
        this.expiredProductCount = expiredProductCount;
    }
    
    public Integer getNearExpiryProductCount() {
        return nearExpiryProductCount;
    }
    
    public void setNearExpiryProductCount(Integer nearExpiryProductCount) {
        this.nearExpiryProductCount = nearExpiryProductCount;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Long getAuditorId() {
        return auditorId;
    }
    
    public void setAuditorId(Long auditorId) {
        this.auditorId = auditorId;
    }
    
    public String getAuditorName() {
        return auditorName;
    }
    
    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }
    
    public LocalDateTime getAuditTime() {
        return auditTime;
    }
    
    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }
    
    public String getAuditOpinion() {
        return auditOpinion;
    }
    
    public void setAuditOpinion(String auditOpinion) {
        this.auditOpinion = auditOpinion;
    }
    
    public Long getPublisherId() {
        return publisherId;
    }
    
    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }
    
    public String getPublisherName() {
        return publisherName;
    }
    
    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }
    
    public LocalDateTime getPublishTime() {
        return publishTime;
    }
    
    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
    
    public Long getArchiverId() {
        return archiverId;
    }
    
    public void setArchiverId(Long archiverId) {
        this.archiverId = archiverId;
    }
    
    public String getArchiverName() {
        return archiverName;
    }
    
    public void setArchiverName(String archiverName) {
        this.archiverName = archiverName;
    }
    
    public LocalDateTime getArchiveTime() {
        return archiveTime;
    }
    
    public void setArchiveTime(LocalDateTime archiveTime) {
        this.archiveTime = archiveTime;
    }
    
    public LocalDateTime getLastDataTime() {
        return lastDataTime;
    }
    
    public void setLastDataTime(LocalDateTime lastDataTime) {
        this.lastDataTime = lastDataTime;
    }
    
    public String getReportFilePath() {
        return reportFilePath;
    }
    
    public void setReportFilePath(String reportFilePath) {
        this.reportFilePath = reportFilePath;
    }
    
    public Long getReportFileSize() {
        return reportFileSize;
    }
    
    public void setReportFileSize(Long reportFileSize) {
        this.reportFileSize = reportFileSize;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Long getCreateUserId() {
        return createUserId;
    }
    
    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
    
    public String getCreateUserName() {
        return createUserName;
    }
    
    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }
    
    public Long getUpdateUserId() {
        return updateUserId;
    }
    
    public void setUpdateUserId(Long updateUserId) {
        this.updateUserId = updateUserId;
    }
    
    public String getUpdateUserName() {
        return updateUserName;
    }
    
    public void setUpdateUserName(String updateUserName) {
        this.updateUserName = updateUserName;
    }
    
    public List<InventoryReportItem> getInventoryReportItems() {
        return inventoryReportItems;
    }
    
    public void setInventoryReportItems(List<InventoryReportItem> inventoryReportItems) {
        this.inventoryReportItems = inventoryReportItems;
    }
    
    /**
     * 检查报告是否可审核
     */
    public boolean canAudit() {
        return this.status == 1; // 只有待审核状态可以审核
    }
    
    /**
     * 检查报告是否可发布
     */
    public boolean canPublish() {
        return this.status == 2; // 只有已审核状态可以发布
    }
    
    /**
     * 检查报告是否可归档
     */
    public boolean canArchive() {
        return this.status == 3 && this.publishTime != null && 
               this.publishTime.isBefore(LocalDateTime.now().minusDays(30)); // 发布30天后可归档
    }
    
    /**
     * 检查报告是否可作废
     */
    public boolean canCancel() {
        return this.status == 1 || this.status == 2; // 待审核或已审核状态可作废
    }
    
    /**
     * 生成报告摘要
     */
    public String generateSummary() {
        return String.format("库存报告[%s]: %s, 仓库: %s, 库存价值: %s, 状态: %d", 
                reportNo, title, warehouseName, 
                totalValue != null ? totalValue.toString() : "0", 
                status);
    }
    
    @Override
    public String toString() {
        return "InventoryReport{" +
                "id=" + id +
                ", reportNo='" + reportNo + '\'' +
                ", title='" + title + '\'' +
                ", warehouseId=" + warehouseId +
                ", warehouseName='" + warehouseName + '\'' +
                ", reportType=" + reportType +
                ", status=" + status +
                ", totalValue=" + totalValue +
                '}';
    }
}
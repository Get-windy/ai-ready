package cn.aiedge.erp.invoice.service;

import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票申请服务接口
 * 提供发票申请相关的业务逻辑操作
 */
public interface InvoiceApplicationService {
    
    /**
     * 创建发票申请
     * 
     * @param application 发票申请对象
     * @return 创建的发票申请
     */
    InvoiceApplication createInvoiceApplication(InvoiceApplication application);
    
    /**
     * 根据ID获取发票申请
     * 
     * @param id 申请ID
     * @return 发票申请对象（如果存在）
     */
    Optional<InvoiceApplication> getInvoiceApplicationById(Long id);
    
    /**
     * 获取所有发票申请
     * 
     * @return 发票申请列表
     */
    List<InvoiceApplication> getAllInvoiceApplications();
    
    /**
     * 分页获取发票申请
     * 
     * @param pageable 分页参数
     * @return 发票申请分页
     */
    Page<InvoiceApplication> getInvoiceApplications(Pageable pageable);
    
    /**
     * 根据状态获取发票申请
     * 
     * @param status 申请状态
     * @return 发票申请列表
     */
    List<InvoiceApplication> getInvoiceApplicationsByStatus(InvoiceStatus status);
    
    /**
     * 根据客户ID获取发票申请
     * 
     * @param customerId 客户ID
     * @return 发票申请列表
     */
    List<InvoiceApplication> getInvoiceApplicationsByCustomerId(Long customerId);
    
    /**
     * 根据供应商ID获取发票申请
     * 
     * @param supplierId 供应商ID
     * @return 发票申请列表
     */
    List<InvoiceApplication> getInvoiceApplicationsBySupplierId(Long supplierId);
    
    /**
     * 根据日期范围获取发票申请
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票申请列表
     */
    List<InvoiceApplication> getInvoiceApplicationsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * 更新发票申请
     * 
     * @param id 申请ID
     * @param application 更新的申请对象
     * @return 更新后的申请对象
     */
    InvoiceApplication updateInvoiceApplication(Long id, InvoiceApplication application);
    
    /**
     * 删除发票申请（逻辑删除）
     * 
     * @param id 申请ID
     * @param deletedBy 删除人ID
     * @return 是否成功删除
     */
    boolean deleteInvoiceApplication(Long id, String deletedBy);
    
    /**
     * 提交发票申请
     * 
     * @param id 申请ID
     * @param submittedBy 提交人ID
     * @return 是否成功提交
     */
    boolean submitInvoiceApplication(Long id, Long submittedBy);
    
    /**
     * 批准发票申请
     * 
     * @param id 申请ID
     * @param approvedBy 批准人ID
     * @param notes 批准意见
     * @return 是否成功批准
     */
    boolean approveInvoiceApplication(Long id, Long approvedBy, String notes);
    
    /**
     * 拒绝发票申请
     * 
     * @param id 申请ID
     * @param rejectedBy 拒绝人ID
     * @param reason 拒绝原因
     * @return 是否成功拒绝
     */
    boolean rejectInvoiceApplication(Long id, Long rejectedBy, String reason);
    
    /**
     * 取消发票申请
     * 
     * @param id 申请ID
     * @param cancelledBy 取消人ID
     * @param reason 取消原因
     * @return 是否成功取消
     */
    boolean cancelInvoiceApplication(Long id, Long cancelledBy, String reason);
    
    /**
     * 生成发票
     * 
     * @param id 申请ID
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 生成的发票ID
     */
    Long generateInvoiceFromApplication(Long id, Long issuedBy, String issuedByName);
    
    /**
     * 验证发票申请数据
     * 
     * @param application 发票申请对象
     * @return 是否有效
     */
    boolean validateInvoiceApplication(InvoiceApplication application);
    
    /**
     * 搜索发票申请
     * 
     * @param keyword 关键词
     * @return 发票申请列表
     */
    List<InvoiceApplication> searchInvoiceApplications(String keyword);
}
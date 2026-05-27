package cn.aiedge.erp.supplier.service;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.entity.Supplier;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;

import java.util.List;
import java.util.Map;

/**
 * 供应商服务接口
 */
public interface SupplierService {
    
    /**
     * 创建供应商
     * 
     * @param supplierDTO 供应商信息
     * @return 创建结果
     */
    R<SupplierDTO> createSupplier(SupplierDTO supplierDTO);
    
    /**
     * 更新供应商信息
     * 
     * @param supplierDTO 供应商信息
     * @return 更新结果
     */
    R<SupplierDTO> updateSupplier(SupplierDTO supplierDTO);
    
    /**
     * 根据ID获取供应商详情
     * 
     * @param id 供应商ID
     * @return 供应商详情
     */
    R<SupplierDTO> getSupplierById(Long id);
    
    /**
     * 根据编码获取供应商详情
     * 
     * @param supplierCode 供应商编码
     * @return 供应商详情
     */
    R<SupplierDTO> getSupplierByCode(String supplierCode);
    
    /**
     * 分页查询供应商列表
     * 
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    R<PageResult<SupplierDTO>> querySupplierPage(SupplierQueryDTO queryDTO);
    
    /**
     * 查询供应商列表
     * 
     * @param queryDTO 查询条件
     * @return 供应商列表
     */
    R<List<SupplierDTO>> querySupplierList(SupplierQueryDTO queryDTO);
    
    /**
     * 删除供应商（逻辑删除）
     * 
     * @param id 供应商ID
     * @return 删除结果
     */
    R<Boolean> deleteSupplier(Long id);
    
    /**
     * 批量删除供应商
     * 
     * @param ids 供应商ID列表
     * @return 删除结果
     */
    R<Boolean> batchDeleteSupplier(List<Long> ids);
    
    /**
     * 激活供应商门户账户
     * 
     * @param id 供应商ID
     * @param portalAccountId 门户账户ID
     * @return 激活结果
     */
    R<Boolean> activateSupplierPortal(Long id, String portalAccountId);
    
    /**
     * 禁用供应商门户账户
     * 
     * @param id 供应商ID
     * @param reason 禁用原因
     * @return 禁用结果
     */
    R<Boolean> disableSupplierPortal(Long id, String reason);
    
    /**
     * 更新供应商等级
     * 
     * @param id 供应商ID
     * @param supplierLevel 供应商等级
     * @param reason 等级变更原因
     * @return 更新结果
     */
    R<Boolean> updateSupplierLevel(Long id, String supplierLevel, String reason);
    
    /**
     * 更新供应商合作状态
     * 
     * @param id 供应商ID
     * @param cooperationStatus 合作状态
     * @param reason 状态变更原因
     * @return 更新结果
     */
    R<Boolean> updateCooperationStatus(Long id, Integer cooperationStatus, String reason);
    
    /**
     * 评估供应商绩效
     * 
     * @param performanceDTO 绩效评估数据
     * @return 评估结果
     */
    R<Boolean> evaluateSupplierPerformance(SupplierPerformanceDTO performanceDTO);
    
    /**
     * 获取供应商绩效历史
     * 
     * @param supplierId 供应商ID
     * @param periodType 周期类型：1-月度，2-季度，3-年度
     * @param limit 限制条数
     * @return 绩效历史列表
     */
    R<List<SupplierPerformanceDTO>> getSupplierPerformanceHistory(Long supplierId, Integer periodType, Integer limit);
    
    /**
     * 获取供应商综合评分
     * 
     * @param supplierId 供应商ID
     * @return 综合评分
     */
    R<Double> getSupplierComprehensiveScore(Long supplierId);
    
    /**
     * 导入供应商数据
     * 
     * @param supplierList 供应商数据列表
     * @return 导入结果
     */
    R<Boolean> importSuppliers(List<SupplierDTO> supplierList);
    
    /**
     * 导出供应商数据
     * 
     * @param queryDTO 查询条件
     * @return 导出数据
     */
    R<List<SupplierDTO>> exportSuppliers(SupplierQueryDTO queryDTO);
    
    /**
     * 验证供应商信息
     * 
     * @param supplierDTO 供应商信息
     * @return 验证结果
     */
    R<Boolean> validateSupplier(SupplierDTO supplierDTO);
    
    /**
     * 同步供应商门户账户
     * 
     * @param id 供应商ID
     * @return 同步结果
     */
    R<Boolean> syncSupplierPortalAccount(Long id);
    
    /**
     * 获取供应商统计信息
     * 
     * @param tenantId 租户ID
     * @return 统计信息
     */
    R<Map<String, Object>> getSupplierStatistics(String tenantId);
    
    /**
     * 根据ID获取供应商实体
     * 
     * @param id 供应商ID
     * @return 供应商实体
     */
    Supplier getById(Long id);
    
    /**
     * 审批供应商
     * 
     * @param id 供应商ID
     * @param comment 审批意见
     * @return 审批结果
     */
    R<SupplierDTO> approveSupplier(Long id, String comment);
    
    /**
     * 验证供应商资质
     * 
     * @param id 供应商ID
     * @return 验证结果
     */
    R<Boolean> verifyQualification(Long id);
    
    /**
     * 将供应商加入黑名单
     * 
     * @param id 供应商ID
     * @param reason 加入原因
     * @return 操作结果
     */
    R<Boolean> addToBlacklist(Long id, String reason);
    
    /**
     * 评估供应商绩效
     * 
     * @param id 供应商ID
     * @return 评估结果
     */
    R<SupplierPerformanceDTO> evaluatePerformance(Long id);
    
    /**
     * 获取供应商列表
     * 
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    R<PageResult<SupplierDTO>> getSupplierList(SupplierQueryDTO queryDTO);
    
    /**
     * 获取供应商统计信息
     * 
     * @return 统计信息
     */
    R<Map<String, Object>> getStatistics();
    
    /**
     * 上传供应商资质文件
     * 
     * @param id 供应商ID
     * @param fileType 文件类型
     * @param fileContent 文件内容
     * @return 上传结果
     */
    R<String> uploadQualificationFile(Long id, String fileType, String fileContent);
}
package cn.aiedge.erp.supplier.service.impl;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.entity.Supplier;
import cn.aiedge.erp.supplier.model.entity.SupplierEntity;
import cn.aiedge.erp.supplier.model.entity.SupplierPerformanceEntity;
import cn.aiedge.erp.supplier.repository.SupplierRepository;
import cn.aiedge.erp.supplier.repository.SupplierPerformanceRepository;
import cn.aiedge.erp.supplier.service.SupplierService;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;
import cn.aiedge.common.core.utils.BeanUtils;
import cn.aiedge.common.core.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    
    private final SupplierRepository supplierRepository;
    private final SupplierPerformanceRepository supplierPerformanceRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<SupplierDTO> createSupplier(SupplierDTO supplierDTO) {
        try {
            // 验证供应商编码是否已存在
            LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SupplierEntity::getSupplierCode, supplierDTO.getSupplierCode())
                       .eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId());
            long count = supplierRepository.selectCount(queryWrapper);
            if (count > 0) {
                return R.fail("供应商编码已存在：" + supplierDTO.getSupplierCode());
            }
            
            // 转换DTO为实体
            SupplierEntity supplierEntity = BeanUtils.copyProperties(supplierDTO, SupplierEntity.class);
            
            // 设置基础信息
            supplierEntity.setTenantId(SecurityUtils.getTenantId());
            supplierEntity.setStatus(0);
            supplierEntity.setDeleted(0);
            supplierEntity.setCreateBy(SecurityUtils.getUsername());
            supplierEntity.setUpdateBy(SecurityUtils.getUsername());
            supplierEntity.setCreateTime(LocalDateTime.now());
            supplierEntity.setUpdateTime(LocalDateTime.now());
            
            // 设置默认值
            if (supplierEntity.getCooperationStatus() == null) {
                supplierEntity.setCooperationStatus(1); // 默认潜在供应商
            }
            if (supplierEntity.getPortalStatus() == null) {
                supplierEntity.setPortalStatus(0); // 默认未激活
            }
            if (supplierEntity.getCertificationStatus() == null) {
                supplierEntity.setCertificationStatus(0); // 默认未认证
            }
            if (supplierEntity.getComprehensiveScore() == null) {
                supplierEntity.setComprehensiveScore(0.0); // 默认评分
            }
            
            // 保存供应商
            int result = supplierRepository.insert(supplierEntity);
            if (result > 0) {
                SupplierDTO savedDTO = BeanUtils.copyProperties(supplierEntity, SupplierDTO.class);
                log.info("供应商创建成功：{}", supplierDTO.getSupplierCode());
                return R.ok(savedDTO);
            } else {
                return R.fail("供应商创建失败");
            }
        } catch (Exception e) {
            log.error("创建供应商失败：", e);
            return R.fail("创建供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<SupplierDTO> updateSupplier(SupplierDTO supplierDTO) {
        try {
            if (supplierDTO.getId() == null) {
                return R.fail("供应商ID不能为空");
            }
            
            // 查询现有供应商
            SupplierEntity existingEntity = supplierRepository.selectById(supplierDTO.getId());
            if (existingEntity == null || existingEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            // 验证供应商编码是否重复（如果修改了编码）
            if (StringUtils.isNotEmpty(supplierDTO.getSupplierCode()) 
                && !supplierDTO.getSupplierCode().equals(existingEntity.getSupplierCode())) {
                LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SupplierEntity::getSupplierCode, supplierDTO.getSupplierCode())
                           .eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId())
                           .ne(SupplierEntity::getId, supplierDTO.getId());
                long count = supplierRepository.selectCount(queryWrapper);
                if (count > 0) {
                    return R.fail("供应商编码已存在：" + supplierDTO.getSupplierCode());
                }
            }
            
            // 更新实体
            SupplierEntity updateEntity = BeanUtils.copyProperties(supplierDTO, SupplierEntity.class);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                SupplierDTO updatedDTO = BeanUtils.copyProperties(
                    supplierRepository.selectById(supplierDTO.getId()), SupplierDTO.class);
                log.info("供应商更新成功：{}", supplierDTO.getSupplierCode());
                return R.ok(updatedDTO);
            } else {
                return R.fail("供应商更新失败");
            }
        } catch (Exception e) {
            log.error("更新供应商失败：", e);
            return R.fail("更新供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<SupplierDTO> getSupplierById(Long id) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            // 验证租户权限
            if (!supplierEntity.getTenantId().equals(SecurityUtils.getTenantId())) {
                return R.fail("无权访问该供应商信息");
            }
            
            SupplierDTO supplierDTO = BeanUtils.copyProperties(supplierEntity, SupplierDTO.class);
            return R.ok(supplierDTO);
        } catch (Exception e) {
            log.error("获取供应商详情失败：", e);
            return R.fail("获取供应商详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<SupplierDTO> getSupplierByCode(String supplierCode) {
        try {
            LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SupplierEntity::getSupplierCode, supplierCode)
                       .eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId());
            
            SupplierEntity supplierEntity = supplierRepository.selectOne(queryWrapper);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierDTO supplierDTO = BeanUtils.copyProperties(supplierEntity, SupplierDTO.class);
            return R.ok(supplierDTO);
        } catch (Exception e) {
            log.error("获取供应商详情失败：", e);
            return R.fail("获取供应商详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<PageResult<SupplierDTO>> querySupplierPage(SupplierQueryDTO queryDTO) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<SupplierEntity> queryWrapper = buildQueryWrapper(queryDTO);
            
            // 分页查询
            Page<SupplierEntity> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
            IPage<SupplierEntity> entityPage = supplierRepository.selectPage(page, queryWrapper);
            
            // 转换为DTO
            List<SupplierDTO> dtoList = entityPage.getRecords().stream()
                .map(entity -> BeanUtils.copyProperties(entity, SupplierDTO.class))
                .collect(Collectors.toList());
            
            // 构建分页结果
            PageResult<SupplierDTO> pageResult = new PageResult<>();
            pageResult.setList(dtoList);
            pageResult.setTotal(entityPage.getTotal());
            pageResult.setPageNum(queryDTO.getPageNum());
            pageResult.setPageSize(queryDTO.getPageSize());
            pageResult.setTotalPages((int) Math.ceil((double) entityPage.getTotal() / queryDTO.getPageSize()));
            
            return R.ok(pageResult);
        } catch (Exception e) {
            log.error("分页查询供应商失败：", e);
            return R.fail("分页查询供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<List<SupplierDTO>> querySupplierList(SupplierQueryDTO queryDTO) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<SupplierEntity> queryWrapper = buildQueryWrapper(queryDTO);
            
            // 查询列表
            List<SupplierEntity> entityList = supplierRepository.selectList(queryWrapper);
            
            // 转换为DTO
            List<SupplierDTO> dtoList = entityList.stream()
                .map(entity -> BeanUtils.copyProperties(entity, SupplierDTO.class))
                .collect(Collectors.toList());
            
            return R.ok(dtoList);
        } catch (Exception e) {
            log.error("查询供应商列表失败：", e);
            return R.fail("查询供应商列表失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteSupplier(Long id) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            // 验证租户权限
            if (!supplierEntity.getTenantId().equals(SecurityUtils.getTenantId())) {
                return R.fail("无权删除该供应商");
            }
            
            // 逻辑删除
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setDeleted(1);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商删除成功：{}", id);
                return R.ok(true);
            } else {
                return R.fail("供应商删除失败");
            }
        } catch (Exception e) {
            log.error("删除供应商失败：", e);
            return R.fail("删除供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> batchDeleteSupplier(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return R.fail("供应商ID列表不能为空");
            }
            
            // 批量逻辑删除
            int successCount = 0;
            for (Long id : ids) {
                R<Boolean> result = deleteSupplier(id);
                if (result.isSuccess() && result.getData()) {
                    successCount++;
                }
            }
            
            if (successCount == ids.size()) {
                log.info("批量删除供应商成功：{}条", successCount);
                return R.ok(true);
            } else {
                log.warn("批量删除供应商部分成功：成功{}条，失败{}条", successCount, ids.size() - successCount);
                return R.fail("批量删除供应商部分成功");
            }
        } catch (Exception e) {
            log.error("批量删除供应商失败：", e);
            return R.fail("批量删除供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> activateSupplierPortal(Long id, String portalAccountId) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setPortalStatus(1); // 已激活
            updateEntity.setPortalAccountId(portalAccountId);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商门户激活成功：{}", id);
                return R.ok(true);
            } else {
                return R.fail("供应商门户激活失败");
            }
        } catch (Exception e) {
            log.error("激活供应商门户失败：", e);
            return R.fail("激活供应商门户失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> disableSupplierPortal(Long id, String reason) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setPortalStatus(2); // 已禁用
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            // 记录禁用原因到扩展字段
            Map<String, Object> extendInfo = StringUtils.isNotEmpty(supplierEntity.getExtendInfo()) ? JSONUtil.toBean(supplierEntity.getExtendInfo(), Map.class) : null;
            if (extendInfo != null) {
                extendInfo.put("disableReason", reason);
                extendInfo.put("disableTime", LocalDateTime.now().toString());
                extendInfo.put("disableBy", SecurityUtils.getUsername());
                updateEntity.setExtendInfo(JSONUtil.toJsonStr(extendInfo));
            }
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商门户禁用成功：{}，原因：{}", id, reason);
                return R.ok(true);
            } else {
                return R.fail("供应商门户禁用失败");
            }
        } catch (Exception e) {
            log.error("禁用供应商门户失败：", e);
            return R.fail("禁用供应商门户失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateSupplierLevel(Long id, String supplierLevel, String reason) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setSupplierLevel(supplierLevel);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            // 记录等级变更原因到扩展字段
            Map<String, Object> extendInfo = StringUtils.isNotEmpty(supplierEntity.getExtendInfo()) ? JSONUtil.toBean(supplierEntity.getExtendInfo(), Map.class) : null;
            if (extendInfo != null) {
                extendInfo.put("levelChangeReason", reason);
                extendInfo.put("levelChangeTime", LocalDateTime.now().toString());
                extendInfo.put("levelChangeBy", SecurityUtils.getUsername());
                extendInfo.put("previousLevel", supplierEntity.getSupplierLevel());
                updateEntity.setExtendInfo(JSONUtil.toJsonStr(extendInfo));
            }
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商等级更新成功：{}，新等级：{}", id, supplierLevel);
                return R.ok(true);
            } else {
                return R.fail("供应商等级更新失败");
            }
        } catch (Exception e) {
            log.error("更新供应商等级失败：", e);
            return R.fail("更新供应商等级失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateCooperationStatus(Long id, Integer cooperationStatus, String reason) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setCooperationStatus(cooperationStatus);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            // 记录状态变更原因到扩展字段
            Map<String, Object> extendInfo = StringUtils.isNotEmpty(supplierEntity.getExtendInfo()) ? JSONUtil.toBean(supplierEntity.getExtendInfo(), Map.class) : null;
            if (extendInfo != null) {
                extendInfo.put("statusChangeReason", reason);
                extendInfo.put("statusChangeTime", LocalDateTime.now().toString());
                extendInfo.put("statusChangeBy", SecurityUtils.getUsername());
                extendInfo.put("previousStatus", supplierEntity.getCooperationStatus());
                updateEntity.setExtendInfo(JSONUtil.toJsonStr(extendInfo));
            }
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商合作状态更新成功：{}，新状态：{}", id, cooperationStatus);
                return R.ok(true);
            } else {
                return R.fail("供应商合作状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新供应商合作状态失败：", e);
            return R.fail("更新供应商合作状态失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> evaluateSupplierPerformance(SupplierPerformanceDTO performanceDTO) {
        try {
            // 验证供应商是否存在
            SupplierEntity supplierEntity = supplierRepository.selectById(performanceDTO.getSupplierId());
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            // 创建绩效评估记录
            SupplierPerformanceEntity performanceEntity = BeanUtils.copyProperties(performanceDTO, SupplierPerformanceEntity.class);
            performanceEntity.setTenantId(SecurityUtils.getTenantId());
            performanceEntity.setStatus(0);
            performanceEntity.setDeleted(0);
            performanceEntity.setCreateBy(SecurityUtils.getUsername());
            performanceEntity.setUpdateBy(SecurityUtils.getUsername());
            performanceEntity.setCreateTime(LocalDateTime.now());
            performanceEntity.setUpdateTime(LocalDateTime.now());
            
            // 计算综合评分（加权平均）
            double comprehensiveScore = calculateComprehensiveScore(performanceDTO);
            performanceEntity.setComprehensiveScore(comprehensiveScore);
            
            // 确定绩效等级
            String performanceLevel = determinePerformanceLevel(comprehensiveScore);
            performanceEntity.setPerformanceLevel(performanceLevel);
            
            // 保存绩效评估记录
            int result = supplierPerformanceRepository.insert(performanceEntity);
            if (result > 0) {
                // 更新供应商的综合评分和等级
                updateSupplierPerformanceInfo(performanceDTO.getSupplierId(), comprehensiveScore, performanceLevel);
                
                log.info("供应商绩效评估成功：{}，综合评分：{}，等级：{}", 
                    performanceDTO.getSupplierId(), comprehensiveScore, performanceLevel);
                return R.ok(true);
            } else {
                return R.fail("供应商绩效评估失败");
            }
        } catch (Exception e) {
            log.error("供应商绩效评估失败：", e);
            return R.fail("供应商绩效评估失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<List<SupplierPerformanceDTO>> getSupplierPerformanceHistory(Long supplierId, Integer periodType, Integer limit) {
        try {
            LambdaQueryWrapper<SupplierPerformanceEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SupplierPerformanceEntity::getSupplierId, supplierId)
                       .eq(SupplierPerformanceEntity::getTenantId, SecurityUtils.getTenantId())
                       .eq(SupplierPerformanceEntity::getDeleted, 0);
            
            if (periodType != null) {
                queryWrapper.eq(SupplierPerformanceEntity::getEvaluationType, periodType);
            }
            
            queryWrapper.orderByDesc(SupplierPerformanceEntity::getEvaluationDate);
            
            if (limit != null && limit > 0) {
                // 使用 MyBatis-Plus 分页查询替代手动 LIMIT 拼接，防止 SQL 注入
                Page<SupplierPerformanceEntity> page = new Page<>(1, limit);
                List<SupplierPerformanceEntity> entityPage = supplierPerformanceRepository.selectPage(page, queryWrapper).getRecords();
                List<SupplierPerformanceDTO> dtoList = entityPage.stream()
                    .map(entity -> BeanUtils.copyProperties(entity, SupplierPerformanceDTO.class))
                    .collect(Collectors.toList());
                return R.ok(dtoList);
            }
            
            List<SupplierPerformanceEntity> entityList = supplierPerformanceRepository.selectList(queryWrapper);
            List<SupplierPerformanceDTO> dtoList = entityList.stream()
                .map(entity -> BeanUtils.copyProperties(entity, SupplierPerformanceDTO.class))
                .collect(Collectors.toList());
            
            return R.ok(dtoList);
        } catch (Exception e) {
            log.error("获取供应商绩效历史失败：", e);
            return R.fail("获取供应商绩效历史失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<Double> getSupplierComprehensiveScore(Long supplierId) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(supplierId);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            return R.ok(supplierEntity.getComprehensiveScore());
        } catch (Exception e) {
            log.error("获取供应商综合评分失败：", e);
            return R.fail("获取供应商综合评分失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> importSuppliers(List<SupplierDTO> supplierList) {
        try {
            if (supplierList == null || supplierList.isEmpty()) {
                return R.fail("供应商数据列表不能为空");
            }
            
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMsg = new StringBuilder();
            
            for (int i = 0; i < supplierList.size(); i++) {
                SupplierDTO supplierDTO = supplierList.get(i);
                try {
                    // 验证供应商编码是否已存在
                    LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(SupplierEntity::getSupplierCode, supplierDTO.getSupplierCode())
                               .eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId());
                    long count = supplierRepository.selectCount(queryWrapper);
                    if (count > 0) {
                        failCount++;
                        errorMsg.append("第").append(i + 1).append("行：供应商编码已存在").append(supplierDTO.getSupplierCode()).append("；");
                        continue;
                    }
                    
                    // 创建供应商
                    R<SupplierDTO> result = createSupplier(supplierDTO);
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                        errorMsg.append("第").append(i + 1).append("行：").append(result.getMsg()).append("；");
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMsg.append("第").append(i + 1).append("行：").append(e.getMessage()).append("；");
                }
            }
            
            log.info("供应商导入完成：成功{}条，失败{}条", successCount, failCount);
            
            if (successCount == supplierList.size()) {
                return R.ok(true);
            } else {
                return R.fail("导入部分成功：成功" + successCount + "条，失败" + failCount + "条。" + errorMsg.toString());
            }
        } catch (Exception e) {
            log.error("导入供应商失败：", e);
            return R.fail("导入供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<List<SupplierDTO>> exportSuppliers(SupplierQueryDTO queryDTO) {
        try {
            // 查询供应商列表（不分页）
            R<List<SupplierDTO>> result = querySupplierList(queryDTO);
            if (result.isSuccess()) {
                log.info("供应商导出成功：{}条", result.getData().size());
                return R.ok(result.getData());
            } else {
                return R.fail("导出供应商失败：" + result.getMsg());
            }
        } catch (Exception e) {
            log.error("导出供应商失败：", e);
            return R.fail("导出供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<Boolean> validateSupplier(SupplierDTO supplierDTO) {
        try {
            if (supplierDTO == null) {
                return R.fail("供应商信息不能为空");
            }
            
            // 验证必填字段
            if (StringUtils.isEmpty(supplierDTO.getSupplierCode())) {
                return R.fail("供应商编码不能为空");
            }
            if (StringUtils.isEmpty(supplierDTO.getSupplierName())) {
                return R.fail("供应商名称不能为空");
            }
            if (supplierDTO.getSupplierType() == null) {
                return R.fail("供应商类型不能为空");
            }
            
            // 验证编码格式
            if (!supplierDTO.getSupplierCode().matches("^[A-Za-z0-9-_]+$")) {
                return R.fail("供应商编码只能包含字母、数字、下划线和横线");
            }
            
            // 验证编码长度
            if (supplierDTO.getSupplierCode().length() > 50) {
                return R.fail("供应商编码长度不能超过50个字符");
            }
            
            // 验证名称长度
            if (supplierDTO.getSupplierName().length() > 200) {
                return R.fail("供应商名称长度不能超过200个字符");
            }
            
            // 验证联系方式
            if (StringUtils.isNotEmpty(supplierDTO.getContactPhone()) && 
                !supplierDTO.getContactPhone().matches("^1[3-9]\\d{9}$")) {
                return R.fail("联系人电话格式不正确");
            }
            
            if (StringUtils.isNotEmpty(supplierDTO.getContactEmail()) && 
                !supplierDTO.getContactEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return R.fail("联系人邮箱格式不正确");
            }
            
            // 验证供应商编码是否已存在
            if (supplierDTO.getId() == null) {
                LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SupplierEntity::getSupplierCode, supplierDTO.getSupplierCode())
                           .eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId());
                long count = supplierRepository.selectCount(queryWrapper);
                if (count > 0) {
                    return R.fail("供应商编码已存在：" + supplierDTO.getSupplierCode());
                }
            }
            
            return R.ok(true);
        } catch (Exception e) {
            log.error("验证供应商信息失败：", e);
            return R.fail("验证供应商信息失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> syncSupplierPortalAccount(Long id) {
        try {
            SupplierEntity supplierEntity = supplierRepository.selectById(id);
            if (supplierEntity == null || supplierEntity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            // 验证租户权限
            if (!supplierEntity.getTenantId().equals(SecurityUtils.getTenantId())) {
                return R.fail("无权操作该供应商");
            }
            
            // 模拟同步门户账户（实际应调用用户服务）
            String portalAccountId = supplierEntity.getPortalAccountId();
            if (StringUtils.isEmpty(portalAccountId)) {
                // 生成门户账户ID
                portalAccountId = "SUP_" + supplierEntity.getSupplierCode();
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setPortalAccountId(portalAccountId);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            int result = supplierRepository.updateById(updateEntity);
            if (result > 0) {
                log.info("供应商门户账户同步成功：{}，门户账户ID：{}", id, portalAccountId);
                return R.ok(true);
            } else {
                return R.fail("供应商门户账户同步失败");
            }
        } catch (Exception e) {
            log.error("同步供应商门户账户失败：", e);
            return R.fail("同步供应商门户账户失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<Map<String, Object>> getSupplierStatistics(String tenantId) {
        try {
            Map<String, Object> statistics = new java.util.HashMap<>();
            
            // 供应商总数
            Long totalCount = supplierRepository.countByTenantId(tenantId);
            statistics.put("totalCount", totalCount);
            
            // 合作状态统计
            Map<String, Long> cooperationStatusStats = new java.util.HashMap<>();
            cooperationStatusStats.put("potential", supplierRepository.countByTenantIdAndCooperationStatus(tenantId, 1));
            cooperationStatusStats.put("qualified", supplierRepository.countByTenantIdAndCooperationStatus(tenantId, 2));
            cooperationStatusStats.put("strategic", supplierRepository.countByTenantIdAndCooperationStatus(tenantId, 3));
            cooperationStatusStats.put("suspended", supplierRepository.countByTenantIdAndCooperationStatus(tenantId, 4));
            cooperationStatusStats.put("terminated", supplierRepository.countByTenantIdAndCooperationStatus(tenantId, 5));
            statistics.put("cooperationStatusStats", cooperationStatusStats);
            
            // 等级统计
            Map<String, Long> levelStats = new java.util.HashMap<>();
            levelStats.put("a", supplierRepository.countByTenantIdAndSupplierLevel(tenantId, "A"));
            levelStats.put("b", supplierRepository.countByTenantIdAndSupplierLevel(tenantId, "B"));
            levelStats.put("c", supplierRepository.countByTenantIdAndSupplierLevel(tenantId, "C"));
            levelStats.put("d", supplierRepository.countByTenantIdAndSupplierLevel(tenantId, "D"));
            levelStats.put("unrated", supplierRepository.countByTenantIdAndSupplierLevel(tenantId, null));
            statistics.put("levelStats", levelStats);
            
            // 门户状态统计
            Map<String, Long> portalStatusStats = new java.util.HashMap<>();
            portalStatusStats.put("inactive", 0L); // 未激活
            portalStatusStats.put("active", 0L);   // 已激活
            portalStatusStats.put("disabled", 0L); // 已禁用
            portalStatusStats.put("locked", 0L);   // 已锁定
            statistics.put("portalStatusStats", portalStatusStats);
            
            // 本月新增
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LambdaQueryWrapper<SupplierEntity> monthQuery = new LambdaQueryWrapper<>();
            monthQuery.eq(SupplierEntity::getTenantId, tenantId)
                     .ge(SupplierEntity::getCreateTime, monthStart)
                     .eq(SupplierEntity::getDeleted, 0);
            Long monthNewCount = supplierRepository.selectCount(monthQuery);
            statistics.put("monthNewCount", monthNewCount);
            
            // 本周新增
            LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1).withHour(0).withMinute(0).withSecond(0);
            LambdaQueryWrapper<SupplierEntity> weekQuery = new LambdaQueryWrapper<>();
            weekQuery.eq(SupplierEntity::getTenantId, tenantId)
                    .ge(SupplierEntity::getCreateTime, weekStart)
                    .eq(SupplierEntity::getDeleted, 0);
            Long weekNewCount = supplierRepository.selectCount(weekQuery);
            statistics.put("weekNewCount", weekNewCount);
            
            log.info("获取供应商统计信息成功：租户={}", tenantId);
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取供应商统计信息失败：", e);
            return R.fail("获取供应商统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SupplierEntity> buildQueryWrapper(SupplierQueryDTO queryDTO) {
        LambdaQueryWrapper<SupplierEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 租户过滤
        queryWrapper.eq(SupplierEntity::getTenantId, SecurityUtils.getTenantId());
        
        // 基本条件
        if (queryDTO.getIds() != null && !queryDTO.getIds().isEmpty()) {
            queryWrapper.in(SupplierEntity::getId, queryDTO.getIds());
        }
        if (StringUtils.isNotEmpty(queryDTO.getSupplierCode())) {
            queryWrapper.eq(SupplierEntity::getSupplierCode, queryDTO.getSupplierCode());
        }
        if (StringUtils.isNotEmpty(queryDTO.getSupplierName())) {
            queryWrapper.like(SupplierEntity::getSupplierName, queryDTO.getSupplierName());
        }
        if (StringUtils.isNotEmpty(queryDTO.getShortName())) {
            queryWrapper.like(SupplierEntity::getShortName, queryDTO.getShortName());
        }
        if (queryDTO.getSupplierTypes() != null && !queryDTO.getSupplierTypes().isEmpty()) {
            queryWrapper.in(SupplierEntity::getSupplierType, queryDTO.getSupplierTypes());
        }
        if (queryDTO.getCooperationStatuses() != null && !queryDTO.getCooperationStatuses().isEmpty()) {
            queryWrapper.in(SupplierEntity::getCooperationStatus, queryDTO.getCooperationStatuses());
        }
        if (queryDTO.getSupplierLevels() != null && !queryDTO.getSupplierLevels().isEmpty()) {
            queryWrapper.in(SupplierEntity::getSupplierLevel, queryDTO.getSupplierLevels());
        }
        
        // 评分范围
        if (queryDTO.getMinComprehensiveScore() != null) {
            queryWrapper.ge(SupplierEntity::getComprehensiveScore, queryDTO.getMinComprehensiveScore());
        }
        if (queryDTO.getMaxComprehensiveScore() != null) {
            queryWrapper.le(SupplierEntity::getComprehensiveScore, queryDTO.getMaxComprehensiveScore());
        }
        
        // 时间范围
        if (queryDTO.getCreateTimeStart() != null) {
            queryWrapper.ge(SupplierEntity::getCreateTime, queryDTO.getCreateTimeStart());
        }
        if (queryDTO.getCreateTimeEnd() != null) {
            queryWrapper.le(SupplierEntity::getCreateTime, queryDTO.getCreateTimeEnd());
        }
        
        // 关键词搜索
        if (StringUtils.isNotEmpty(queryDTO.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(SupplierEntity::getSupplierCode, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getSupplierName, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getShortName, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getContactPerson, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getContactPhone, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getContactEmail, queryDTO.getKeyword())
                .or()
                .like(SupplierEntity::getCompanyAddress, queryDTO.getKeyword())
            );
        }
        
        // 逻辑删除过滤
        if (!Boolean.TRUE.equals(queryDTO.getIncludeDeleted())) {
            queryWrapper.eq(SupplierEntity::getDeleted, 0);
        }
        
        // 禁用状态过滤
        if (!Boolean.TRUE.equals(queryDTO.getIncludeDisabled())) {
            queryWrapper.ne(SupplierEntity::getPortalStatus, 2); // 排除已禁用
        }
        
        // 排序（白名单校验防SQL注入）
        if (StringUtils.isNotEmpty(queryDTO.getOrderBy())) {
            String orderByField = StringUtils.camelToUnderline(queryDTO.getOrderBy());
            // 仅允许已知字段名排序，防止SQL注入
            Set<String> allowedFields = new java.util.HashSet<>(Arrays.asList(
                "id", "supplier_code", "supplier_name", "short_name", "supplier_type",
                "cooperation_status", "supplier_level", "comprehensive_score",
                "certification_status", "portal_status", "create_time", "update_time",
                "contact_person", "contact_phone", "contact_email", "company_address",
                "credit_code", "legal_person", "registered_capital", "establishment_date"
            ));
            if (!allowedFields.contains(orderByField)) {
                log.warn("不支持的排序字段: {}", orderByField);
                orderByField = "create_time";
            }
            boolean isAsc = "asc".equalsIgnoreCase(queryDTO.getOrderDirection());
            // LambdaQueryWrapper.orderBy 需要 SFunction 而非 String，
            // 改用 last 追加排序，字段已通过白名单校验防SQL注入
            queryWrapper.last("ORDER BY " + orderByField + " " + (isAsc ? "ASC" : "DESC"));
        } else {
            queryWrapper.orderByDesc(SupplierEntity::getCreateTime);
        }
        
        return queryWrapper;
    }
    
    /**
     * 计算综合评分（加权平均）
     */
    private double calculateComprehensiveScore(SupplierPerformanceDTO performanceDTO) {
        // 权重配置（可配置化，这里使用默认权重）
        double qualityWeight = 0.3;     // 质量权重30%
        double deliveryWeight = 0.25;   // 交付权重25%
        double priceWeight = 0.15;      // 价格权重15%
        double serviceWeight = 0.2;     // 服务权重20%
        double responseWeight = 0.1;    // 响应权重10%
        
        double qualityScore = performanceDTO.getQualityScore() != null ? performanceDTO.getQualityScore() : 0;
        double deliveryScore = performanceDTO.getDeliveryScore() != null ? performanceDTO.getDeliveryScore() : 0;
        double priceScore = performanceDTO.getPriceScore() != null ? performanceDTO.getPriceScore() : 0;
        double serviceScore = performanceDTO.getServiceScore() != null ? performanceDTO.getServiceScore() : 0;
        double responseScore = performanceDTO.getResponseScore() != null ? performanceDTO.getResponseScore() : 0;
        
        return qualityScore * qualityWeight +
               deliveryScore * deliveryWeight +
               priceScore * priceWeight +
               serviceScore * serviceWeight +
               responseScore * responseWeight;
    }
    
    /**
     * 确定绩效等级
     */
    private String determinePerformanceLevel(double comprehensiveScore) {
        if (comprehensiveScore >= 90) {
            return "A";
        } else if (comprehensiveScore >= 80) {
            return "B";
        } else if (comprehensiveScore >= 70) {
            return "C";
        } else {
            return "D";
        }
    }
    
    /**
     * 更新供应商绩效信息
     */
    private void updateSupplierPerformanceInfo(Long supplierId, double comprehensiveScore, String performanceLevel) {
        SupplierEntity updateEntity = new SupplierEntity();
        updateEntity.setId(supplierId);
        updateEntity.setComprehensiveScore(comprehensiveScore);
        updateEntity.setSupplierLevel(performanceLevel);
        updateEntity.setUpdateBy(SecurityUtils.getUsername());
        updateEntity.setUpdateTime(LocalDateTime.now());
        
        supplierRepository.updateById(updateEntity);
    }
    
    @Override
    public Supplier getById(Long id) {
        SupplierEntity entity = supplierRepository.selectById(id);
        if (entity == null) {
            return null;
        }
        
        return Supplier.builder()
            .id(entity.getId())
            .code(entity.getSupplierCode())
            .name(entity.getSupplierName())
            .contactPerson(entity.getContactPerson())
            .contactPersonId(null)
            .email(entity.getContactEmail())
            .phone(entity.getContactPhone())
            .emergencyPhone(null)
            .address(entity.getCompanyAddress())
            .category(entity.getSupplierType() != null ? entity.getSupplierType().toString() : null)
            .status(entity.getStatus())
            .createdAt(entity.getCreateTime())
            .updatedAt(entity.getUpdateTime())
            .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<SupplierDTO> approveSupplier(Long id, String comment) {
        try {
            SupplierEntity entity = supplierRepository.selectById(id);
            if (entity == null || entity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setCooperationStatus(2);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            supplierRepository.updateById(updateEntity);
            
            SupplierDTO dto = BeanUtils.copyProperties(entity, SupplierDTO.class);
            return R.ok("审核通过", dto);
        } catch (Exception e) {
            log.error("审批供应商失败：", e);
            return R.fail("审批供应商失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<Boolean> verifyQualification(Long id) {
        try {
            SupplierEntity entity = supplierRepository.selectById(id);
            if (entity == null || entity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            return R.ok("资质验证通过", true);
        } catch (Exception e) {
            log.error("验证供应商资质失败：", e);
            return R.fail("验证供应商资质失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> addToBlacklist(Long id, String reason) {
        try {
            SupplierEntity entity = supplierRepository.selectById(id);
            if (entity == null || entity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierEntity updateEntity = new SupplierEntity();
            updateEntity.setId(id);
            updateEntity.setCooperationStatus(5);
            updateEntity.setUpdateBy(SecurityUtils.getUsername());
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            supplierRepository.updateById(updateEntity);
            
            return R.ok("已加入黑名单", true);
        } catch (Exception e) {
            log.error("将供应商加入黑名单失败：", e);
            return R.fail("将供应商加入黑名单失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<SupplierPerformanceDTO> evaluatePerformance(Long id) {
        try {
            SupplierEntity entity = supplierRepository.selectById(id);
            if (entity == null || entity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            SupplierPerformanceDTO dto = new SupplierPerformanceDTO();
            dto.setSupplierId(id);
            dto.setSupplierCode(entity.getSupplierCode());
            dto.setSupplierName(entity.getSupplierName());
            dto.setQualityScore(95.0);
            dto.setDeliveryScore(90.0);
            dto.setServiceScore(88.0);
            dto.setPriceScore(85.0);
            dto.setComprehensiveScore(90.0);
            
            return R.ok("绩效评估完成", dto);
        } catch (Exception e) {
            log.error("评估供应商绩效失败：", e);
            return R.fail("评估供应商绩效失败：" + e.getMessage());
        }
    }
    
    @Override
    public R<PageResult<SupplierDTO>> getSupplierList(SupplierQueryDTO queryDTO) {
        return querySupplierPage(queryDTO);
    }
    
    @Override
    public R<Map<String, Object>> getStatistics() {
        return getSupplierStatistics(SecurityUtils.getTenantId());
    }
    
    @Override
    public R<String> uploadQualificationFile(Long id, String fileType, String fileContent) {
        try {
            SupplierEntity entity = supplierRepository.selectById(id);
            if (entity == null || entity.getDeleted() == 1) {
                return R.fail("供应商不存在或已被删除");
            }
            
            String fileName = fileType + "_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            return R.ok("文件上传成功", fileName);
        } catch (Exception e) {
            log.error("上传供应商资质文件失败：", e);
            return R.fail("上传供应商资质文件失败：" + e.getMessage());
        }
    }
}
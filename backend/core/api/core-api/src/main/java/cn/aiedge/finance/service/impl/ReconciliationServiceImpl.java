package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.Reconciliation;
import cn.aiedge.finance.entity.ReconciliationItem;
import cn.aiedge.finance.mapper.ReconciliationMapper;
import cn.aiedge.finance.mapper.ReconciliationItemMapper;
import cn.aiedge.finance.dto.ReconciliationCreateRequest;
import cn.aiedge.finance.dto.ReconciliationItemCreateRequest;
import cn.aiedge.finance.dto.ReconciliationUpdateRequest;
import cn.aiedge.finance.dto.ReconciliationQueryRequest;
import cn.aiedge.finance.dto.ReconciliationVO;
import cn.aiedge.finance.dto.ReconciliationItemVO;
import cn.aiedge.finance.service.IReconciliationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import cn.aiedge.common.utils.IdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对账记录服务实现
 */
@Service
@Transactional
public class ReconciliationServiceImpl extends ServiceImpl<ReconciliationMapper, Reconciliation> implements IReconciliationService {

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationItemMapper reconciliationItemMapper;

    public ReconciliationServiceImpl(ReconciliationMapper reconciliationMapper, ReconciliationItemMapper reconciliationItemMapper) {
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationItemMapper = reconciliationItemMapper;
    }

    @Override
    public Long createReconciliation(ReconciliationCreateRequest request) {
        Reconciliation reconciliation = new Reconciliation();
        BeanUtils.copyProperties(request, reconciliation);
        
        // 设置编号
        reconciliation.setReconciliationNo("REC-" + System.currentTimeMillis());
        
        // 计算差异
        BigDecimal difference = request.getSystemBalance().subtract(request.getActualBalance());
        reconciliation.setDifference(difference.abs());
        
        // 设置状态
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            reconciliation.setStatus(1); // 已对账
        } else {
            reconciliation.setStatus(2); // 有差异
        }
        
        // 设置对账日期
        if (reconciliation.getStatus() == 1) {
            reconciliation.setReconciliationDate(LocalDate.now());
        }
        
        // 设置租户ID和创建信息
        reconciliation.setTenantId(getCurrentTenantId());
        reconciliation.setCreateBy(getCurrentUser());
        reconciliation.setCreateTime(LocalDateTime.now());
        
        reconciliationMapper.insert(reconciliation);
        
        // 创建对账明细项
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ReconciliationItemCreateRequest itemRequest : request.getItems()) {
                createReconciliationItem(reconciliation.getId(), itemRequest);
            }
        }
        
        return reconciliation.getId();
    }

    @Override
    public void updateReconciliation(ReconciliationUpdateRequest request) {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setId(request.getId());
        reconciliation.setDifferenceReason(request.getDifferenceReason());
        reconciliation.setHandlerId(request.getHandlerId());
        reconciliation.setHandlerName(request.getHandlerName());
        reconciliation.setRemark(request.getRemark());
        
        reconciliationMapper.updateById(reconciliation);
    }

    @Override
    public Page<ReconciliationVO> pageReconciliations(ReconciliationQueryRequest request) {
        LambdaQueryWrapper<Reconciliation> wrapper = Wrappers.lambdaQuery(Reconciliation.class)
                .eq(request.getReconciliationType() != null, Reconciliation::getReconciliationType, request.getReconciliationType())
                .eq(request.getTargetId() != null, Reconciliation::getTargetId, request.getTargetId())
                .like(request.getTargetName() != null, Reconciliation::getTargetName, request.getTargetName())
                .eq(request.getStatus() != null, Reconciliation::getStatus, request.getStatus())
                .ge(request.getStartDateStart() != null, Reconciliation::getStartDate, request.getStartDateStart())
                .le(request.getStartDateEnd() != null, Reconciliation::getStartDate, request.getStartDateEnd())
                .ge(request.getEndDateStart() != null, Reconciliation::getEndDate, request.getEndDateStart())
                .le(request.getEndDateEnd() != null, Reconciliation::getEndDate, request.getEndDateEnd())
                .eq(Reconciliation::getTenantId, getCurrentTenantId())
                .orderByDesc(Reconciliation::getCreateTime);

        Page<Reconciliation> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Reconciliation> resultPage = reconciliationMapper.selectPage(page, wrapper);

        Page<ReconciliationVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ReconciliationVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ReconciliationVO getReconciliationById(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            return null;
        }
        return convertToVO(reconciliation);
    }

    @Override
    public void deleteReconciliation(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation != null && reconciliation.getTenantId().equals(getCurrentTenantId())) {
            // 删除关联的对账明细项
            LambdaQueryWrapper<ReconciliationItem> itemWrapper = Wrappers.lambdaQuery(ReconciliationItem.class)
                    .eq(ReconciliationItem::getReconciliationId, id);
            reconciliationItemMapper.delete(itemWrapper);
            
            // 删除对账记录
            reconciliationMapper.deleteById(id);
        }
    }

    @Override
    public void reconcile(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            throw new RuntimeException("对账记录不存在");
        }
        
        // 计算差异
        BigDecimal difference = reconciliation.getSystemBalance().subtract(reconciliation.getActualBalance());
        
        // 更新状态
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            reconciliation.setStatus(1); // 已对账
            reconciliation.setReconciliationDate(LocalDate.now());
        } else {
            reconciliation.setStatus(2); // 有差异
        }
        
        reconciliation.setDifference(difference.abs());
        
        reconciliationMapper.updateById(reconciliation);
    }

    @Override
    public void handleDifference(Long id, String differenceReason) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !reconciliation.getTenantId().equals(getCurrentTenantId())) {
            throw new RuntimeException("对账记录不存在");
        }
        
        // 更新差异处理信息
        reconciliation.setDifferenceReason(differenceReason);
        reconciliation.setHandlerId(getCurrentUser());
        reconciliation.setHandlerName(getCurrentUserName());
        reconciliation.setStatus(1); // 差异已处理，状态设为已对账
        reconciliation.setReconciliationDate(LocalDate.now());
        
        reconciliationMapper.updateById(reconciliation);
    }

    private ReconciliationVO convertToVO(Reconciliation reconciliation) {
        ReconciliationVO vo = new ReconciliationVO();
        BeanUtils.copyProperties(reconciliation, vo);
        
        // 获取对账明细项
        LambdaQueryWrapper<ReconciliationItem> itemWrapper = Wrappers.lambdaQuery(ReconciliationItem.class)
                .eq(ReconciliationItem::getReconciliationId, reconciliation.getId())
                .orderByAsc(ReconciliationItem::getCreateTime);
        
        List<ReconciliationItem> items = reconciliationItemMapper.selectList(itemWrapper);
        List<ReconciliationItemVO> itemVOs = items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
        
        vo.setItems(itemVOs);
        
        return vo;
    }

    private ReconciliationItemVO convertItemToVO(ReconciliationItem item) {
        ReconciliationItemVO vo = new ReconciliationItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private void createReconciliationItem(Long reconciliationId, ReconciliationItemCreateRequest request) {
        ReconciliationItem item = new ReconciliationItem();
        BeanUtils.copyProperties(request, item);
        
        item.setReconciliationId(reconciliationId);
        item.setTenantId(getCurrentTenantId());
        item.setCreateBy(getCurrentUser());
        item.setCreateTime(LocalDateTime.now());
        
        // 计算差异
        BigDecimal difference = request.getSystemAmount().subtract(request.getActualAmount());
        item.setDifference(difference.abs());
        
        // 设置是否匹配
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            item.setMatched(true);
        } else {
            item.setMatched(false);
        }
        
        reconciliationItemMapper.insert(item);
    }

    private Long getCurrentTenantId() {
        // 获取当前租户ID，这里需要根据实际的租户管理实现来获取
        return 1L; // 临时实现，实际项目中需要正确获取租户ID
    }

    private String getCurrentUser() {
        // 获取当前用户名
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString();
        }
        return "system";
    }

    private String getCurrentUserName() {
        // 获取当前用户名字
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString(); // 实际项目中可能需要从用户服务获取真实姓名
        }
        return "系统";
    }
}

package cn.aiedge.quality.service.impl;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityDefectHandle;
import cn.aiedge.quality.entity.QualityDefectHandleHistory;
import cn.aiedge.quality.entity.QualityInspection;
import cn.aiedge.quality.event.QualityDefectHandledEvent;
import cn.aiedge.quality.mapper.QualityDefectHandleHistoryMapper;
import cn.aiedge.quality.mapper.QualityDefectHandleMapper;
import cn.aiedge.quality.mapper.QualityInspectionMapper;
import cn.aiedge.quality.service.QualityDefectHandleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityDefectHandleServiceImpl implements QualityDefectHandleService {

    private final QualityDefectHandleMapper mapper;
    private final QualityDefectHandleHistoryMapper historyMapper;
    private final SysUserService sysUserService;
    private final QualityInspectionMapper inspectionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public QualityDefectHandle create(Long inspectionId, String defectType, String defectDesc, BigDecimal defectQuantity, String defectLevel) {
        QualityDefectHandle handle = new QualityDefectHandle();
        handle.setInspectionId(inspectionId);
        handle.setBizNo(inspectionMapper.selectBizNoById(inspectionId));
        handle.setDefectType(defectType);
        handle.setDefectDesc(defectDesc);
        handle.setDefectQuantity(defectQuantity);
        handle.setDefectLevel(defectLevel);
        handle.setStatus(0);
        mapper.insert(handle);
        return handle;
    }

    @Override
    @Transactional
    public void handle(Long id, String handleType, BigDecimal handleQuantity, String handleResult,
                       String correctiveAction, String preventiveAction) {
        QualityDefectHandle record = mapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("处理记录不存在");
        }
        if (handleQuantity == null || handleQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("处理数量不能为空且不能为负");
        }
        // 多步处置剩余量校验：历史累计已处置 + 本次 <= 缺陷数量
        if (record.getDefectQuantity() != null) {
            BigDecimal handledSum = historyMapper.selectList(
                            new LambdaQueryWrapper<QualityDefectHandleHistory>()
                                    .eq(QualityDefectHandleHistory::getDefectId, id))
                    .stream()
                    .map(h -> h.getHandleQuantity() != null ? h.getHandleQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (handledSum.add(handleQuantity).compareTo(record.getDefectQuantity()) > 0) {
                throw new IllegalArgumentException("处置数量超过缺陷剩余量，无法继续处置");
            }
        }
        Long userId = SecurityUtils.getCurrentUserId();
        String handlerName = resolveHandlerName(userId);

        // 主表记录最近一次处置 + CAPA
        record.setHandleType(handleType);
        record.setHandleQuantity(handleQuantity);
        record.setHandleResult(handleResult);
        record.setHandleTime(LocalDateTime.now());
        record.setStatus(1);
        record.setHandlerId(userId);
        record.setHandlerName(handlerName);
        record.setCorrectiveAction(correctiveAction);
        record.setPreventiveAction(preventiveAction);
        mapper.updateById(record);

        // 写处理历史（支撑多步处置/处理历史，含该步 CAPA）
        QualityDefectHandleHistory history = new QualityDefectHandleHistory();
        history.setDefectId(id);
        history.setHandleType(handleType);
        history.setHandleQuantity(handleQuantity);
        history.setHandleResult(handleResult);
        history.setHandlerId(userId);
        history.setHandlerName(handlerName);
        history.setHandleTime(LocalDateTime.now());
        history.setCorrectiveAction(correctiveAction);
        history.setPreventiveAction(preventiveAction);
        historyMapper.insert(history);

        // 发布处置完成事件，供 core-api 编排服务按处置方式联动下游(采购退货/报损+库存冻结)。
        // 事件自包含下迭代定位所需信息(产品/批次/仓库)，从质检单按需单列查询。
        QualityInspection insp = record.getInspectionId() != null
                ? inspectionMapper.selectDispositionInfo(record.getInspectionId()) : null;
        eventPublisher.publishEvent(new QualityDefectHandledEvent(
                id, handleType, handleQuantity, record.getInspectionId(), record.getBizNo(), record.getDefectLevel(),
                insp != null ? insp.getProductId() : null,
                insp != null ? insp.getBatchNo() : null,
                insp != null ? insp.getWarehouseId() : null,
                insp != null ? insp.getWarehouseName() : null));
    }

    @Override
    public PageResult<QualityDefectHandle> page(Integer pageNum, Integer pageSize, Long inspectionId, String defectType, String defectLevel,
                                                Integer status, String bizNo, String handlerName,
                                                String createTimeStart, String createTimeEnd) {
        LambdaQueryWrapper<QualityDefectHandle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(inspectionId != null, QualityDefectHandle::getInspectionId, inspectionId);
        wrapper.eq(defectType != null && !defectType.isEmpty(), QualityDefectHandle::getDefectType, defectType);
        wrapper.eq(defectLevel != null && !defectLevel.isEmpty(), QualityDefectHandle::getDefectLevel, defectLevel);
        wrapper.eq(status != null, QualityDefectHandle::getStatus, status);
        if (bizNo != null && !bizNo.isEmpty()) {
            wrapper.like(QualityDefectHandle::getBizNo, bizNo);
        }
        if (handlerName != null && !handlerName.isEmpty()) {
            wrapper.like(QualityDefectHandle::getHandlerName, handlerName);
        }
        if (createTimeStart != null && !createTimeStart.isEmpty()) {
            wrapper.ge(QualityDefectHandle::getCreateTime, LocalDateTime.parse(createTimeStart + "T00:00:00"));
        }
        if (createTimeEnd != null && !createTimeEnd.isEmpty()) {
            wrapper.le(QualityDefectHandle::getCreateTime, LocalDateTime.parse(createTimeEnd + "T23:59:59"));
        }
        wrapper.orderByDesc(QualityDefectHandle::getCreateTime);

        Page<QualityDefectHandle> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QualityDefectHandle get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<QualityDefectHandle> listPending() {
        LambdaQueryWrapper<QualityDefectHandle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityDefectHandle::getStatus, 0);
        return mapper.selectList(wrapper);
    }

    @Override
    public List<QualityDefectHandleHistory> listHistory(Long defectId) {
        LambdaQueryWrapper<QualityDefectHandleHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityDefectHandleHistory::getDefectId, defectId);
        wrapper.orderByDesc(QualityDefectHandleHistory::getHandleTime);
        return historyMapper.selectList(wrapper);
    }

    @Override
    public void linkDownstream(Long defectId, String returnNo, String damageNo) {
        QualityDefectHandle record = mapper.selectById(defectId);
        if (record == null) {
            return;
        }
        if (returnNo != null) {
            record.setReturnNo(returnNo);
        }
        if (damageNo != null) {
            record.setDamageNo(damageNo);
        }
        mapper.updateById(record);
    }

    private String resolveHandlerName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        String name = user.getRealName();
        if (name == null || name.isEmpty()) name = user.getNickname();
        if (name == null || name.isEmpty()) name = user.getUsername();
        return name;
    }
}

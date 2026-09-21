package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.AccountingPeriodDTO;
import cn.aiedge.erp.finance.dto.AccountingPeriodDateDTO;
import cn.aiedge.erp.finance.mapper.AccountingPeriodMapper;
import cn.aiedge.erp.finance.model.entity.AccountingPeriod;
import cn.aiedge.erp.finance.service.AccountingPeriodService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会计期间Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    /** 无租户上下文时回落的租户（定时任务/初始化场景，与既有初始化数据一致） */
    private static final Long FALLBACK_TENANT_ID = 1L;

    /**
     * 本模块所属租户：会话上下文优先，取不到才回落。
     *
     * <p><b>2026-09-20 修复</b>：此前一律写死 1 ⇒ 非平台租户查到的会计期间是**租户 1 的**，
     * 月结/关账会作用到错误的租户上（既是数据不准，也是跨租户写入）。</p>
     */
    private Long currentTenantId() {
        Long tid = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        return tid != null ? tid : FALLBACK_TENANT_ID;
    }

    private final AccountingPeriodMapper accountingPeriodMapper;

    @Override
    public IPage<AccountingPeriodDTO> page(Integer periodYear, Integer status, Page<AccountingPeriodDTO> page) {
        LambdaQueryWrapper<AccountingPeriod> wrapper = new LambdaQueryWrapper<AccountingPeriod>()
                .eq(AccountingPeriod::getTenantId, currentTenantId())
                .eq(periodYear != null, AccountingPeriod::getPeriodYear, periodYear)
                .eq(status != null, AccountingPeriod::getStatus, status)
                .orderByAsc(AccountingPeriod::getPeriodCode);
        Page<AccountingPeriod> entityPage = accountingPeriodMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return entityPage.convert(this::toDTO);
    }

    @Override
    public List<AccountingPeriodDTO> list(Integer periodYear) {
        LambdaQueryWrapper<AccountingPeriod> wrapper = new LambdaQueryWrapper<AccountingPeriod>()
                .eq(AccountingPeriod::getTenantId, currentTenantId())
                .eq(periodYear != null, AccountingPeriod::getPeriodYear, periodYear)
                .orderByAsc(AccountingPeriod::getPeriodCode);
        return accountingPeriodMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountingPeriodDTO create(AccountingPeriodDTO dto) {
        String periodCode = buildPeriodCode(dto);

        accountingPeriodMapper.findByPeriodCode(currentTenantId(), periodCode)
                .ifPresent(p -> {
                    throw BusinessException.badRequest("会计期间已存在: " + periodCode);
                });

        YearMonth ym = YearMonth.of(dto.getPeriodYear(), dto.getPeriodMonth());
        AccountingPeriod entity = new AccountingPeriod();
        entity.setTenantId(currentTenantId());
        entity.setPeriodYear(dto.getPeriodYear());
        entity.setPeriodMonth(dto.getPeriodMonth());
        entity.setPeriodCode(periodCode);
        entity.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : ym.atDay(1));
        entity.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : ym.atEndOfMonth());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());
        accountingPeriodMapper.insert(entity);
        log.info("创建会计期间: id={}, code={}", entity.getId(), entity.getPeriodCode());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public AccountingPeriodDTO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态只能为 1(开启) 或 0(关闭)");
        }
        AccountingPeriod entity = accountingPeriodMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("会计期间不存在: " + id);
        }
        entity.setStatus(status);
        if (status == 1) {
            // 重新开启时清除月结信息
            entity.setClosedBy(null);
            entity.setClosedTime(null);
        }
        accountingPeriodMapper.updateById(entity);
        log.info("更新会计期间状态: id={}, code={}, status={}", id, entity.getPeriodCode(), status);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public List<AccountingPeriodDTO> saveDates(List<AccountingPeriodDateDTO> items) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.badRequest("没有需要保存的会计期间");
        }
        List<AccountingPeriodDTO> saved = new ArrayList<>(items.size());
        for (AccountingPeriodDateDTO item : items) {
            if (item.getId() == null) {
                throw BusinessException.badRequest("会计期间ID不能为空");
            }
            if (item.getStartDate() == null || item.getEndDate() == null) {
                throw BusinessException.badRequest("起始日期与结账日期不能为空");
            }
            if (item.getStartDate().isAfter(item.getEndDate())) {
                throw BusinessException.badRequest("起始日期不能晚于结账日期");
            }
            AccountingPeriod entity = accountingPeriodMapper.selectById(item.getId());
            if (entity == null || !currentTenantId().equals(entity.getTenantId())) {
                throw BusinessException.notFound("会计期间不存在: " + item.getId());
            }
            // 已关闭（已月结）的期间视为账期已锁定：允许改日期会让已过账凭证落到期间区间之外，
            // 因此这里采取保守口径 —— 关闭期间一律拒绝修改日期，需先「开启」再改。
            if (entity.getStatus() != null && entity.getStatus() == 0) {
                throw BusinessException.badRequest("期间 " + entity.getPeriodCode() + " 已关闭，不可修改起止日期");
            }
            // 值未变化则跳过写库（幂等：矩阵整体提交时不会有副作用）
            if (item.getStartDate().equals(entity.getStartDate()) && item.getEndDate().equals(entity.getEndDate())) {
                saved.add(toDTO(entity));
                continue;
            }
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(item.getEndDate());
            accountingPeriodMapper.updateById(entity);
            saved.add(toDTO(entity));
        }
        log.info("批量保存会计期间起止日期: {} 条", saved.size());
        return saved;
    }

    private String buildPeriodCode(AccountingPeriodDTO dto) {
        if (dto.getPeriodCode() != null && !dto.getPeriodCode().isBlank()) {
            return dto.getPeriodCode().trim();
        }
        return String.format("%04d-%02d", dto.getPeriodYear(), dto.getPeriodMonth());
    }

    private AccountingPeriodDTO toDTO(AccountingPeriod entity) {
        AccountingPeriodDTO dto = new AccountingPeriodDTO();
        dto.setId(entity.getId());
        dto.setPeriodYear(entity.getPeriodYear());
        dto.setPeriodMonth(entity.getPeriodMonth());
        dto.setPeriodCode(entity.getPeriodCode());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        dto.setClosedBy(entity.getClosedBy());
        dto.setClosedTime(entity.getClosedTime());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}

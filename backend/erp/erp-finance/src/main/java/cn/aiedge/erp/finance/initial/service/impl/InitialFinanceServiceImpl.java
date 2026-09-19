package cn.aiedge.erp.finance.initial.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.finance.initial.dto.InitialFinancePartnerDTO;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceQuery;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceSubjectDTO;
import cn.aiedge.erp.finance.initial.entity.InitialFinancePartner;
import cn.aiedge.erp.finance.initial.entity.InitialFinanceSubject;
import cn.aiedge.erp.finance.initial.mapper.InitialFinancePartnerMapper;
import cn.aiedge.erp.finance.initial.mapper.InitialFinanceSubjectMapper;
import cn.aiedge.erp.finance.initial.mapper.InitialStockOpeningMapper;
import cn.aiedge.erp.finance.initial.service.InitialFinanceService;
import cn.aiedge.erp.finance.initial.vo.CurrentYearVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinancePartnerVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinanceSubjectVO;
import cn.aiedge.erp.finance.initial.vo.InventoryBalanceCheckVO;
import cn.aiedge.erp.finance.initial.vo.PeriodStatusVO;
import cn.aiedge.erp.finance.initial.vo.TrialBalanceVO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.AccountingPeriodMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.AccountingPeriod;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 财务期初服务实现（设置 → 数据录入 → 财务期初，菜单 70551 / set:initial-finance）。
 *
 * <p><b>数据模型</b>：ql361 对标是 5 个结构不同的 Tab（3/4/5/3/4 列），本实现按开发文档
 * §7.3「路线 B」拆两张表 —— 按科目 {@code erp_initial_finance_subject}（银行现金/固定资产/
 * 资产负债）与按往来单位 {@code erp_initial_finance_partner}（应付/应收）。</p>
 *
 * <p><b>租户与逻辑删除</b>：两张表都带 {@code tenant_id}，全部查询/更新走 MyBatis-Plus Wrapper，
 * 由租户插件与 {@code @TableLogic} 自动注入条件，**没有手写 SQL**（不存在漏拼租户条件的风险）。</p>
 *
 * <p><b>更新为什么用 UpdateWrapper 而不是 updateById</b>：{@code updateById} 会忽略 null 字段，
 * 「把借贷方向清空」这类操作会静默失败；UpdateWrapper 的 {@code .set(col, null)} 才能真正写 NULL。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitialFinanceServiceImpl implements InitialFinanceService {

    /** 按科目的期初类型值域 */
    private static final Set<String> SUBJECT_TYPES = Set.of(
            InitialFinanceSubject.TYPE_BANK_CASH,
            InitialFinanceSubject.TYPE_FIXED_ASSET,
            InitialFinanceSubject.TYPE_BALANCE_SHEET);

    /** 按往来单位的期初类型值域 */
    private static final Set<String> PARTNER_TYPES = Set.of(
            InitialFinancePartner.TYPE_PAYABLE,
            InitialFinancePartner.TYPE_RECEIVABLE);

    /** 年度值域（与前端 :min/:max 一致，开发文档 §10.1 第 30 条） */
    private static final int YEAR_MIN = 2000;
    private static final int YEAR_MAX = 2099;

    /**
     * 存货类科目前缀 —— **沿用本系统既有的资产负债表口径**，不新造映射：
     * {@code FinancialReportServiceImpl.ASSET_LINE_DEFS} 里资产侧固定项目就是
     * {@code {"140", "库存商品"}}（覆盖 1401 材料采购 / 1403 原材料 / 1405 库存商品等 14xx）。
     */
    private static final String INVENTORY_SUBJECT_CODE_PREFIX = "140";

    /** 对平/平衡容差（与资产负债表的平衡容差同口径） */
    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.01");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InitialFinanceSubjectMapper subjectMapper;
    private final InitialFinancePartnerMapper partnerMapper;
    private final AccountSubjectMapper accountSubjectMapper;
    /** 会计期间（关账联动 + 当前会计年）——本模块自带 Mapper，与《会计期间》页同源 */
    private final AccountingPeriodMapper accountingPeriodMapper;
    /** 库存期初金额只读查询（存货对平检查的库存侧取数，手写 SQL 已自带租户条件） */
    private final InitialStockOpeningMapper initialStockOpeningMapper;

    // ══════════════════════════ 查询 ══════════════════════════

    @Override
    public Page<InitialFinanceSubjectVO> pageSubject(InitialFinanceQuery query) {
        String type = requireSubjectType(query.getInitialType());
        Page<InitialFinanceSubject> page = new Page<>(normalizePageNum(query), normalizePageSize(query));
        Page<InitialFinanceSubject> result = subjectMapper.selectPage(page, subjectWrapper(query, type));

        Page<InitialFinanceSubjectVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<InitialFinanceSubjectVO> records = new ArrayList<>();
        for (InitialFinanceSubject entity : result.getRecords()) {
            records.add(toSubjectVO(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public List<InitialFinanceSubjectVO> listSubject(InitialFinanceQuery query) {
        String type = requireSubjectType(query.getInitialType());
        List<InitialFinanceSubject> rows = subjectMapper.selectList(subjectWrapper(query, type));
        List<InitialFinanceSubjectVO> voList = new ArrayList<>();
        for (InitialFinanceSubject entity : rows) {
            voList.add(toSubjectVO(entity));
        }
        return voList;
    }

    @Override
    public Page<InitialFinancePartnerVO> pagePartner(InitialFinanceQuery query) {
        String type = requirePartnerType(query.getInitialType());
        Page<InitialFinancePartner> page = new Page<>(normalizePageNum(query), normalizePageSize(query));
        Page<InitialFinancePartner> result = partnerMapper.selectPage(page, partnerWrapper(query, type));

        Page<InitialFinancePartnerVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<InitialFinancePartnerVO> records = new ArrayList<>();
        for (InitialFinancePartner entity : result.getRecords()) {
            records.add(toPartnerVO(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public List<InitialFinancePartnerVO> listPartner(InitialFinanceQuery query) {
        String type = requirePartnerType(query.getInitialType());
        List<InitialFinancePartner> rows = partnerMapper.selectList(partnerWrapper(query, type));
        List<InitialFinancePartnerVO> voList = new ArrayList<>();
        for (InitialFinancePartner entity : rows) {
            voList.add(toPartnerVO(entity));
        }
        return voList;
    }

    /**
     * 「按科目」查询条件。
     * ⚠️ {@code initial_type} 是**必带**条件：不带就会把 3 个 Tab 的数据串在一起。
     */
    private LambdaQueryWrapper<InitialFinanceSubject> subjectWrapper(InitialFinanceQuery query, String type) {
        LambdaQueryWrapper<InitialFinanceSubject> wrapper = Wrappers.<InitialFinanceSubject>lambdaQuery()
                .eq(InitialFinanceSubject::getInitialType, type)
                .eq(query.getPeriodYear() != null, InitialFinanceSubject::getPeriodYear, query.getPeriodYear())
                .like(StringUtils.hasText(query.getSubjectCode()),
                        InitialFinanceSubject::getSubjectCode, query.getSubjectCode())
                .like(StringUtils.hasText(query.getSubjectName()),
                        InitialFinanceSubject::getSubjectName, query.getSubjectName());
        // 按 id 倒序 = 最新录入在前（雪花 ID 单调递增，排序稳定可复现）
        wrapper.orderByDesc(InitialFinanceSubject::getId);
        return wrapper;
    }

    /** 「按往来单位」查询条件 */
    private LambdaQueryWrapper<InitialFinancePartner> partnerWrapper(InitialFinanceQuery query, String type) {
        LambdaQueryWrapper<InitialFinancePartner> wrapper = Wrappers.<InitialFinancePartner>lambdaQuery()
                .eq(InitialFinancePartner::getInitialType, type)
                .eq(query.getPeriodYear() != null, InitialFinancePartner::getPeriodYear, query.getPeriodYear())
                .like(StringUtils.hasText(query.getPartnerCode()),
                        InitialFinancePartner::getPartnerCode, query.getPartnerCode())
                .like(StringUtils.hasText(query.getPartnerName()),
                        InitialFinancePartner::getPartnerName, query.getPartnerName());
        wrapper.orderByDesc(InitialFinancePartner::getId);
        return wrapper;
    }

    // ══════════════════════════ 保存（按科目） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSubject(List<InitialFinanceSubjectDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("期初数据不能为空");
        }
        int affected = 0;
        for (InitialFinanceSubjectDTO dto : rows) {
            if (dto.getId() != null) {
                affected += doUpdateSubject(dto);
            } else {
                affected += doInsertSubject(dto);
            }
        }
        log.info("保存财务期初（按科目）: rows={}, affected={}", rows.size(), affected);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSubject(InitialFinanceSubjectDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("期初记录ID不能为空");
        }
        return doUpdateSubject(dto);
    }

    private int doInsertSubject(InitialFinanceSubjectDTO dto) {
        validateSubject(dto, true);
        fillSubjectSnapshot(dto);
        // 防重录：同租户 + 同类型 + 同年度 + 同科目只能有一条（对标「保存期初」= 整体保存语义）
        Long exists = subjectMapper.selectCount(Wrappers.<InitialFinanceSubject>lambdaQuery()
                .eq(InitialFinanceSubject::getInitialType, dto.getInitialType())
                .eq(InitialFinanceSubject::getPeriodYear, dto.getPeriodYear())
                .eq(InitialFinanceSubject::getSubjectId, dto.getSubjectId()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException(String.format("科目「%s」%d 年度已录入该期初，请勿重复录入",
                    dto.getSubjectName() == null ? dto.getSubjectCode() : dto.getSubjectName(), dto.getPeriodYear()));
        }

        InitialFinanceSubject entity = new InitialFinanceSubject()
                .setInitialType(dto.getInitialType())
                .setPeriodYear(dto.getPeriodYear())
                .setSubjectId(dto.getSubjectId())
                .setSubjectCode(dto.getSubjectCode())
                .setSubjectName(dto.getSubjectName())
                .setDirection(resolveSubjectDirection(dto))
                .setOpeningAmount(dto.getOpeningAmount());
        int affected = subjectMapper.insert(entity);
        // 回填生成的雪花 ID，便于调用方（批量保存）后续回读
        dto.setId(entity.getId());
        return affected;
    }

    private int doUpdateSubject(InitialFinanceSubjectDTO dto) {
        InitialFinanceSubject exist = subjectMapper.selectById(dto.getId());
        if (exist == null) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        // 期初类型缺省时按库中值补齐（前端批量保存可能不带）
        if (!StringUtils.hasText(dto.getInitialType())) {
            dto.setInitialType(exist.getInitialType());
        }
        if (!exist.getInitialType().equals(dto.getInitialType())) {
            throw new IllegalArgumentException("不允许修改期初类型");
        }
        // 科目不可改（对标中科目是行的身份；开发文档 §10.1 第 19 条要求断言未被改）
        if (dto.getSubjectId() != null && !dto.getSubjectId().equals(exist.getSubjectId())) {
            throw new IllegalArgumentException("不允许修改期初所属科目");
        }
        dto.setSubjectId(exist.getSubjectId());
        dto.setSubjectCode(exist.getSubjectCode());
        dto.setSubjectName(exist.getSubjectName());
        // 只改年度/借贷方向/期初金额（都允许改），并做一次换年后 的防重录校验
        validateSubjectAmountAndYear(dto);
        Long dup = subjectMapper.selectCount(Wrappers.<InitialFinanceSubject>lambdaQuery()
                .eq(InitialFinanceSubject::getInitialType, dto.getInitialType())
                .eq(InitialFinanceSubject::getPeriodYear, dto.getPeriodYear())
                .eq(InitialFinanceSubject::getSubjectId, dto.getSubjectId())
                .ne(InitialFinanceSubject::getId, dto.getId()));
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException(String.format("该科目在 %d 年度已存在另一条期初，请勿重复录入",
                    dto.getPeriodYear()));
        }

        // ⚠️ 用 UpdateWrapper 显式 set：updateById 会忽略 null，「清空借贷方向」会静默失败
        UpdateWrapper<InitialFinanceSubject> wrapper = Wrappers.<InitialFinanceSubject>update()
                .eq("id", dto.getId())
                .eq("initial_type", dto.getInitialType())
                .set("period_year", dto.getPeriodYear())
                .set("direction", resolveSubjectDirection(dto))
                .set("opening_amount", dto.getOpeningAmount())
                .set("update_time", LocalDateTime.now());
        int affected = subjectMapper.update(null, wrapper);
        if (affected == 0) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSubject(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("期初记录ID不能为空");
        }
        // 关账联动：删除同样会改变已关账年度的期初，故与新增/修改同口径拦截
        InitialFinanceSubject exist = subjectMapper.selectById(id);
        if (exist == null) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        assertPeriodEditable(exist.getPeriodYear());
        // @TableLogic → 逻辑删除（UPDATE deleted = 1），不是物理删除
        int affected = subjectMapper.deleteById(id);
        if (affected == 0) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        return affected;
    }

    // ══════════════════════════ 保存（按往来单位） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int savePartner(List<InitialFinancePartnerDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("期初数据不能为空");
        }
        int affected = 0;
        for (InitialFinancePartnerDTO dto : rows) {
            if (dto.getId() != null) {
                affected += doUpdatePartner(dto);
            } else {
                affected += doInsertPartner(dto);
            }
        }
        log.info("保存财务期初（按往来单位）: rows={}, affected={}", rows.size(), affected);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePartner(InitialFinancePartnerDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("期初记录ID不能为空");
        }
        return doUpdatePartner(dto);
    }

    private int doInsertPartner(InitialFinancePartnerDTO dto) {
        validatePartner(dto, true);
        Long exists = partnerMapper.selectCount(Wrappers.<InitialFinancePartner>lambdaQuery()
                .eq(InitialFinancePartner::getInitialType, dto.getInitialType())
                .eq(InitialFinancePartner::getPeriodYear, dto.getPeriodYear())
                .eq(InitialFinancePartner::getPartnerId, dto.getPartnerId()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException(String.format("往来单位「%s」%d 年度已录入该期初，请勿重复录入",
                    dto.getPartnerName() == null ? dto.getPartnerCode() : dto.getPartnerName(), dto.getPeriodYear()));
        }

        InitialFinancePartner entity = new InitialFinancePartner()
                .setInitialType(dto.getInitialType())
                .setPeriodYear(dto.getPeriodYear())
                .setPartnerId(dto.getPartnerId())
                .setPartnerCode(dto.getPartnerCode())
                .setPartnerName(dto.getPartnerName())
                // 与期初类型无关的列强制置空（应付行不得带应收金额，反之亦然）
                .setDefaultHandler(InitialFinancePartner.TYPE_RECEIVABLE.equals(dto.getInitialType())
                        ? dto.getDefaultHandler() : null)
                .setPayableAmount(InitialFinancePartner.TYPE_PAYABLE.equals(dto.getInitialType())
                        ? dto.getPayableAmount() : null)
                .setPrepayAmount(InitialFinancePartner.TYPE_PAYABLE.equals(dto.getInitialType())
                        ? dto.getPrepayAmount() : null)
                .setReceivableAmount(InitialFinancePartner.TYPE_RECEIVABLE.equals(dto.getInitialType())
                        ? dto.getReceivableAmount() : null)
                .setAdvanceAmount(InitialFinancePartner.TYPE_RECEIVABLE.equals(dto.getInitialType())
                        ? dto.getAdvanceAmount() : null);
        int affected = partnerMapper.insert(entity);
        dto.setId(entity.getId());
        return affected;
    }

    private int doUpdatePartner(InitialFinancePartnerDTO dto) {
        InitialFinancePartner exist = partnerMapper.selectById(dto.getId());
        if (exist == null) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        if (!StringUtils.hasText(dto.getInitialType())) {
            dto.setInitialType(exist.getInitialType());
        }
        if (!exist.getInitialType().equals(dto.getInitialType())) {
            throw new IllegalArgumentException("不允许修改期初类型");
        }
        if (dto.getPartnerId() != null && !dto.getPartnerId().equals(exist.getPartnerId())) {
            throw new IllegalArgumentException("不允许修改期初所属往来单位");
        }
        dto.setPartnerId(exist.getPartnerId());
        dto.setPartnerCode(exist.getPartnerCode());
        dto.setPartnerName(exist.getPartnerName());
        validatePartnerAmountAndYear(dto);

        Long dup = partnerMapper.selectCount(Wrappers.<InitialFinancePartner>lambdaQuery()
                .eq(InitialFinancePartner::getInitialType, dto.getInitialType())
                .eq(InitialFinancePartner::getPeriodYear, dto.getPeriodYear())
                .eq(InitialFinancePartner::getPartnerId, dto.getPartnerId())
                .ne(InitialFinancePartner::getId, dto.getId()));
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException(String.format("该往来单位在 %d 年度已存在另一条期初，请勿重复录入",
                    dto.getPeriodYear()));
        }

        boolean payable = InitialFinancePartner.TYPE_PAYABLE.equals(dto.getInitialType());
        UpdateWrapper<InitialFinancePartner> wrapper = Wrappers.<InitialFinancePartner>update()
                .eq("id", dto.getId())
                .eq("initial_type", dto.getInitialType())
                .set("period_year", dto.getPeriodYear())
                .set("default_handler", payable ? null : dto.getDefaultHandler())
                .set("payable_amount", payable ? dto.getPayableAmount() : null)
                .set("prepay_amount", payable ? dto.getPrepayAmount() : null)
                .set("receivable_amount", payable ? null : dto.getReceivableAmount())
                .set("advance_amount", payable ? null : dto.getAdvanceAmount())
                .set("update_time", LocalDateTime.now());
        int affected = partnerMapper.update(null, wrapper);
        if (affected == 0) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePartner(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("期初记录ID不能为空");
        }
        // 关账联动：同 deleteSubject，已全部关账的年度不允许删期初
        InitialFinancePartner exist = partnerMapper.selectById(id);
        if (exist == null) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        assertPeriodEditable(exist.getPeriodYear());
        int affected = partnerMapper.deleteById(id);
        if (affected == 0) {
            throw new IllegalArgumentException("期初记录不存在或已被删除");
        }
        return affected;
    }

    // ══════════════════════════ 试算平衡 ══════════════════════════

    /**
     * 期初试算平衡。
     *
     * <p><b>口径（本实现裁定，开发文档 §11 未预设）</b>：只统计「按科目」三类期初 ——
     * 银行现金 / 固定资产两个 Tab 的科目性质为资产类，一律计入**借方**；
     * 资产负债 Tab 按行上的 {@code direction} 分别计入借/贷。
     * 「按往来单位」的应付/应收期初属往来余额维度，不参与科目试算平衡
     * （对标亦未把二者并列成一张平衡表）。</p>
     *
     * <p><b>不阻断保存</b>：本接口只报数，「不平衡不允许保存」属产品裁定项，
     * 开发文档 §11 明确「不预设」→ 前端仅提示，不做保存门禁。</p>
     */
    @Override
    public TrialBalanceVO trialBalance(Integer periodYear) {
        LambdaQueryWrapper<InitialFinanceSubject> wrapper = Wrappers.<InitialFinanceSubject>lambdaQuery()
                .in(InitialFinanceSubject::getInitialType, SUBJECT_TYPES)
                .eq(periodYear != null, InitialFinanceSubject::getPeriodYear, periodYear);
        List<InitialFinanceSubject> rows = subjectMapper.selectList(wrapper);

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (InitialFinanceSubject row : rows) {
            BigDecimal amount = row.getOpeningAmount() == null ? BigDecimal.ZERO : row.getOpeningAmount();
            // 负债/权益类科目才可能是贷方；只有显式 CREDIT 才计入贷方，其余（含 null）按借方
            if (InitialFinanceSubject.DIRECTION_CREDIT.equals(row.getDirection())) {
                creditTotal = creditTotal.add(amount);
            } else {
                debitTotal = debitTotal.add(amount);
            }
        }

        TrialBalanceVO vo = new TrialBalanceVO();
        vo.setPeriodYear(periodYear);
        vo.setDebitTotal(debitTotal);
        vo.setCreditTotal(creditTotal);
        vo.setDifference(debitTotal.subtract(creditTotal));
        vo.setBalanced(debitTotal.compareTo(creditTotal) == 0);
        vo.setRowCount(rows.size());
        // 存货对平检查（复用已查出的本科目行，避免二次查询）
        vo.setInventoryCheck(buildInventoryCheck(rows));
        return vo;
    }

    /**
     * 存货对平检查：财务侧「存货类科目」期初余额合计 ↔ 库存侧期初金额合计 Σ(quantity × unit_price)。
     *
     * <p><b>口径（沿用开发文档 §5.4 / §10.1 第 32 项的既有裁定，未新增规则）</b>：
     * <ul>
     *   <li>财务侧 = 本次试算已查出的「按科目」行中，{@code subject_code} 以 {@code 140} 开头的行
     *       金额合计（前缀口径与本系统资产负债表 {@code ASSET_LINE_DEFS} 一致）；</li>
     *   <li>库存侧 = {@code erp_stock} 中 {@code deleted = 0 AND is_initial = 1} 行的
     *       Σ(quantity × unit_price)（与《库存期初》列表/导出同一公式；金额是派生值不落库）；</li>
     *   <li>差额 = 财务侧 − 库存侧，|差额| &lt; 0.01 视为对平。</li>
     * </ul>
     *
     * <p><b>只报数不改数</b>：不自动改写任何一侧数据，也不阻断保存 —— 是否强制对平属产品裁定。</p>
     */
    private InventoryBalanceCheckVO buildInventoryCheck(List<InitialFinanceSubject> subjectRows) {
        BigDecimal financeAmount = BigDecimal.ZERO;
        long financeRowCount = 0;
        for (InitialFinanceSubject row : subjectRows) {
            if (row.getSubjectCode() == null
                    || !row.getSubjectCode().startsWith(INVENTORY_SUBJECT_CODE_PREFIX)) {
                continue;
            }
            financeRowCount++;
            financeAmount = financeAmount.add(
                    row.getOpeningAmount() == null ? BigDecimal.ZERO : row.getOpeningAmount());
        }

        // 租户口径与《库存期初》页完全一致（同一个 MyBatisPlusConfig.getCurrentTenantIdValue()），
        // 否则两侧口径不同会让「差额」失去意义：租户不可解析时传 null = 平台级可见。
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        BigDecimal stockAmount = initialStockOpeningMapper.sumOpeningAmount(tenantId);
        long stockRowCount = initialStockOpeningMapper.countOpeningRows(tenantId);
        if (stockAmount == null) {
            stockAmount = BigDecimal.ZERO;
        }

        BigDecimal difference = financeAmount.subtract(stockAmount);
        boolean matched = difference.abs().compareTo(BALANCE_TOLERANCE) < 0;

        InventoryBalanceCheckVO check = new InventoryBalanceCheckVO();
        check.setSubjectCodePrefix(INVENTORY_SUBJECT_CODE_PREFIX);
        check.setFinanceAmount(financeAmount);
        check.setStockAmount(stockAmount);
        check.setDifference(difference);
        check.setMatched(matched);
        check.setFinanceRowCount(financeRowCount);
        check.setStockRowCount(stockRowCount);
        check.setNote(matched
                ? "存货已对平（财务侧存货类科目期初与库存期初金额一致）"
                : "存货未对平：请核对存货类科目（14xx）期初金额与《库存期初》的数量×成本单价");
        return check;
    }

    // ══════════════════════════ 会计期间联动 ══════════════════════════

    /**
     * 当前会计年。
     *
     * <p><b>回退链</b>（按真实数据设计，devdb 实测见开发文档「实现收口 ④」）：
     * <ol>
     *   <li>{@code fin_accounting_period} 中 {@code status = 1} 且「今天落在 start_date..end_date 之间」
     *       的那一行所属的 {@code period_year}；</li>
     *   <li>找不到 → 该租户最大的 {@code period_year}；</li>
     *   <li>再没有（该租户零期间）→ 服务器系统年，并给出 hint 让页面提示。</li>
     * </ol>
     *
     * <p><b>为什么不复用 AccountingPeriodService#list</b>：那个实现把
     * {@code tenant_id = 1} 写死在 wrapper 里（{@code AccountingPeriodServiceImpl:35,54}），
     * 非 1 租户会恒空 → 直接落到「回退系统年」，与真实会计年不符。此处改走 Wrapper
     * **不带**显式 tenant_id，由租户插件按会话注入（与全站其它查询同口径）。</p>
     */
    @Override
    public CurrentYearVO currentYear() {
        int systemYear = LocalDate.now().getYear();
        List<AccountingPeriod> periods = loadAccountingPeriods();

        CurrentYearVO vo = new CurrentYearVO();
        vo.setSystemYear(systemYear);

        // ① 命中「开启中 + 今天落在期间内」
        LocalDate today = LocalDate.now();
        for (AccountingPeriod period : periods) {
            if (!Integer.valueOf(1).equals(period.getStatus())
                    || period.getStartDate() == null || period.getEndDate() == null) {
                continue;
            }
            if (!today.isBefore(period.getStartDate()) && !today.isAfter(period.getEndDate())) {
                vo.setPeriodYear(period.getPeriodYear());
                vo.setSource(CurrentYearVO.SOURCE_OPEN_PERIOD);
                return vo;
            }
        }

        // ② 回退：该租户最大会计年度
        Integer maxYear = null;
        for (AccountingPeriod period : periods) {
            if (period.getPeriodYear() != null
                    && (maxYear == null || period.getPeriodYear() > maxYear)) {
                maxYear = period.getPeriodYear();
            }
        }
        if (maxYear != null) {
            vo.setPeriodYear(maxYear);
            vo.setSource(CurrentYearVO.SOURCE_MAX_PERIOD_YEAR);
            vo.setHint(String.format(
                    "今天不在任何开启中的会计期间内，年度已按最大会计年度 %d 取值", maxYear));
            return vo;
        }

        // ③ 再回退：服务器系统年
        vo.setPeriodYear(systemYear);
        vo.setSource(CurrentYearVO.SOURCE_SYSTEM_YEAR);
        vo.setHint(String.format(
                "本租户尚未设置会计期间，年度已回退为系统年 %d，请先到《会计期间》维护期间", systemYear));
        return vo;
    }

    /**
     * 指定年度的会计期间开启情况。
     *
     * <p><b>裁定（开发文档与《会计期间开发文档》均未预设 → 本实现取较宽松口径）</b>：
     * 「只要该年度**存在任一开启期间**即允许」；只有当该年度**全部期间已关闭**
     * （开启数 = 0 且关闭数 &gt; 0）时才禁止。该年度**一条期间都没有**时不视为「全部关闭」，
     * 仍允许录入（否则临时试录 2099 等未建期间年度会被一刀切禁掉）。</p>
     */
    @Override
    public PeriodStatusVO periodStatus(Integer periodYear) {
        Integer targetYear = periodYear != null ? periodYear : currentYear().getPeriodYear();
        int openCount = 0;
        int closedCount = 0;
        for (AccountingPeriod period : loadAccountingPeriods()) {
            if (!targetYear.equals(period.getPeriodYear())) {
                continue;
            }
            if (Integer.valueOf(0).equals(period.getStatus())) {
                closedCount++;
            } else {
                openCount++;
            }
        }

        boolean editable = !(openCount == 0 && closedCount > 0);
        PeriodStatusVO vo = new PeriodStatusVO();
        vo.setPeriodYear(targetYear);
        vo.setOpenCount(openCount);
        vo.setClosedCount(closedCount);
        vo.setEditable(editable);
        vo.setReason(editable ? null : String.format(
                "%d 年度的会计期间已全部关闭（已月结），不允许新增、修改或删除该年度的期初；如需调整请先到《会计期间》开启对应期间",
                targetYear));
        return vo;
    }

    /** 关账保护：目标年度已全部关账则直接抛 400（Controller 统一转业务错误） */
    private void assertPeriodEditable(Integer periodYear) {
        PeriodStatusVO status = periodStatus(periodYear);
        if (!status.isEditable()) {
            throw new IllegalArgumentException(status.getReason());
        }
    }

    /** 该租户全部会计期间（租户条件由拦截器按会话注入；单租户 12 行/年，全量加载无压力） */
    private List<AccountingPeriod> loadAccountingPeriods() {
        return accountingPeriodMapper.selectList(Wrappers.<AccountingPeriod>lambdaQuery()
                .orderByAsc(AccountingPeriod::getPeriodYear)
                .orderByAsc(AccountingPeriod::getPeriodMonth));
    }

    // ══════════════════════════ 校验与映射 ══════════════════════════

    private void validateSubject(InitialFinanceSubjectDTO dto, boolean requireSubject) {
        if (dto == null) {
            throw new IllegalArgumentException("期初数据不能为空");
        }
        requireSubjectType(dto.getInitialType());
        validateSubjectAmountAndYear(dto);
        if (requireSubject) {
            if (dto.getSubjectId() == null) {
                throw new IllegalArgumentException("请选择科目");
            }
        }
    }

    private void validateSubjectAmountAndYear(InitialFinanceSubjectDTO dto) {
        validatePeriodYear(dto.getPeriodYear());
        // 关账联动：目标年度已全部关账则禁止录入/改期初（口径见 assertPeriodEditable）
        assertPeriodEditable(dto.getPeriodYear());
        if (dto.getOpeningAmount() == null) {
            throw new IllegalArgumentException("请输入期初金额");
        }
        // 借贷方向只在「资产负债期初」有意义（对标其余两个「按科目」Tab 无 direction 列）
        if (StringUtils.hasText(dto.getDirection())
                && !InitialFinanceSubject.DIRECTION_DEBIT.equals(dto.getDirection())
                && !InitialFinanceSubject.DIRECTION_CREDIT.equals(dto.getDirection())) {
            throw new IllegalArgumentException("借贷方向只能是 DEBIT 或 CREDIT");
        }
    }

    private void validatePartner(InitialFinancePartnerDTO dto, boolean requirePartner) {
        if (dto == null) {
            throw new IllegalArgumentException("期初数据不能为空");
        }
        requirePartnerType(dto.getInitialType());
        validatePartnerAmountAndYear(dto);
        if (requirePartner && dto.getPartnerId() == null) {
            throw new IllegalArgumentException("请选择往来单位");
        }
    }

    private void validatePartnerAmountAndYear(InitialFinancePartnerDTO dto) {
        validatePeriodYear(dto.getPeriodYear());
        // 关账联动：目标年度已全部关账则禁止录入/改期初（口径见 assertPeriodEditable）
        assertPeriodEditable(dto.getPeriodYear());
        if (InitialFinancePartner.TYPE_PAYABLE.equals(dto.getInitialType())) {
            if (dto.getPayableAmount() == null) {
                throw new IllegalArgumentException("请输入应付金额");
            }
        } else if (dto.getReceivableAmount() == null) {
            throw new IllegalArgumentException("请输入应收金额");
        }
    }

    /** 年度值域 2000–2099（与前端 :min/:max 一致，开发文档 §10.1 第 30 条） */
    private void validatePeriodYear(Integer periodYear) {
        if (periodYear == null) {
            throw new IllegalArgumentException("请选择期初年度");
        }
        if (periodYear < YEAR_MIN || periodYear > YEAR_MAX) {
            throw new IllegalArgumentException(String.format("期初年度需在 %d-%d 之间", YEAR_MIN, YEAR_MAX));
        }
    }

    /**
     * 借贷方向归一：
     * 资产负债期初未填时默认「借方」（对标资产负债 Tab 实测行多为借方）；
     * 其余两个类型（银行现金/固定资产）无该列 → 强制 null，避免脏数据。
     */
    private String resolveSubjectDirection(InitialFinanceSubjectDTO dto) {
        if (!InitialFinanceSubject.TYPE_BALANCE_SHEET.equals(dto.getInitialType())) {
            return null;
        }
        return StringUtils.hasText(dto.getDirection())
                ? dto.getDirection() : InitialFinanceSubject.DIRECTION_DEBIT;
    }

    /**
     * 补全科目快照：上游（前端下拉）通常已带编号/名称；若只传了 subjectId
     * （例如接口直调），则从会计科目档案 {@code finance_account_subject} 反查补全，
     * 保证列表的「科目编号/科目名称」两列永远有值。
     */
    private void fillSubjectSnapshot(InitialFinanceSubjectDTO dto) {
        if (StringUtils.hasText(dto.getSubjectCode()) && StringUtils.hasText(dto.getSubjectName())) {
            return;
        }
        if (dto.getSubjectId() == null) {
            return;
        }
        AccountSubject subject = accountSubjectMapper.selectById(dto.getSubjectId());
        if (subject == null) {
            throw new IllegalArgumentException("所选科目不存在或已被删除");
        }
        if (!StringUtils.hasText(dto.getSubjectCode())) {
            dto.setSubjectCode(subject.getSubjectCode());
        }
        if (!StringUtils.hasText(dto.getSubjectName())) {
            dto.setSubjectName(subject.getSubjectName());
        }
    }

    private String requireSubjectType(String initialType) {
        if (!StringUtils.hasText(initialType) || !SUBJECT_TYPES.contains(initialType)) {
            throw new IllegalArgumentException("期初类型不合法（按科目：BANK_CASH / FIXED_ASSET / BALANCE_SHEET）");
        }
        return initialType;
    }

    private String requirePartnerType(String initialType) {
        if (!StringUtils.hasText(initialType) || !PARTNER_TYPES.contains(initialType)) {
            throw new IllegalArgumentException("期初类型不合法（按往来单位：PAYABLE / RECEIVABLE）");
        }
        return initialType;
    }

    private long normalizePageNum(InitialFinanceQuery query) {
        Integer pageNum = query.getPageNum();
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    private long normalizePageSize(InitialFinanceQuery query) {
        Integer pageSize = query.getPageSize();
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        // 上限保护：避免 pageSize=100000 打爆内存
        return Math.min(pageSize, 500);
    }

    private InitialFinanceSubjectVO toSubjectVO(InitialFinanceSubject entity) {
        InitialFinanceSubjectVO vo = new InitialFinanceSubjectVO();
        vo.setId(entity.getId());
        vo.setInitialType(entity.getInitialType());
        vo.setPeriodYear(entity.getPeriodYear());
        vo.setSubjectId(entity.getSubjectId());
        vo.setSubjectCode(entity.getSubjectCode());
        vo.setSubjectName(entity.getSubjectName());
        vo.setDirection(entity.getDirection());
        vo.setOpeningAmount(entity.getOpeningAmount());
        vo.setCreateTime(entity.getCreateTime() == null
                ? null : entity.getCreateTime().format(DATE_TIME_FORMATTER));
        return vo;
    }

    private InitialFinancePartnerVO toPartnerVO(InitialFinancePartner entity) {
        InitialFinancePartnerVO vo = new InitialFinancePartnerVO();
        vo.setId(entity.getId());
        vo.setInitialType(entity.getInitialType());
        vo.setPeriodYear(entity.getPeriodYear());
        vo.setPartnerId(entity.getPartnerId());
        vo.setPartnerCode(entity.getPartnerCode());
        vo.setPartnerName(entity.getPartnerName());
        vo.setDefaultHandler(entity.getDefaultHandler());
        vo.setPayableAmount(entity.getPayableAmount());
        vo.setPrepayAmount(entity.getPrepayAmount());
        vo.setReceivableAmount(entity.getReceivableAmount());
        vo.setAdvanceAmount(entity.getAdvanceAmount());
        vo.setCreateTime(entity.getCreateTime() == null
                ? null : entity.getCreateTime().format(DATE_TIME_FORMATTER));
        return vo;
    }
}

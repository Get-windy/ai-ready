package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.performance.HrPerformance;
import cn.aiedge.hr.salary.HrSalaryPayment;
import cn.aiedge.hr.salary.HrSalaryStructure;
import cn.aiedge.hr.mapper.HrEmployeeMapper;
import cn.aiedge.hr.mapper.HrPerformanceMapper;
import cn.aiedge.hr.mapper.HrSalaryPaymentMapper;
import cn.aiedge.hr.service.HrAttendanceService;
import cn.aiedge.hr.service.HrSalaryPaymentService;
import cn.aiedge.hr.service.HrSalaryStructureService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 薪资发放服务实现
 *
 * <p>与旧实现的三处实质性订正：</p>
 * <ol>
 *   <li><b>个税口径</b>：旧实现按月单独计税（分段数组其实是「级距宽度」，算术自洽但**口径错**），
 *       本实现改为**累计预扣预缴法**——按当年累计收入、累计减除费用（5000 × 月数）、
 *       累计专项扣除计算累计应纳税额，再减去本年已预扣税额；</li>
 *   <li><b>绩效联动</b>：`performance_amount` 不再固定等于薪资结构里的绩效基数，
 *       而是乘以该员工当期已确认绩效考核的**绩效系数**（未考核默认 1.0）；</li>
 *   <li><b>请假不再误扣</b>：缺勤天数只统计 `ABSENT`，`LEAVE`（请假批准写入的休假标记）不计入扣款。</li>
 * </ol>
 *
 * <p><b>仍未实现的缺口</b>（如实登记）：社保/公积金比例（10.5% / 12%）与起征点（5000）
 * 仍写死在本类中，**没有薪资规则引擎**；见《薪资管理开发文档》「剩余缺口」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrSalaryPaymentServiceImpl extends ServiceImpl<HrSalaryPaymentMapper, HrSalaryPayment>
        implements HrSalaryPaymentService {

    /** 个税基本减除费用（元/月） */
    private static final BigDecimal TAX_BASIC_DEDUCTION = new BigDecimal("5000");
    /** 社保个人比例（写死，待规则引擎接管） */
    private static final BigDecimal SOCIAL_RATE = new BigDecimal("0.105");
    /** 公积金个人比例（写死，待规则引擎接管） */
    private static final BigDecimal FUND_RATE = new BigDecimal("0.12");
    /** 加班工资倍率（写死，待规则引擎接管） */
    private static final BigDecimal OVERTIME_RATE = new BigDecimal("1.5");

    /** 累计预扣法年度预扣率表：{ 级距上限, 税率, 速算扣除数 } */
    private static final BigDecimal[][] CUMULATIVE_TAX_BRACKETS = {
            {new BigDecimal("36000"), new BigDecimal("0.03"), new BigDecimal("0")},
            {new BigDecimal("144000"), new BigDecimal("0.10"), new BigDecimal("2520")},
            {new BigDecimal("300000"), new BigDecimal("0.20"), new BigDecimal("16920")},
            {new BigDecimal("420000"), new BigDecimal("0.25"), new BigDecimal("31920")},
            {new BigDecimal("660000"), new BigDecimal("0.30"), new BigDecimal("52920")},
            {new BigDecimal("960000"), new BigDecimal("0.35"), new BigDecimal("85920")},
            {null, new BigDecimal("0.45"), new BigDecimal("181920")},
    };

    private final HrEmployeeMapper employeeMapper;
    private final HrPerformanceMapper performanceMapper;
    private final HrSalaryStructureService structureService;
    private final HrAttendanceService attendanceService;
    private final HrLookupHelper lookupHelper;

    @Override
    public Page<HrSalaryPayment> pagePayments(Page<HrSalaryPayment> page, Long tenantId,
                                              Long employeeId, String paymentMonth,
                                              Integer status, Long deptId, String keyword) {
        LambdaQueryWrapper<HrSalaryPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrSalaryPayment::getTenantId, tenantId)
                .eq(employeeId != null, HrSalaryPayment::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(paymentMonth), HrSalaryPayment::getPaymentMonth, paymentMonth)
                .eq(status != null, HrSalaryPayment::getStatus, status);
        applyDeptScope(wrapper, deptId, keyword);
        wrapper.orderByDesc(HrSalaryPayment::getPaymentMonth).orderByDesc(HrSalaryPayment::getId);
        Page<HrSalaryPayment> result = page(page, wrapper);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public Map<String, Object> payslip(Long id) {
        HrSalaryPayment payment = id == null ? null : getById(id);
        if (payment == null) {
            throw new BusinessException("薪资记录不存在");
        }
        enrich(List.of(payment));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payment", payment);
        HrSalaryStructure structure = structureService.getEffectiveByEmployeeId(payment.getEmployeeId());
        result.put("structure", structure);
        return result;
    }

    // ── 月度生成 ──────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateMonthlyPayment(String paymentMonth) {
        YearMonth ym = parseMonth(paymentMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        int workDays = countWorkDays(monthStart, monthEnd);
        Long tenantId = SecurityUtils.getCurrentTenantId();

        // 显式带租户条件：即便将来 hr_employee 被加进租户忽略清单，也不会变成"全租户发薪"
        List<HrEmployee> employees = employeeMapper.selectList(new LambdaQueryWrapper<HrEmployee>()
                .eq(tenantId != null, HrEmployee::getTenantId, tenantId)
                .in(HrEmployee::getStatus, 1, 2));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentMonth", paymentMonth);
        if (employees.isEmpty()) {
            log.warn("无在职员工，跳过薪资生成: month={}", paymentMonth);
            result.put("generated", 0);
            result.put("skipped", 0);
            result.put("noStructure", 0);
            result.put("failed", 0);
            result.put("message", "没有在职或试用期员工，未生成任何记录");
            return result;
        }

        List<HrSalaryPayment> payments = new ArrayList<>();
        int skipped = 0;
        int noStructure = 0;
        int failed = 0;

        for (HrEmployee employee : employees) {
            // 幂等：同月已有记录则跳过，避免重复生成覆盖已发放数据
            Long dup = baseMapper.selectCount(new LambdaQueryWrapper<HrSalaryPayment>()
                    .eq(HrSalaryPayment::getEmployeeId, employee.getId())
                    .eq(HrSalaryPayment::getPaymentMonth, paymentMonth));
            if (dup != null && dup > 0) {
                skipped++;
                continue;
            }
            HrSalaryStructure structure = structureService.getEffectiveByEmployeeId(employee.getId());
            if (structure == null) {
                noStructure++;
                log.warn("员工 {} 无有效薪资结构，跳过", employee.getEmployeeNo());
                continue;
            }
            try {
                payments.add(buildPayment(employee, structure, paymentMonth, workDays));
            } catch (Exception e) {
                // 单个员工失败不再被吞成"静默 0 条"：计数并冒泡到统计结果里
                failed++;
                log.error("生成员工 {} 薪资失败: {}", employee.getEmployeeNo(), e.getMessage(), e);
            }
        }

        if (!payments.isEmpty()) {
            saveBatch(payments);
        }
        result.put("generated", payments.size());
        result.put("skipped", skipped);
        result.put("noStructure", noStructure);
        result.put("failed", failed);
        result.put("message", String.format("生成完成：新增 %d 条，已存在跳过 %d 条，无薪资结构 %d 人，失败 %d 人",
                payments.size(), skipped, noStructure, failed));
        log.info("月度薪资生成: month={}, generated={}, skipped={}, noStructure={}, failed={}",
                paymentMonth, payments.size(), skipped, noStructure, failed);
        return result;
    }

    private HrSalaryPayment buildPayment(HrEmployee employee, HrSalaryStructure structure,
                                         String paymentMonth, int workDays) {
        BigDecimal baseAmount = valueOrZero(structure.getBaseSalary());
        BigDecimal perfBase = valueOrZero(structure.getPerformanceSalary());
        BigDecimal performanceAmount = perfBase.multiply(resolvePerformanceCoefficient(employee.getId(), paymentMonth))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal allowanceAmount = BigDecimal.ZERO
                .add(valueOrZero(structure.getPositionAllowance()))
                .add(valueOrZero(structure.getTransportAllowance()))
                .add(valueOrZero(structure.getMealAllowance()))
                .add(valueOrZero(structure.getHousingAllowance()))
                .add(valueOrZero(structure.getOtherAllowance()));

        // ── 考勤 ──
        List<HrAttendance> attendances = attendanceService.getByEmployeeAndMonth(employee.getId(), paymentMonth);
        BigDecimal actualHours = attendances.stream()
                .map(HrAttendance::getWorkHours).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long absentDays = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();

        BigDecimal standardHours = BigDecimal.valueOf(workDays).multiply(new BigDecimal("8"));
        BigDecimal overtimeHours = actualHours.subtract(standardHours).max(BigDecimal.ZERO);

        BigDecimal dailyRate = workDays > 0
                ? baseAmount.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal hourlyRate = dailyRate.divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP);
        BigDecimal overtimeAmount = hourlyRate.multiply(overtimeHours)
                .multiply(OVERTIME_RATE).setScale(2, RoundingMode.HALF_UP);

        // 缺勤扣款：只统计 ABSENT，休假日（LEAVE）不扣
        BigDecimal deductAmount = dailyRate.multiply(BigDecimal.valueOf(absentDays)).setScale(2, RoundingMode.HALF_UP);

        // ── 社保 / 公积金 ──
        BigDecimal socialDeduct = valueOrZero(structure.getSocialBase())
                .multiply(SOCIAL_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fundDeduct = valueOrZero(structure.getFundBase())
                .multiply(FUND_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossAmount = baseAmount.add(performanceAmount).add(allowanceAmount).add(overtimeAmount);
        BigDecimal taxDeduct = calculateCumulativeTax(employee.getId(), employee.getHireDate(), paymentMonth,
                grossAmount, socialDeduct.add(fundDeduct));

        BigDecimal actualAmount = grossAmount
                .subtract(socialDeduct).subtract(fundDeduct).subtract(taxDeduct).subtract(deductAmount)
                .setScale(2, RoundingMode.HALF_UP);

        HrSalaryPayment payment = new HrSalaryPayment();
        payment.setTenantId(employee.getTenantId());
        payment.setEmployeeId(employee.getId());
        payment.setPaymentMonth(paymentMonth);
        payment.setBaseAmount(baseAmount);
        payment.setPerformanceAmount(performanceAmount);
        payment.setAllowanceAmount(allowanceAmount);
        payment.setOvertimeAmount(overtimeAmount);
        payment.setDeductAmount(deductAmount);
        payment.setSocialDeduct(socialDeduct);
        payment.setFundDeduct(fundDeduct);
        payment.setTaxDeduct(taxDeduct);
        payment.setGrossAmount(grossAmount);
        payment.setActualAmount(actualAmount);
        payment.setStatus(0);
        payment.setWorkHours(actualHours);
        payment.setAbsentDays((int) absentDays);
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        return payment;
    }

    /**
     * 绩效系数：优先取该员工在当期（或当期所在年度最近一期）**已确认/已考核**绩效记录的
     * `performance_coefficient`；没有记录或系数为空时按 1.0（不做调整）。
     */
    private BigDecimal resolvePerformanceCoefficient(Long employeeId, String paymentMonth) {
        HrPerformance performance = performanceMapper.selectOne(new LambdaQueryWrapper<HrPerformance>()
                .eq(HrPerformance::getEmployeeId, employeeId)
                .isNotNull(HrPerformance::getLevel)
                .in(HrPerformance::getStatus, 1, 2)
                .orderByDesc(HrPerformance::getReviewPeriod)
                .orderByDesc(HrPerformance::getId)
                .last("limit 1"));
        if (performance == null || performance.getPerformanceCoefficient() == null) {
            return BigDecimal.ONE;
        }
        return performance.getPerformanceCoefficient();
    }

    /**
     * 个税：**累计预扣预缴法**。
     *
     * <p>本期应预扣预缴税额 = (累计应纳税所得额 × 预扣率 − 速算扣除数) − 本年已预扣预缴税额。<br>
     * 累计应纳税所得额 = 本年累计收入 − 累计减除费用 − 累计专项扣除(社保 + 公积金)。</p>
     *
     * <p><b>累计减除费用</b>按 5000 元/月 × 「纳税人当年截至本月**在本单位的任职受雇月份数**」
     * 计算（见国税总局 2018 年第 61 号公告的口径）——即从 `hire_date` 与当年 1 月 1 日的**较晚者**
     * 起算，而不是直接用日历月份。若直接用日历月份（如 9 月即 5000 × 9），会把员工入职前的月份
     * 也当作已在本单位受雇，导致累计减除费用虚高、**应缴税额被少算甚至算成 0**。</p>
     */
    private BigDecimal calculateCumulativeTax(Long employeeId, LocalDate hireDate, String paymentMonth,
                                              BigDecimal currentGross, BigDecimal currentSpecialDeduction) {
        int year = Integer.parseInt(paymentMonth.substring(0, 4));
        LocalDate periodEnd = YearMonth.parse(paymentMonth).atEndOfMonth();

        List<HrSalaryPayment> history = list(new LambdaQueryWrapper<HrSalaryPayment>()
                .eq(HrSalaryPayment::getEmployeeId, employeeId)
                .likeRight(HrSalaryPayment::getPaymentMonth, year + "-")
                .lt(HrSalaryPayment::getPaymentMonth, paymentMonth));

        BigDecimal cumulativeGross = currentGross;
        BigDecimal cumulativeSpecial = currentSpecialDeduction;
        BigDecimal withheldTax = BigDecimal.ZERO;
        for (HrSalaryPayment h : history) {
            cumulativeGross = cumulativeGross.add(valueOrZero(h.getGrossAmount()));
            cumulativeSpecial = cumulativeSpecial
                    .add(valueOrZero(h.getSocialDeduct()))
                    .add(valueOrZero(h.getFundDeduct()));
            withheldTax = withheldTax.add(valueOrZero(h.getTaxDeduct()));
        }

        int monthsEmployed = resolveMonthsEmployed(hireDate, year, periodEnd);
        BigDecimal basicDeduction = TAX_BASIC_DEDUCTION.multiply(BigDecimal.valueOf(monthsEmployed));
        BigDecimal cumulativeTaxable = cumulativeGross.subtract(cumulativeSpecial).subtract(basicDeduction);
        if (cumulativeTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal cumulativeTax = applyRateTable(cumulativeTaxable);
        BigDecimal currentTax = cumulativeTax.subtract(withheldTax);
        return currentTax.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** 本单位当年截至本月的任职受雇月份数（1~12）；无入职日期时按 12 个月（最宽松）处理 */
    private int resolveMonthsEmployed(LocalDate hireDate, int year, LocalDate periodEnd) {
        if (hireDate == null) {
            return Math.max(1, Math.min(12, periodEnd.getMonthValue()));
        }
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate start = hireDate.isBefore(yearStart) ? yearStart : hireDate;
        if (start.isAfter(periodEnd)) {
            return 1;
        }
        int months = (int) java.time.temporal.ChronoUnit.MONTHS.between(
                start.withDayOfMonth(1), periodEnd.withDayOfMonth(1)) + 1;
        return Math.max(1, Math.min(12, months));
    }

    /** 按年度累计预扣率表算累计应纳税额 */
    private BigDecimal applyRateTable(BigDecimal cumulativeTaxable) {
        for (BigDecimal[] bracket : CUMULATIVE_TAX_BRACKETS) {
            BigDecimal upper = bracket[0];
            if (upper == null || cumulativeTaxable.compareTo(upper) <= 0) {
                return cumulativeTaxable.multiply(bracket[1]).subtract(bracket[2]);
            }
        }
        BigDecimal[] last = CUMULATIVE_TAX_BRACKETS[CUMULATIVE_TAX_BRACKETS.length - 1];
        return cumulativeTaxable.multiply(last[1]).subtract(last[2]);
    }

    // ── 发放流转 ──────────────────────────────────────────

    /**
     * 确认发放。
     *
     * <p>可从「待发放(0)」或「已撤销(2)」进入——撤销的语义是「这笔发放作废、可重发」，
     * 若禁止从 2 再确认，被撤销的记录会成为谁也用不了的死数据（发错只能删了重建）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long id) {
        HrSalaryPayment payment = require(id);
        if (payment.getStatus() != null && payment.getStatus() == 1) {
            throw new BusinessException("该薪资记录已发放，不能重复确认");
        }
        LambdaUpdateWrapper<HrSalaryPayment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrSalaryPayment::getId, id)
                .set(HrSalaryPayment::getStatus, 1)
                .set(HrSalaryPayment::getPaymentDate, LocalDate.now())
                .set(HrSalaryPayment::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("确认发放薪资: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchConfirm(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要发放的薪资记录");
        }
        for (Long id : ids) {
            HrSalaryPayment payment = getById(id);
            if (payment == null || (payment.getStatus() != null && payment.getStatus() != 0)) {
                continue;
            }
            LambdaUpdateWrapper<HrSalaryPayment> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(HrSalaryPayment::getId, id)
                    .set(HrSalaryPayment::getStatus, 1)
                    .set(HrSalaryPayment::getPaymentDate, LocalDate.now())
                    .set(HrSalaryPayment::getUpdateTime, LocalDateTime.now());
            update(wrapper);
        }
        log.info("批量确认发放薪资: count={}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePayment(Long id) {
        HrSalaryPayment payment = require(id);
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new BusinessException("只有「已发放」的薪资记录可以撤销");
        }
        LambdaUpdateWrapper<HrSalaryPayment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrSalaryPayment::getId, id)
                .set(HrSalaryPayment::getStatus, 2)
                .set(HrSalaryPayment::getPaymentDate, null)
                .set(HrSalaryPayment::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("撤销薪资发放: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayment(Long id) {
        require(id);
        removeById(id);
    }

    @Override
    public Map<String, Object> statistics(String paymentMonth, Long deptId) {
        LambdaQueryWrapper<HrSalaryPayment> wrapper = new LambdaQueryWrapper<HrSalaryPayment>()
                .eq(StringUtils.hasText(paymentMonth), HrSalaryPayment::getPaymentMonth, paymentMonth);
        applyDeptScope(wrapper, deptId, null);
        List<HrSalaryPayment> rows = list(wrapper);

        BigDecimal gross = sum(rows, HrSalaryPayment::getGrossAmount);
        BigDecimal actual = sum(rows, HrSalaryPayment::getActualAmount);
        BigDecimal social = sum(rows, HrSalaryPayment::getSocialDeduct);
        BigDecimal fund = sum(rows, HrSalaryPayment::getFundDeduct);
        BigDecimal tax = sum(rows, HrSalaryPayment::getTaxDeduct);
        BigDecimal overtime = sum(rows, HrSalaryPayment::getOvertimeAmount);

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("paymentMonth", paymentMonth);
        stat.put("headcount", rows.size());
        stat.put("paidCount", rows.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count());
        stat.put("pendingCount", rows.stream().filter(p -> p.getStatus() != null && p.getStatus() == 0).count());
        stat.put("grossTotal", gross);
        stat.put("actualTotal", actual);
        stat.put("socialTotal", social);
        stat.put("fundTotal", fund);
        stat.put("taxTotal", tax);
        stat.put("overtimeTotal", overtime);
        return stat;
    }

    // ── 内部 ──────────────────────────────────────────────

    private BigDecimal sum(List<HrSalaryPayment> rows,
                           java.util.function.Function<HrSalaryPayment, BigDecimal> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private void applyDeptScope(LambdaQueryWrapper<HrSalaryPayment> wrapper, Long deptId, String keyword) {
        if (deptId != null) {
            List<Long> ids = lookupHelper.employeeIdsOfDept(deptId);
            if (ids.isEmpty()) {
                wrapper.eq(HrSalaryPayment::getId, -1L);
                return;
            }
            wrapper.in(HrSalaryPayment::getEmployeeId, ids);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> ids = lookupHelper.employeeIdsByKeyword(keyword);
            if (ids.isEmpty()) {
                wrapper.eq(HrSalaryPayment::getId, -1L);
            } else {
                wrapper.in(HrSalaryPayment::getEmployeeId, ids);
            }
        }
    }

    private HrSalaryPayment require(Long id) {
        HrSalaryPayment payment = id == null ? null : getById(id);
        if (payment == null) {
            throw new BusinessException("薪资记录不存在");
        }
        return payment;
    }

    private YearMonth parseMonth(String paymentMonth) {
        if (!StringUtils.hasText(paymentMonth)) {
            throw new BusinessException("发放月份不能为空");
        }
        try {
            return YearMonth.parse(paymentMonth.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("发放月份格式错误，应为 yyyy-MM：" + paymentMonth);
        }
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int countWorkDays(LocalDate start, LocalDate end) {
        int workDays = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() <= 5) {
                workDays++;
            }
            date = date.plusDays(1);
        }
        return workDays;
    }

    private void enrich(List<HrSalaryPayment> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrSalaryPayment::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> deptNameByEmp = lookupHelper.deptNameByEmployeeId(empMap.keySet());
        for (HrSalaryPayment p : rows) {
            HrEmployee emp = empMap.get(p.getEmployeeId());
            if (emp != null) {
                p.setEmployeeName(emp.getEmployeeName());
                p.setEmployeeNo(emp.getEmployeeNo());
            }
            p.setDeptName(deptNameByEmp.get(p.getEmployeeId()));
        }
    }
}

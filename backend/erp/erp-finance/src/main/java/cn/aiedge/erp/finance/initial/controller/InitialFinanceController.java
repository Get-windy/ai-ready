package cn.aiedge.erp.finance.initial.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.initial.dto.InitialFinancePartnerDTO;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceQuery;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceSubjectDTO;
import cn.aiedge.erp.finance.initial.entity.InitialFinancePartner;
import cn.aiedge.erp.finance.initial.service.InitialFinanceService;
import cn.aiedge.erp.finance.initial.vo.CurrentYearVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinancePartnerVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinanceSubjectVO;
import cn.aiedge.erp.finance.initial.vo.PeriodStatusVO;
import cn.aiedge.erp.finance.initial.vo.TrialBalanceVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 财务期初 Controller（设置 → 数据录入 → 财务期初，菜单 70551 / set:initial-finance，单入口）。
 *
 * <p>对标 ql361「设置 → 期初录入 → 财务期初」实测形态 = **5 个 Tab 的台账**：
 * 银行现金期初 / 应付期初 / 应收期初 / 固定资产期初 / 资产负债期初。
 * 本控制器按「两种维度」分两组端点（开发文档 §7.3 路线 B）：
 * 按科目（银行现金 / 固定资产 / 资产负债）与按往来单位（应付 / 应收）。</p>
 *
 * <p><b>接口契约（与前端 {@code api/set.ts} 的 initialFinanceApi 严格一一对应）</b>：
 * <pre>
 *   GET    /subject/page      按科目分页（必传 initialType）
 *   POST   /subject/save      按科目批量保存（body 为数组；id 有值=更新，无值=新增）
 *   PUT    /subject/update    按科目单条更新（id 在 body；科目不可改）
 *   DELETE /subject/{id}      按科目删除（逻辑删除）
 *   GET    /partner/page      按往来单位分页
 *   POST   /partner/save      按往来单位批量保存
 *   PUT    /partner/update    按往来单位单条更新
 *   DELETE /partner/{id}      按往来单位删除
 *   GET    /trial-balance     期初试算平衡（借贷合计 + 存货对平检查）
 *   GET    /current-year      当前会计年（取代前端写死的自然年）
 *   GET    /period-status     指定年度关账状态（关账保护：已全部关账的年度禁止增/改/删）
 *   GET    /export            导出（与分页同口径，不分页；前端据返回值生成 xlsx）
 * </pre>
 * ⚠️ 历史 P0（2026-09-18 前）：前端打的是 {@code /set/initial-finance/*} —— 后端**零控制器**、
 * 库**零表**，5 个调用全部 404（开发文档 §6.2 / §12-P0）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/initial")
@RequiredArgsConstructor
@Tag(name = "财务期初", description = "设置 → 数据录入 → 财务期初：5 类期初台账（按科目 + 按往来单位）")
public class InitialFinanceController {

    private final InitialFinanceService initialFinanceService;

    /** Spring 容器里的全局 ObjectMapper（用于把「数组 / 单条对象」两种 body 归一成列表） */
    private final ObjectMapper objectMapper;

    // ══════════════════════════ 按科目（银行现金 / 固定资产 / 资产负债） ══════════════════════════

    @Operation(summary = "分页查询期初（按科目）：initialType = BANK_CASH / FIXED_ASSET / BALANCE_SHEET")
    @GetMapping("/subject/page")
    @SaCheckPermission("set:initial-finance:view")
    public Result<Page<InitialFinanceSubjectVO>> subjectPage(InitialFinanceQuery query) {
        try {
            return Result.ok(initialFinanceService.pageSubject(query));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "保存期初（按科目）：body 为数组（批量，id 有值=更新、无值=新增）；单条对象亦接受")
    @PostMapping("/subject/save")
    @SaCheckPermission("set:initial-finance:create")
    public Result<Integer> subjectSave(@RequestBody JsonNode body) {
        try {
            return Result.ok("保存成功",
                    initialFinanceService.saveSubject(readRows(body, InitialFinanceSubjectDTO.class)));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (JsonProcessingException e) {
            return Result.fail(400, "期初数据格式不正确");
        }
    }

    @Operation(summary = "更新期初（按科目，单条）：科目不可改")
    @PutMapping("/subject/update")
    @SaCheckPermission("set:initial-finance:update")
    public Result<Integer> subjectUpdate(@RequestBody InitialFinanceSubjectDTO dto) {
        try {
            return Result.ok("更新成功", initialFinanceService.updateSubject(dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除期初（按科目，逻辑删除）")
    @DeleteMapping("/subject/{id}")
    @SaCheckPermission("set:initial-finance:delete")
    public Result<Integer> subjectDelete(@Parameter(description = "期初记录ID") @PathVariable Long id) {
        try {
            return Result.ok("删除成功", initialFinanceService.deleteSubject(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ══════════════════════════ 按往来单位（应付 / 应收） ══════════════════════════

    @Operation(summary = "分页查询期初（按往来单位）：initialType = PAYABLE / RECEIVABLE")
    @GetMapping("/partner/page")
    @SaCheckPermission("set:initial-finance:view")
    public Result<Page<InitialFinancePartnerVO>> partnerPage(InitialFinanceQuery query) {
        try {
            return Result.ok(initialFinanceService.pagePartner(query));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "保存期初（按往来单位）：body 为数组（批量）；单条对象亦接受")
    @PostMapping("/partner/save")
    @SaCheckPermission("set:initial-finance:create")
    public Result<Integer> partnerSave(@RequestBody JsonNode body) {
        try {
            return Result.ok("保存成功",
                    initialFinanceService.savePartner(readRows(body, InitialFinancePartnerDTO.class)));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (JsonProcessingException e) {
            return Result.fail(400, "期初数据格式不正确");
        }
    }

    @Operation(summary = "更新期初（按往来单位，单条）：往来单位不可改")
    @PutMapping("/partner/update")
    @SaCheckPermission("set:initial-finance:update")
    public Result<Integer> partnerUpdate(@RequestBody InitialFinancePartnerDTO dto) {
        try {
            return Result.ok("更新成功", initialFinanceService.updatePartner(dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除期初（按往来单位，逻辑删除）")
    @DeleteMapping("/partner/{id}")
    @SaCheckPermission("set:initial-finance:delete")
    public Result<Integer> partnerDelete(@Parameter(description = "期初记录ID") @PathVariable Long id) {
        try {
            return Result.ok("删除成功", initialFinanceService.deletePartner(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ══════════════════════════ 试算平衡 / 导出 ══════════════════════════

    @Operation(summary = "期初试算平衡：返回 {debitTotal, creditTotal, difference, balanced, inventoryCheck}")
    @GetMapping("/trial-balance")
    @SaCheckPermission("set:initial-finance:view")
    public Result<TrialBalanceVO> trialBalance(
            @Parameter(description = "期初年度，不传=跨年度汇总") @RequestParam(required = false) Integer periodYear) {
        return Result.ok(initialFinanceService.trialBalance(periodYear));
    }

    // ══════════════════════════ 会计期间联动 ══════════════════════════

    /**
     * 当前会计年（取代前端原先写死的自然年）。
     * 回退链：开启中且今天落在期间内的年度 → 该租户最大会计年度 → 服务器系统年。
     */
    @Operation(summary = "当前会计年：返回 {periodYear, systemYear, source, hint}")
    @GetMapping("/current-year")
    @SaCheckPermission("set:initial-finance:view")
    public Result<CurrentYearVO> currentYear() {
        return Result.ok(initialFinanceService.currentYear());
    }

    /**
     * 指定年度的会计期间开启情况（关账保护）：前端据此置灰「录入期初 / 保存期初」并给出原因；
     * 后端在新增/修改/删除时按同一口径硬校验（{@code periodStatus} 不通过直接 400）。
     */
    @Operation(summary = "年度关账状态：返回 {openCount, closedCount, editable, reason}")
    @GetMapping("/period-status")
    @SaCheckPermission("set:initial-finance:view")
    public Result<PeriodStatusVO> periodStatus(
            @Parameter(description = "期初年度，不传=当前会计年") @RequestParam(required = false) Integer periodYear) {
        return Result.ok(initialFinanceService.periodStatus(periodYear));
    }

    /**
     * 导出当前 Tab 的全部期初（与分页同一套过滤条件，不分页）。
     * 返回 {@code Object}：按科目的 Tab 返回 SubjectVO 列表，按往来单位的 Tab 返回 PartnerVO 列表。
     */
    @Operation(summary = "导出期初（与分页同口径，不分页）")
    @GetMapping("/export")
    @SaCheckPermission("set:initial-finance:view")
    public Result<Object> export(InitialFinanceQuery query) {
        try {
            String type = query.getInitialType();
            if (InitialFinancePartner.TYPE_PAYABLE.equals(type)
                    || InitialFinancePartner.TYPE_RECEIVABLE.equals(type)) {
                return Result.ok(initialFinanceService.listPartner(query));
            }
            return Result.ok(initialFinanceService.listSubject(query));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ══════════════════════════ 内部工具 ══════════════════════════

    /**
     * 把保存请求体归一成列表：**数组**（批量保存，前端「保存期初」/「录入期初」都用这个形状）
     * 与**单个对象**（接口直调一条）都接受 —— 避免调用方因形状不同收到 400。
     */
    private <T> List<T> readRows(JsonNode body, Class<T> type) throws JsonProcessingException {
        if (body == null || body.isNull() || (body.isArray() && body.size() == 0)) {
            throw new IllegalArgumentException("期初数据不能为空");
        }
        if (body.isArray()) {
            List<T> rows = new ArrayList<>();
            for (JsonNode node : body) {
                rows.add(objectMapper.treeToValue(node, type));
            }
            return rows;
        }
        if (body.isObject()) {
            List<T> rows = new ArrayList<>();
            rows.add(objectMapper.treeToValue(body, type));
            return rows;
        }
        throw new IllegalArgumentException("期初数据格式不正确");
    }
}

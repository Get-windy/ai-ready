package cn.aiedge.crm.visit.controller;

import cn.aiedge.crm.visit.entity.VisitPlan;
import cn.aiedge.crm.visit.entity.VisitRecord;
import cn.aiedge.crm.visit.service.VisitPlanService;
import cn.aiedge.crm.visit.service.VisitRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
@RequestMapping("/api/crm/visit")
@Tag(name = "CRM外勤拜访", description = "外勤拜访规划/执行/检视管理")
@RequiredArgsConstructor
public class VisitController {

    private final VisitPlanService visitPlanService;
    private final VisitRecordService visitRecordService;

    // ═══ 拜访计划 ═══

    @Operation(summary = "分页查询拜访计划")
    @SaCheckPermission("crm:visit:list")
    @GetMapping("/plan/page")
    public Page<VisitPlan> planPage(
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "状态(0待执行/1执行中/2已完成/3已取消)") @RequestParam(required = false) Integer status,
            @Parameter(description = "计划日期起") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDateStart,
            @Parameter(description = "计划日期止") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDateEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return visitPlanService.pageList(customerId, salesPersonId, status, planDateStart, planDateEnd,
                pageNum != null ? pageNum : page, pageSize != null ? pageSize : size);
    }

    @Operation(summary = "创建拜访计划")
    @SaCheckPermission("crm:visit:create")
    @PostMapping("/plan")
    public VisitPlan createPlan(@RequestBody VisitPlan plan) {
        if (plan.getPlanNo() == null || plan.getPlanNo().isEmpty()) {
            plan.setPlanNo(visitPlanService.generatePlanNo());
        }
        if (plan.getStatus() == null) {
            plan.setStatus(0);
        }
        visitPlanService.save(plan);
        return plan;
    }

    @Operation(summary = "更新拜访计划")
    @SaCheckPermission("crm:visit:update")
    @PutMapping("/plan/{id}")
    public VisitPlan updatePlan(@PathVariable Long id, @RequestBody VisitPlan plan) {
        plan.setId(id);
        visitPlanService.updateById(plan);
        return plan;
    }

    @Operation(summary = "删除拜访计划")
    @SaCheckPermission("crm:visit:delete")
    @DeleteMapping("/plan/{id}")
    public boolean deletePlan(@PathVariable Long id) {
        return visitPlanService.removeById(id);
    }

    @Operation(summary = "取消拜访计划")
    @SaCheckPermission("crm:visit:cancel")
    @PutMapping("/plan/{id}/cancel")
    public boolean cancelPlan(@PathVariable Long id) {
        return visitPlanService.cancel(id);
    }

    // ═══ 拜访执行 ═══

    @Operation(summary = "分页查询拜访执行记录")
    @SaCheckPermission("crm:visit:list")
    @GetMapping("/record/page")
    public Page<VisitRecord> recordPage(
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "拜访结果(1有意向/2一般/3无意向)") @RequestParam(required = false) Integer result,
            @Parameter(description = "拜访日期起") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateStart,
            @Parameter(description = "拜访日期止") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return visitRecordService.pageList(customerId, salesPersonId, result, visitDateStart, visitDateEnd,
                pageNum != null ? pageNum : page, pageSize != null ? pageSize : size);
    }

    @Operation(summary = "创建拜访执行记录（签到打卡，关联计划自动置为已完成）")
    @SaCheckPermission("crm:visit:create")
    @PostMapping("/record")
    public VisitRecord checkIn(@RequestBody VisitRecord record) {
        return visitRecordService.checkIn(record);
    }

    @Operation(summary = "更新拜访执行记录")
    @SaCheckPermission("crm:visit:update")
    @PutMapping("/record/{id}")
    public VisitRecord updateRecord(@PathVariable Long id, @RequestBody VisitRecord record) {
        record.setId(id);
        visitRecordService.updateById(record);
        return record;
    }

    @Operation(summary = "删除拜访执行记录")
    @SaCheckPermission("crm:visit:delete")
    @DeleteMapping("/record/{id}")
    public boolean deleteRecord(@PathVariable Long id) {
        return visitRecordService.removeById(id);
    }

    // ═══ 拜访检视 ═══

    @Operation(summary = "拜访检视（各结果计数+按日期分组+分页列表）")
    @SaCheckPermission("crm:visit:list")
    @GetMapping("/review/page")
    public Map<String, Object> reviewPage(
            @Parameter(description = "拜访结果(1有意向/2一般/3无意向)") @RequestParam(required = false) Integer result,
            @Parameter(description = "拜访日期起") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateStart,
            @Parameter(description = "拜访日期止") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDateEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return visitRecordService.reviewPage(result, visitDateStart, visitDateEnd,
                pageNum != null ? pageNum : page, pageSize != null ? pageSize : size);
    }

    @Operation(summary = "拜访统计汇总（今日/本周拜访数/计划覆盖率）")
    @SaCheckPermission("crm:visit:view")
    @GetMapping("/stats/summary")
    public Map<String, Object> statsSummary() {
        return visitRecordService.statsSummary();
    }
}

package cn.aiedge.hr.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.entity.HrCandidate;
import cn.aiedge.hr.service.HrCandidateService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 候选人管理控制器
 * 候选人信息维护、面试流程推进、面试评价记录、转入职
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "候选人管理", description = "候选人CRUD+面试流程+面试评价+转入职")
@RestController
@RequestMapping("/api/hr/candidate")
@RequiredArgsConstructor
@SaCheckLogin
public class HrCandidateController {

    private final HrCandidateService candidateService;

    /**
     * 分页查询候选人
     */
    @Operation(summary = "分页查询候选人")
    @GetMapping("/page")
    @RequiresPermission("hr:candidate:list")
    public ApiResponse<Page<HrCandidate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long recruitmentId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<HrCandidate> page = new Page<>(pageNum, pageSize);
        Page<HrCandidate> result = candidateService.pageList(page,
                SecurityUtils.getCurrentTenantId(), recruitmentId, name, status);
        return ApiResponse.ok(result);
    }

    /**
     * 候选人统计（各阶段人数）
     */
    @Operation(summary = "候选人统计")
    @GetMapping("/stat")
    @RequiresPermission("hr:candidate:list")
    public ApiResponse<Map<String, Object>> stat(@RequestParam(required = false) Long recruitmentId) {
        return ApiResponse.ok(candidateService.statistics(recruitmentId));
    }

    /**
     * 获取候选人详情
     */
    @Operation(summary = "获取候选人详情")
    @GetMapping("/{id}")
    @RequiresPermission("hr:candidate:list")
    public ApiResponse<HrCandidate> getById(@PathVariable Long id) {
        HrCandidate candidate = candidateService.getById(id);
        if (candidate == null) {
            return ApiResponse.notFound("候选人不存在");
        }
        return ApiResponse.ok(candidate);
    }

    /**
     * 创建候选人
     */
    @Operation(summary = "创建候选人")
    @PostMapping
    @RequiresPermission("hr:candidate:create")
    public ApiResponse<Long> create(@RequestBody HrCandidate candidate) {
        return ApiResponse.ok("创建成功", candidateService.createCandidate(candidate));
    }

    /**
     * 更新候选人
     */
    @Operation(summary = "更新候选人")
    @PutMapping("/{id}")
    @RequiresPermission("hr:candidate:update")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody HrCandidate candidate) {
        candidate.setId(id);
        candidateService.updateCandidate(candidate);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 更新候选人状态(面试流程推进)
     * 0-简历筛选 → 1-初试 → 2-复试 → 3-终面 → 4-待录用 → 5-已录用 / 6-已拒绝 → 7-已入职(须走转入职)
     */
    @Operation(summary = "更新候选人状态")
    @PutMapping("/{id}/status")
    @RequiresPermission("hr:candidate:update")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        candidateService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功", null);
    }

    /**
     * 记录面试评价
     */
    @Operation(summary = "记录面试评价")
    @PutMapping("/{id}/interview")
    @RequiresPermission("hr:candidate:interview")
    public ApiResponse<Void> recordInterview(
            @PathVariable Long id,
            @RequestParam(required = false) String interviewComment,
            @RequestParam(required = false) Integer rating) {
        candidateService.recordInterview(id, SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(), interviewComment, rating);
        return ApiResponse.ok("面试评价记录成功", null);
    }

    /**
     * 候选人转入职：置「已入职」并创建员工档案（补上招聘→员工档案的断链）
     */
    @Operation(summary = "候选人转入职")
    @PostMapping("/{id}/hire")
    @RequiresPermission("hr:candidate:update")
    public ApiResponse<Long> hire(@PathVariable Long id, @RequestBody(required = false) HrEmployee employee) {
        return ApiResponse.ok("转入职成功", candidateService.hireToEmployee(id, employee));
    }

    /**
     * 删除候选人（已入职不可删）
     */
    @Operation(summary = "删除候选人")
    @DeleteMapping("/{id}")
    @RequiresPermission("hr:candidate:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ApiResponse.ok("删除成功", null);
    }
}

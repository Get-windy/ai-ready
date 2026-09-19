package cn.aiedge.hr.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.hr.entity.HrRecruitment;
import cn.aiedge.hr.service.HrRecruitmentService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招聘管理控制器
 * 招聘职位发布、状态流转
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "招聘管理", description = "招聘职位CRUD+状态更新")
@RestController
@RequestMapping("/api/hr/recruitment")
@RequiredArgsConstructor
@SaCheckLogin
public class HrRecruitmentController {

    private final HrRecruitmentService recruitmentService;

    /**
     * 分页查询招聘职位
     */
    @Operation(summary = "分页查询招聘职位")
    @GetMapping("/page")
    @RequiresPermission("hr:recruitment:list")
    public ApiResponse<Page<HrRecruitment>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<HrRecruitment> page = new Page<>(pageNum, pageSize);
        Page<HrRecruitment> result = recruitmentService.pageList(page,
                SecurityUtils.getCurrentTenantId(), status, positionName, channel, startDate, endDate);
        return ApiResponse.ok(result);
    }

    /**
     * 导出用全量列表
     */
    @Operation(summary = "招聘职位全量列表（导出用）")
    @GetMapping("/list")
    @RequiresPermission("hr:recruitment:list")
    public ApiResponse<List<HrRecruitment>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(recruitmentService.listForExport(status, positionName, channel, startDate, endDate));
    }

    /**
     * 招聘统计
     */
    @Operation(summary = "招聘统计")
    @GetMapping("/stat")
    @RequiresPermission("hr:recruitment:list")
    public ApiResponse<Map<String, Object>> stat(@RequestParam(required = false) Integer status) {
        return ApiResponse.ok(recruitmentService.statistics(status));
    }

    /**
     * 获取招聘职位详情
     */
    @Operation(summary = "获取招聘职位详情")
    @GetMapping("/{id}")
    @RequiresPermission("hr:recruitment:list")
    public ApiResponse<HrRecruitment> getById(@PathVariable Long id) {
        HrRecruitment recruitment = recruitmentService.getById(id);
        if (recruitment == null) {
            return ApiResponse.notFound("招聘职位不存在");
        }
        return ApiResponse.ok(recruitment);
    }

    /**
     * 创建招聘职位
     */
    @Operation(summary = "创建招聘职位")
    @PostMapping
    @RequiresPermission("hr:recruitment:create")
    public ApiResponse<Long> create(@RequestBody HrRecruitment recruitment) {
        return ApiResponse.ok("创建成功", recruitmentService.createRecruitment(recruitment));
    }

    /**
     * 更新招聘职位
     */
    @Operation(summary = "更新招聘职位")
    @PutMapping("/{id}")
    @RequiresPermission("hr:recruitment:update")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody HrRecruitment recruitment) {
        recruitment.setId(id);
        recruitmentService.updateRecruitment(recruitment);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 删除招聘职位
     */
    @Operation(summary = "删除招聘职位")
    @DeleteMapping("/{id}")
    @RequiresPermission("hr:recruitment:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        recruitmentService.removeById(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 更新招聘职位状态
     */
    @Operation(summary = "更新招聘职位状态")
    @PutMapping("/{id}/status")
    @RequiresPermission("hr:recruitment:update")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        recruitmentService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功", null);
    }
}

package cn.aiedge.hr.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.hr.entity.HrRecruitment;
import cn.aiedge.hr.service.HrRecruitmentService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Page<HrRecruitment>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrRecruitment> page = new Page<>(pageNum, pageSize);
        Page<HrRecruitment> result = recruitmentService.pageList(page, tenantId, status, positionName, channel, startDate, endDate);
        return ApiResponse.ok(result);
    }

    /**
     * 获取招聘职位详情
     */
    @Operation(summary = "获取招聘职位详情")
    @GetMapping("/{id}")
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
    public ApiResponse<Long> create(@RequestBody HrRecruitment recruitment) {
        recruitment.setTenantId(SecurityUtils.getCurrentTenantId());
        recruitment.setPublisherId(SecurityUtils.getCurrentUserId());
        recruitment.setApplicantCount(0);
        recruitment.setHiredCount(0);
        recruitmentService.save(recruitment);
        return ApiResponse.ok("创建成功", recruitment.getId());
    }

    /**
     * 更新招聘职位
     */
    @Operation(summary = "更新招聘职位")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody HrRecruitment recruitment) {
        recruitment.setId(id);
        recruitmentService.updateById(recruitment);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 删除招聘职位
     */
    @Operation(summary = "删除招聘职位")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        recruitmentService.removeById(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 更新招聘职位状态
     */
    @Operation(summary = "更新招聘职位状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        recruitmentService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功", null);
    }
}

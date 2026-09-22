package cn.aiedge.department.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.department.dto.DepartmentQueryRequest;
import cn.aiedge.department.dto.DepartmentVO;
import cn.aiedge.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 部门管理控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "部门管理", description = "部门增删改查接口")
@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "分页查询部门")
    @SaCheckPermission("tenant-admin:department:list")
    @GetMapping("/page")
    @RequiresPermission("tenant-admin:department:list")
    public ApiResponse<PageResult<DepartmentVO>> pageList(DepartmentQueryRequest request) {
        return ApiResponse.ok(departmentService.pageList(request));
    }

    @Operation(summary = "获取所有部门列表")
    @GetMapping("/list")
    public ApiResponse<List<DepartmentVO>> listAll() {
        return ApiResponse.ok(departmentService.listAll());
    }

    @Operation(summary = "获取部门下拉选项（含禁用部门，供全站部门下拉使用）")
    @GetMapping("/options")
    public ApiResponse<List<DepartmentVO>> options() {
        // 权限口径与 /list 保持一致：不挂权限注解，仅要求登录。
        // 不要擅自加权限码 —— 会锁死非超管用户，导致无法选择上级部门。
        return ApiResponse.ok(departmentService.listOptions());
    }

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    public ApiResponse<List<DepartmentVO>> getTree() {
        return ApiResponse.ok(departmentService.getTree());
    }

    @Operation(summary = "获取部门详情")
    @SaCheckPermission("tenant-admin:department:query")
    @GetMapping("/{id}")
    @RequiresPermission("tenant-admin:department:query")
    public ApiResponse<DepartmentVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(departmentService.getDetail(id));
    }

    @Operation(summary = "创建部门")
    @SaCheckPermission("tenant-admin:department:create")
    @PostMapping
    @RequiresPermission("tenant-admin:department:create")
    public ApiResponse<Long> create(@RequestBody DepartmentVO request) {
        return ApiResponse.ok(departmentService.create(request));
    }

    @Operation(summary = "更新部门")
    @SaCheckPermission("tenant-admin:department:edit")
    @PutMapping("/{id}")
    @RequiresPermission("tenant-admin:department:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody DepartmentVO request) {
        departmentService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除部门")
    @SaCheckPermission("tenant-admin:department:delete")
    @DeleteMapping("/{id}")
    @RequiresPermission("tenant-admin:department:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除部门")
    @SaCheckPermission("tenant-admin:department:delete")
    @DeleteMapping("/batch")
    @RequiresPermission("tenant-admin:department:delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        departmentService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "更新部门状态")
    @SaCheckPermission("tenant-admin:department:edit")
    @PutMapping("/{id}/status")
    @RequiresPermission("tenant-admin:department:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        departmentService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @Operation(summary = "移动部门")
    @SaCheckPermission("tenant-admin:department:edit")
    @PostMapping("/{id}/move")
    @RequiresPermission("tenant-admin:department:edit")
    public ApiResponse<Void> move(@PathVariable Long id, @RequestBody MoveRequest request) {
        departmentService.move(id, request.getTargetId(), request.getPosition());
        return ApiResponse.ok();
    }

    @Operation(summary = "导出部门")
    @SaCheckPermission("tenant-admin:department:list")
    @GetMapping("/export")
    @RequiresPermission("tenant-admin:department:list")
    public void export(DepartmentQueryRequest request, HttpServletResponse response) throws IOException {
        departmentService.export(request, response);
    }

    /**
     * 移动请求体
     */
    @lombok.Data
    static class MoveRequest {
        private Long targetId;
        private String position;
    }
}

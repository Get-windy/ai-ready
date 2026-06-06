package cn.aiedge.position.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.position.dto.*;
import cn.aiedge.position.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 岗位管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "岗位管理", description = "岗位增删改查接口")
@RestController
@RequestMapping("/api/position")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "分页查询岗位")
    @GetMapping("/page")
    @RequiresPermission("position:list")
    public ApiResponse<PageResult<PositionVO>> pageList(PositionQueryRequest request) {
        return ApiResponse.ok(positionService.pageList(request));
    }

    @Operation(summary = "获取岗位列表")
    @GetMapping("/list")
    public ApiResponse<List<PositionVO>> list() {
        return ApiResponse.ok(positionService.listAll());
    }

    @Operation(summary = "获取岗位详情")
    @GetMapping("/{id}")
    @RequiresPermission("position:query")
    public ApiResponse<PositionVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(positionService.getDetail(id));
    }

    @Operation(summary = "创建岗位")
    @PostMapping
    @RequiresPermission("position:create")
    public ApiResponse<Long> create(@Valid @RequestBody PositionCreateRequest request) {
        Long positionId = positionService.create(request);
        return ApiResponse.success(positionId);
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    @RequiresPermission("position:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PositionUpdateRequest request) {
        request.setId(id);
        positionService.update(request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    @RequiresPermission("position:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "批量删除岗位")
    @DeleteMapping("/batch")
    @RequiresPermission("position:delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        positionService.batchDelete(ids);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/禁用岗位")
    @PutMapping("/{id}/status")
    @RequiresPermission("position:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        positionService.updateStatus(id, status);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "导出岗位")
    @GetMapping("/export")
    @RequiresPermission("position:list")
    public void export(PositionQueryRequest request, HttpServletResponse response) throws IOException {
        positionService.export(request, response);
    }

    @Operation(summary = "根据部门获取岗位")
    @GetMapping("/dept/{deptId}")
    public ApiResponse<List<PositionVO>> getByDeptId(@PathVariable Long deptId) {
        return ApiResponse.ok(positionService.getByDeptId(deptId));
    }

    @Operation(summary = "分配岗位给用户")
    @PostMapping("/assign")
    @RequiresPermission("position:assign")
    public ApiResponse<Void> assignToUser(@RequestParam Long userId,
                                           @RequestBody List<Long> positionIds,
                                           @RequestParam(required = false) Long primaryPositionId) {
        positionService.assignToUser(userId, positionIds, primaryPositionId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "移除用户岗位")
    @DeleteMapping("/remove")
    @RequiresPermission("position:assign")
    public ApiResponse<Void> removeFromUser(@RequestParam Long userId,
                                            @RequestBody List<Long> positionIds) {
        positionService.removeFromUser(userId, positionIds);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "获取岗位下的用户")
    @GetMapping("/{id}/users")
    @RequiresPermission("position:query")
    public ApiResponse<List<Long>> getUsers(@PathVariable Long id) {
        return ApiResponse.ok(positionService.getUserIds(id));
    }
}

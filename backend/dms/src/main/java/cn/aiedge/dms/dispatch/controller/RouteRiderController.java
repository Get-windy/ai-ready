package cn.aiedge.dms.dispatch.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.dispatch.dto.RiderOptionVO;
import cn.aiedge.dms.dispatch.dto.RouteOptionVO;
import cn.aiedge.dms.dispatch.dto.RouteRiderQuery;
import cn.aiedge.dms.dispatch.dto.RouteRiderSaveDTO;
import cn.aiedge.dms.dispatch.dto.RouteRiderVO;
import cn.aiedge.dms.dispatch.service.RouteRiderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 区域分包绑定（线路档案 × 配送员）
 *
 * <p>《智能调度开发文档》§3.4 派单策略「区域分包」：按线路档案绑定固定配送员。
 * 前端在《智能调度》策略配置 Tab 维护；`DispatchService` 在 AREA 策略下消费绑定关系。</p>
 *
 * <p>线路来自《资料 → 配送管理 → 线路》（`erp_route`，只读引用），配送员来自《配送员管理》。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "智能调度-区域分包绑定", description = "线路档案 × 配送员的绑定维护（区域分包派单策略）")
@RestController
@RequestMapping("/api/dms/dispatch/route-rider")
@RequiredArgsConstructor
@SaCheckLogin
public class RouteRiderController {

    private final RouteRiderService routeRiderService;

    @Operation(summary = "绑定列表（分页）")
    @GetMapping("/page")
    @SaCheckPermission("dms:dispatch:view")
    public ApiResponse<Page<RouteRiderVO>> page(RouteRiderQuery query) {
        return ApiResponse.ok(routeRiderService.page(query));
    }

    @Operation(summary = "线路选择器（线路档案，只读）")
    @GetMapping("/route-options")
    @SaCheckPermission("dms:dispatch:view")
    public ApiResponse<List<RouteOptionVO>> routeOptions(
            @Parameter(description = "线路编号/名称关键字") @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(routeRiderService.routeOptions(keyword));
    }

    @Operation(summary = "配送员选择器")
    @GetMapping("/rider-options")
    @SaCheckPermission("dms:dispatch:view")
    public ApiResponse<List<RiderOptionVO>> riderOptions(
            @Parameter(description = "姓名/手机号/编号关键字") @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(routeRiderService.riderOptions(keyword));
    }

    @Operation(summary = "新增绑定")
    @PostMapping
    @SaCheckPermission("dms:dispatch:config")
    public ApiResponse<RouteRiderVO> create(@RequestBody RouteRiderSaveDTO dto) {
        return ApiResponse.ok("绑定成功", routeRiderService.create(dto));
    }

    @Operation(summary = "修改绑定")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:dispatch:config")
    public ApiResponse<RouteRiderVO> update(@PathVariable Long id, @RequestBody RouteRiderSaveDTO dto) {
        return ApiResponse.ok("修改成功", routeRiderService.update(id, dto));
    }

    @Operation(summary = "启用/停用绑定")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:dispatch:config")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object status = body == null ? null : body.get("status");
        routeRiderService.updateStatus(id, status == null ? null : Integer.valueOf(String.valueOf(status)));
        return ApiResponse.ok("状态已更新", null);
    }

    @Operation(summary = "删除绑定")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:dispatch:config")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        routeRiderService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}

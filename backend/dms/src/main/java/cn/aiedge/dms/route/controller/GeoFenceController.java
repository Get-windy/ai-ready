package cn.aiedge.dms.route.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.route.dto.GeoFenceDTO;
import cn.aiedge.dms.route.dto.GeoFenceQueryDTO;
import cn.aiedge.dms.route.service.GeoFenceService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电子围栏档案控制器（配送 → 配送路线 → 路线规划 → 围栏管理）
 *
 * 支持圆形（中心点 + 半径）与多边形（顶点串）两类围栏，
 * 可绑定线路档案 / 运力渠道等业务对象；校验入口见 `POST /api/dms/route/fence-check`。
 *
 * @author AI-Ready Team
 */
@Tag(name = "电子围栏")
@RestController
@RequestMapping("/api/dms/route/fence")
@RequiredArgsConstructor
@SaCheckLogin
public class GeoFenceController {

    private final GeoFenceService geoFenceService;

    @Operation(summary = "围栏分页查询")
    @GetMapping("/page")
    public ApiResponse<Page<GeoFenceDTO>> page(GeoFenceQueryDTO query) {
        return ApiResponse.success(geoFenceService.page(query));
    }

    @Operation(summary = "围栏详情")
    @GetMapping("/{id}")
    public ApiResponse<GeoFenceDTO> detail(@Parameter(description = "围栏ID") @PathVariable Long id) {
        return ApiResponse.success(geoFenceService.detail(id));
    }

    @Operation(summary = "生成下一个围栏编码")
    @GetMapping("/next-code")
    public ApiResponse<String> nextCode() {
        return ApiResponse.success(geoFenceService.nextCode());
    }

    @Operation(summary = "启用围栏下拉（供线路/渠道绑定）")
    @GetMapping("/options")
    public ApiResponse<List<GeoFenceDTO>> options() {
        return ApiResponse.success(geoFenceService.options());
    }

    @Operation(summary = "新增围栏")
    @PostMapping
    public ApiResponse<GeoFenceDTO> create(@RequestBody GeoFenceDTO dto) {
        return ApiResponse.success(geoFenceService.create(dto));
    }

    @Operation(summary = "修改围栏")
    @PutMapping("/{id}")
    public ApiResponse<GeoFenceDTO> update(@Parameter(description = "围栏ID") @PathVariable Long id,
                                           @RequestBody GeoFenceDTO dto) {
        return ApiResponse.success(geoFenceService.update(id, dto));
    }

    @Operation(summary = "删除围栏")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@Parameter(description = "围栏ID") @PathVariable Long id) {
        geoFenceService.remove(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用 / 停用围栏")
    @PutMapping("/{id}/status")
    public ApiResponse<GeoFenceDTO> updateStatus(@Parameter(description = "围栏ID") @PathVariable Long id,
                                                 @Parameter(description = "状态 ENABLED/DISABLED") @RequestParam String status) {
        return ApiResponse.success(geoFenceService.updateStatus(id, status));
    }
}

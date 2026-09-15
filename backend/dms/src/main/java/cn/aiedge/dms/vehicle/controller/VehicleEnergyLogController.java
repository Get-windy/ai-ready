package cn.aiedge.dms.vehicle.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.vehicle.dto.EnergyLogCreateDTO;
import cn.aiedge.dms.vehicle.dto.EnergyLogQuery;
import cn.aiedge.dms.vehicle.dto.EnergyCardCreateDTO;
import cn.aiedge.dms.vehicle.dto.EnergyCardQuery;
import cn.aiedge.dms.vehicle.dto.EnergyCardVO;
import cn.aiedge.dms.vehicle.dto.EnergyLogVO;
import cn.aiedge.dms.vehicle.dto.EnergyStatsVO;
import cn.aiedge.dms.vehicle.service.VehicleEnergyCardService;
import cn.aiedge.dms.vehicle.service.VehicleEnergyLogService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 车辆补能（加油 / 充电 / 加气 / 换电）控制器
 *
 * <p>归属「配送 → 人车管理 → 用车管理」的「车辆补能」Tab（不单独占菜单项）——
 * 补能是一车/一人多条的**从属流水**，没有独立单据生命周期；有单据号的业务（如维保 `WB`）才独立成页。</p>
 *
 * <p>归属模型「车/人二选一」：四轮车传 {@code vehicleId}（按仪表里程核算），
 * 骑手两轮换电传 {@code riderId}（月租套餐按当班里程分摊）。</p>
 */
@Slf4j
@Tag(name = "车辆补能", description = "补能流水（加油/充电/加气/换电）：区间里程与每公里成本自动核算 + 异常标记")
@RestController
@RequestMapping("/api/dms/vehicle/energy")
@RequiredArgsConstructor
@SaCheckLogin
public class VehicleEnergyLogController {

    private final VehicleEnergyLogService energyLogService;
    private final VehicleEnergyCardService energyCardService;

    @Operation(summary = "分页查询补能流水")
    @GetMapping("/page")
    public ApiResponse<IPage<EnergyLogVO>> page(EnergyLogQuery query) {
        return ApiResponse.ok(energyLogService.page(query));
    }

    @Operation(summary = "补能记录详情")
    @GetMapping("/{id}")
    public ApiResponse<EnergyLogVO> detail(@Parameter(description = "补能记录ID") @PathVariable Long id) {
        return ApiResponse.ok(energyLogService.detail(id));
    }

    @Operation(summary = "新增补能记录（自动算区间里程/每公里成本/异常标记）")
    @PostMapping
    public ApiResponse<EnergyLogVO> create(@Valid @RequestBody EnergyLogCreateDTO dto) {
        return ApiResponse.ok("新增成功", energyLogService.create(dto));
    }

    @Operation(summary = "修改补能记录")
    @PutMapping("/{id}")
    public ApiResponse<EnergyLogVO> update(@Parameter(description = "补能记录ID") @PathVariable Long id,
                                           @Valid @RequestBody EnergyLogCreateDTO dto) {
        return ApiResponse.ok("修改成功", energyLogService.update(id, dto));
    }

    @Operation(summary = "删除补能记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "补能记录ID") @PathVariable Long id) {
        energyLogService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "批量删除补能记录")
    @PostMapping("/batch-delete")
    public ApiResponse<Integer> batchDelete(@RequestBody List<Long> ids) {
        return ApiResponse.ok("删除成功", energyLogService.batchDelete(ids));
    }

    @Operation(summary = "能耗报表（汇总 + 油电对比 + 按主体/能源类型分组）")
    @GetMapping("/stats")
    public ApiResponse<EnergyStatsVO> stats(EnergyLogQuery query) {
        return ApiResponse.ok(energyLogService.stats(query));
    }

    // ==================== 补能卡 / 套餐（一卡一车一人） ====================

    @Operation(summary = "分页查询补能卡/套餐")
    @GetMapping("/card/page")
    public ApiResponse<IPage<EnergyCardVO>> cardPage(EnergyCardQuery query) {
        return ApiResponse.ok(energyCardService.page(query));
    }

    @Operation(summary = "补能卡详情")
    @GetMapping("/card/{id}")
    public ApiResponse<EnergyCardVO> cardDetail(@Parameter(description = "补能卡ID") @PathVariable Long id) {
        return ApiResponse.ok(energyCardService.detail(id));
    }

    @Operation(summary = "启用补能卡下拉（补能录入选卡）")
    @GetMapping("/card/options")
    public ApiResponse<List<EnergyCardVO>> cardOptions() {
        return ApiResponse.ok(energyCardService.options());
    }

    @Operation(summary = "新增补能卡/套餐（一卡一车一人）")
    @PostMapping("/card")
    public ApiResponse<EnergyCardVO> cardCreate(@Valid @RequestBody EnergyCardCreateDTO dto) {
        return ApiResponse.ok("新增成功", energyCardService.create(dto));
    }

    @Operation(summary = "修改补能卡/套餐")
    @PutMapping("/card/{id}")
    public ApiResponse<EnergyCardVO> cardUpdate(@Parameter(description = "补能卡ID") @PathVariable Long id,
                                                @Valid @RequestBody EnergyCardCreateDTO dto) {
        return ApiResponse.ok("修改成功", energyCardService.update(id, dto));
    }

    @Operation(summary = "删除补能卡/套餐")
    @DeleteMapping("/card/{id}")
    public ApiResponse<Void> cardDelete(@Parameter(description = "补能卡ID") @PathVariable Long id) {
        energyCardService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启停补能卡")
    @PutMapping("/card/{id}/status")
    public ApiResponse<Void> cardStatus(@Parameter(description = "补能卡ID") @PathVariable Long id,
                                        @RequestParam Integer status) {
        energyCardService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @Operation(summary = "导出补能流水（真实 xlsx）")
    @GetMapping("/export")
    public void export(EnergyLogQuery query, HttpServletResponse response) throws IOException {
        energyLogService.export(query, response);
    }
}

package cn.aiedge.dms.task.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.task.dto.TaskFilterOptionVO;
import cn.aiedge.dms.task.mapper.TaskFilterOptionMapper;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送查询（配发收 → 配送业务 → 配送查询）查询条件下拉
 *
 * <p>只提供「配送司机 / 配送车辆 / 送货员 / 制单人」选择器的候选数据，
 * 供页面选择器使用（对标 ql361 配送查询的放大镜选择器），避免手输 ID。</p>
 */
@Tag(name = "配送查询条件下拉")
@RestController
@RequestMapping("/api/dms/task")
@RequiredArgsConstructor
@SaCheckLogin
public class TaskFilterOptionController {

    private final TaskFilterOptionMapper filterOptionMapper;

    @Operation(summary = "配送查询-查询条件下拉（司机/车辆/送货员/制单人）")
    @GetMapping("/filter-options")
    public ApiResponse<Map<String, Object>> filterOptions() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<TaskFilterOptionVO> riders = filterOptionMapper.selectRiders();
        // 司机与送货员是两个角色，但同源于配送员档案（同一人可分别担任）
        result.put("drivers", riders);
        result.put("deliverymen", riders);
        result.put("vehicles", filterOptionMapper.selectVehicles());
        result.put("creators", filterOptionMapper.selectCreatorNames());
        return ApiResponse.ok(result);
    }
}

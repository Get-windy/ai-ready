package cn.aiedge.dms.task.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.task.dto.DeliveryOutboundFilterVO;
import cn.aiedge.dms.task.mapper.OutboundDeliveryLookupMapper;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 《发货查询》（配发收 → 发货查询，菜单 70156）页面固定项：配送状态 / 配送线路
 *
 * <p>对标 ql361 发货查询的「配送状态、配送线路」两个**页面固定查询项**（不在页面配置弹窗清单内）。
 * 本系统这两项属配送任务（`dms_task`）的执行属性，不在销售出库单上，故：</p>
 * <ol>
 *   <li>本接口按配送执行条件反查命中任务的来源单据号（= 出库单号）；</li>
 *   <li>《发货查询》把它们作为 `outboundNos` 传给 `/api/erp/sale/outbound/page` 做精确过滤。</li>
 * </ol>
 *
 * <p>状态口径与《配送查询》对外三值一致：待配送 = 0/1/2，配送中 = 3/4，已配送 = 5/6
 * （执行态 9 态只在执行侧展示，对外台账统一映射三值，见《配送模块（DMS）开发文档》§5.2）。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "发货查询-配送筛选", description = "按配送状态/线路反查出库单号")
@RestController
@RequestMapping("/api/dms/task")
@RequiredArgsConstructor
@SaCheckLogin
public class OutboundDeliveryFilterController {

    /** 反查上限（超出即截断，前端提示收窄条件，避免「假空列表」） */
    private static final int MAX_LIMIT = 5000;

    /** 对外三值 → 执行态集合 */
    private static final Map<String, List<Integer>> DELIVERY_STATUS_CODES = Map.of(
            "PENDING", List.of(0, 1, 2),
            "DELIVERING", List.of(3, 4),
            "DELIVERED", List.of(5, 6));

    private final OutboundDeliveryLookupMapper lookupMapper;

    @Operation(summary = "按配送状态/线路反查命中的出库单号")
    @GetMapping("/outbound-filter")
    public ApiResponse<DeliveryOutboundFilterVO> outboundFilter(
            @Parameter(description = "配送状态：PENDING-待配送 DELIVERING-配送中 DELIVERED-已配送；空=不限")
            @RequestParam(required = false) String deliveryStatus,
            @Parameter(description = "配送线路档案ID；空=不限")
            @RequestParam(required = false) Long routeId,
            @Parameter(description = "返回上限（缺省 5000）")
            @RequestParam(required = false) Integer limit) {
        List<Integer> statuses = null;
        if (StringUtils.hasText(deliveryStatus)) {
            statuses = DELIVERY_STATUS_CODES.get(deliveryStatus.trim().toUpperCase());
            if (statuses == null) {
                throw BusinessException.badRequest("非法配送状态: " + deliveryStatus);
            }
        }
        int size = limit == null || limit < 1 ? MAX_LIMIT : Math.min(limit, MAX_LIMIT);
        List<String> sourceBillNos = lookupMapper.findSourceBillNos(statuses, routeId, size);

        DeliveryOutboundFilterVO vo = new DeliveryOutboundFilterVO();
        vo.setSourceBillNos(sourceBillNos);
        vo.setMatched(sourceBillNos.size());
        vo.setTruncated(sourceBillNos.size() >= size);
        return ApiResponse.ok(vo);
    }
}

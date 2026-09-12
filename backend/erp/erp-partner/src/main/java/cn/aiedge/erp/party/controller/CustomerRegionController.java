package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.party.entity.CustomerRegion;
import cn.aiedge.erp.party.service.CustomerRegionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 客户区域管理（资料 → 往来单位 → 客户 → 区域管理子标签）
 * <p>
 * 对标 ql361：列表 3 列（区域编号 / 区域名称 / 备注）+ 新增/查询/显示层次结构/打印/导出。
 * </p>
 */
@Tag(name = "客户区域管理", description = "客户区域（区域管理子标签）CRUD")
@RestController
@RequestMapping("/api/erp/customer/region")
@RequiredArgsConstructor
public class CustomerRegionController {

    private final CustomerRegionService customerRegionService;

    @Operation(summary = "分页查询客户区域")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<CustomerRegion>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean showHierarchy,
            @RequestParam(required = false) Long regionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        List<CustomerRegion> all = customerRegionService.getRegionTree(keyword, showHierarchy, regionId);
        int num = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;
        int from = Math.max(0, (num - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<CustomerRegion> records = from < to ? new ArrayList<>(all.subList(from, to)) : new ArrayList<>();

        PageResult<CustomerRegion> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal((long) all.size());
        result.setPageNum((long) num);
        result.setPageSize((long) size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取客户区域列表（不分页）")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<CustomerRegion>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean showHierarchy) {
        return ResponseEntity.ok(ApiResponse.ok(customerRegionService.getRegionTree(keyword, showHierarchy)));
    }

    @Operation(summary = "查询客户区域详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerRegion>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(customerRegionService.getById(id)));
    }

    @Operation(summary = "新增客户区域")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerRegion>> create(@RequestBody Map<String, Object> body) {
        CustomerRegion region = new CustomerRegion();
        region.setRegionName((String) body.get("regionName"));
        region.setRegionCode(resolveCode(body));
        region.setSortOrder(toInt(body.get("sortOrder"), 0));
        region.setStatus(toInt(body.get("status"), 1));
        region.setRemark((String) body.get("remark"));
        applyParent(region, body.get("parentId"));
        customerRegionService.save(region);
        return ResponseEntity.ok(ApiResponse.ok(region));
    }

    @Operation(summary = "更新客户区域")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        CustomerRegion exist = customerRegionService.getById(id);
        if (exist == null) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        CustomerRegion region = new CustomerRegion();
        region.setId(id);
        if (body.get("regionName") != null) {
            region.setRegionName((String) body.get("regionName"));
        }
        if (body.get("regionCode") != null && StringUtils.hasText((String) body.get("regionCode"))) {
            region.setRegionCode((String) body.get("regionCode"));
        }
        if (body.get("sortOrder") != null) {
            region.setSortOrder(toInt(body.get("sortOrder"), 0));
        }
        if (body.get("status") != null) {
            region.setStatus(toInt(body.get("status"), 1));
        }
        if (body.containsKey("remark")) {
            region.setRemark((String) body.get("remark"));
        }
        if (body.containsKey("parentId")) {
            applyParent(region, body.get("parentId"));
        }
        return ResponseEntity.ok(ApiResponse.ok(customerRegionService.updateById(region)));
    }

    @Operation(summary = "删除客户区域（存在子区域时拒绝）")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        CustomerRegion exist = customerRegionService.getById(id);
        if (exist == null) {
            return ResponseEntity.ok(ApiResponse.ok("区域不存在"));
        }
        String blockReason = customerRegionService.checkDeletable(id);
        if (blockReason != null) {
            return ResponseEntity.ok(ApiResponse.ok(blockReason));
        }
        return ResponseEntity.ok(ApiResponse.ok(customerRegionService.removeById(id) ? null : "删除失败"));
    }

    // ── 内部工具 ──

    /** 区域编号：前端未填时按 QY + 3 位序号生成（对标区域编号形态） */
    private String resolveCode(Map<String, Object> body) {
        String provided = (String) body.get("regionCode");
        if (StringUtils.hasText(provided)) {
            return provided;
        }
        LambdaQueryWrapper<CustomerRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CustomerRegion::getRegionCode, "QY");
        wrapper.orderByDesc(CustomerRegion::getRegionCode);
        wrapper.last("LIMIT 1");
        CustomerRegion last = customerRegionService.getOne(wrapper, false);
        int seq = 1;
        if (last != null && last.getRegionCode() != null && last.getRegionCode().length() > 2) {
            try {
                seq = Integer.parseInt(last.getRegionCode().substring(2)) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return "QY" + String.format("%03d", seq);
    }

    /** 上级区域 → parentId / regionLevel（根节点 parentId 固定 0） */
    private void applyParent(CustomerRegion region, Object parentIdRaw) {
        Long parentId = toLong(parentIdRaw);
        if (parentId == null || parentId == 0L) {
            region.setParentId(0L);
            region.setRegionLevel(1);
            return;
        }
        CustomerRegion parent = customerRegionService.getById(parentId);
        region.setParentId(parentId);
        region.setRegionLevel(parent != null && parent.getRegionLevel() != null ? parent.getRegionLevel() + 1 : 1);
    }

    private Integer toInt(Object v, int fallback) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String && StringUtils.hasText((String) v)) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String && StringUtils.hasText((String) v)) {
            try { return Long.valueOf((String) v); } catch (NumberFormatException ignored) { }
        }
        return null;
    }
}

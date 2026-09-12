package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.entity.PartyCategory;
import cn.aiedge.erp.party.service.IPartyCategoryService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "往来单位分类管理", description = "往来单位分类CRUD接口")
@RestController
@RequestMapping("/api/erp/partner/categories")
@RequiredArgsConstructor
public class PartyCategoryController {

    private final IPartyCategoryService partyCategoryService;
    private final PartyService partyService;

    // 类型字符串映射到整数
    private static final Map<String, Integer> TYPE_MAP = Map.of(
        "CUSTOMER", 1,
        "SUPPLIER", 2,
        "LOGISTICS", 3,
        "OTHER", 4
    );

    // 类型 → 分类编码前缀（对标 ql361 分类编码 gysml002 形态）
    private static final Map<Integer, String> CODE_PREFIX = Map.of(
        1, "khml",
        2, "gysml",
        3, "wlgsml",
        4, "wldwml"
    );

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryTree(
            @RequestParam(required = false) String categoryType) {
        Integer typeCode = resolveType(categoryType);
        List<PartyCategory> categories = partyCategoryService.getCategoryTree(typeCode);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @Operation(summary = "根据类型获取分类列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryList(
            @RequestParam(required = false) String categoryType) {
        Integer typeCode = resolveType(categoryType);
        List<PartyCategory> categories = partyCategoryService.getCategoryListByType(typeCode);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody Map<String, Object> body) {
        PartyCategory category = new PartyCategory();
        category.setCategoryName((String) body.get("categoryName"));
        Integer typeCode = resolveType((String) body.get("categoryType"));
        category.setPartyType(typeCode != null ? typeCode : 1);
        category.setCategoryCode(resolveCode(body, category.getPartyType(), null));
        category.setSortOrder(toInt(body.get("sortOrder"), 0));
        category.setStatus(toInt(body.get("status"), 1));
        applyParent(category, body.get("parentId"));
        category.setRemark((String) body.get("remark"));
        boolean ok = partyCategoryService.save(category);
        return ResponseEntity.ok(ApiResponse.ok(ok ? category.getId() : null));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        PartyCategory exist = partyCategoryService.getById(id);
        if (exist == null) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        PartyCategory category = new PartyCategory();
        category.setId(id);
        if (body.get("categoryName") != null) {
            category.setCategoryName((String) body.get("categoryName"));
        }
        if (body.get("sortOrder") != null) {
            category.setSortOrder(toInt(body.get("sortOrder"), 0));
        }
        if (body.get("status") != null) {
            category.setStatus(toInt(body.get("status"), 1));
        }
        if (body.get("remark") != null) {
            category.setRemark((String) body.get("remark"));
        }
        if (body.get("categoryCode") != null && StringUtils.hasText((String) body.get("categoryCode"))) {
            category.setCategoryCode((String) body.get("categoryCode"));
        }
        if (body.containsKey("parentId")) {
            Integer typeCode = exist.getPartyType();
            category.setCategoryCode(resolveCode(body, typeCode, exist.getCategoryCode()));
            applyParent(category, body.get("parentId"));
        }
        return ResponseEntity.ok(ApiResponse.ok(partyCategoryService.updateById(category)));
    }

    @Operation(summary = "删除分类（分类下仍有往来单位时拒绝）")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        PartyCategory exist = partyCategoryService.getById(id);
        if (exist == null) {
            return ResponseEntity.ok(ApiResponse.ok("分类不存在"));
        }
        LambdaQueryWrapper<PartyCategory> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(PartyCategory::getParentId, id).eq(PartyCategory::getDeleted, 0);
        if (partyCategoryService.count(childWrapper) > 0) {
            return ResponseEntity.ok(ApiResponse.ok("请先删除子分类"));
        }
        LambdaQueryWrapper<Party> partyWrapper = new LambdaQueryWrapper<>();
        partyWrapper.eq(Party::getCategoryId, id).eq(Party::getDeleted, 0);
        if (partyService.count(partyWrapper) > 0) {
            return ResponseEntity.ok(ApiResponse.ok("该分类下仍有往来单位，无法删除"));
        }
        return ResponseEntity.ok(ApiResponse.ok(partyCategoryService.removeById(id) ? null : "删除失败"));
    }

    // ── 内部工具 ──

    private Integer resolveType(String categoryType) {
        if (!StringUtils.hasText(categoryType)) {
            return null;
        }
        return TYPE_MAP.getOrDefault(categoryType.toUpperCase(), null);
    }

    /** 分类编码：前端未填时按「类型前缀 + 3 位序号」生成（对标 gysml002） */
    private String resolveCode(Map<String, Object> body, Integer partyType, String currentCode) {
        String provided = (String) body.get("categoryCode");
        if (StringUtils.hasText(provided)) {
            return provided;
        }
        if (StringUtils.hasText(currentCode)) {
            return currentCode;
        }
        String prefix = CODE_PREFIX.getOrDefault(partyType != null ? partyType : 1, "ml");
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PartyCategory::getCategoryCode, prefix);
        wrapper.orderByDesc(PartyCategory::getCategoryCode);
        wrapper.last("LIMIT 1");
        PartyCategory last = partyCategoryService.getOne(wrapper, false);
        int seq = 1;
        if (last != null && last.getCategoryCode() != null
                && last.getCategoryCode().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(last.getCategoryCode().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /** 上级分类 → parentId / level（根节点 parentId 固定为 0，与 buildTree 口径一致） */
    private void applyParent(PartyCategory category, Object parentIdRaw) {
        Long parentId = toLong(parentIdRaw);
        if (parentId == null || parentId == 0L) {
            category.setParentId(0L);
            category.setLevel(1);
            return;
        }
        PartyCategory parent = partyCategoryService.getById(parentId);
        category.setParentId(parentId);
        category.setLevel(parent != null && parent.getLevel() != null ? parent.getLevel() + 1 : 1);
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

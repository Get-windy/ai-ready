package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.entity.WarehouseCategory;
import cn.aiedge.erp.stock.mapper.WarehouseCategoryMapper;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.erp.stock.service.WarehouseCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 仓库分类ServiceImpl
 *
 * @author AI-Ready Team
 * @since 11.156.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class WarehouseCategoryServiceImpl extends ServiceImpl<WarehouseCategoryMapper, WarehouseCategory>
        implements WarehouseCategoryService {

    private final WarehouseMapper warehouseMapper;

    @Override
    public List<WarehouseCategory> getCategoryTree() {
        List<WarehouseCategory> all = this.list(new LambdaQueryWrapper<WarehouseCategory>()
                .orderByAsc(WarehouseCategory::getSortOrder)
                .orderByAsc(WarehouseCategory::getId));

        // 分类下仓库数量（用于树节点右侧计数）
        Map<Long, Integer> countMap = new HashMap<>();
        List<Map<String, Object>> rows = warehouseMapper.selectMaps(new QueryWrapper<Warehouse>()
                .select("category_id AS categoryId, COUNT(*) AS cnt")
                .eq("deleted", 0)
                .groupBy("category_id"));
        for (Map<String, Object> row : rows) {
            Object cid = row.get("categoryId");
            Object cnt = row.get("cnt");
            if (cid != null && cnt != null) {
                countMap.put(Long.valueOf(String.valueOf(cid)), Integer.parseInt(String.valueOf(cnt)));
            }
        }

        return buildTree(all, 0L, countMap);
    }

    private List<WarehouseCategory> buildTree(List<WarehouseCategory> all, Long parentId, Map<Long, Integer> countMap) {
        List<WarehouseCategory> children = new ArrayList<>();
        for (WarehouseCategory node : all) {
            Long pid = node.getParentId() == null ? 0L : node.getParentId();
            if (Objects.equals(pid, parentId)) {
                node.setWarehouseCount(countMap.getOrDefault(node.getId(), 0));
                node.setChildren(buildTree(all, node.getId(), countMap));
                children.add(node);
            }
        }
        return children;
    }

    @Override
    public WarehouseCategory createCategory(WarehouseCategory category) {
        if (!StringUtils.hasText(category.getCategoryName())) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        category.setParentId(parentId);

        WarehouseCategory parent = null;
        if (parentId != 0L) {
            parent = this.getById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("上级分类不存在");
            }
            category.setCategoryLevel(parent.getCategoryLevel() == null ? 2 : parent.getCategoryLevel() + 1);
        } else {
            category.setCategoryLevel(1);
        }

        if (!StringUtils.hasText(category.getCategoryCode())) {
            category.setCategoryCode(generateCategoryCode(parentId, parent));
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        category.setId(null);
        this.save(category);
        return category;
    }

    /** 层级编号：顶级 001/002…；子级 = 父编号 + 3 位序号 */
    private String generateCategoryCode(Long parentId, WarehouseCategory parent) {
        String prefix = parentId == 0L || parent == null || !StringUtils.hasText(parent.getCategoryCode())
                ? ""
                : parent.getCategoryCode();
        long siblings = this.count(new LambdaQueryWrapper<WarehouseCategory>().eq(WarehouseCategory::getParentId, parentId));
        return prefix + String.format("%03d", siblings + 1);
    }

    @Override
    public boolean updateCategory(WarehouseCategory category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        WarehouseCategory exist = this.getById(category.getId());
        if (exist == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        // 不允许把分类挂到自己或自己的子孙下（避免成环）
        Long newParentId = category.getParentId() == null ? exist.getParentId() : category.getParentId();
        if (newParentId != null && newParentId != 0L) {
            if (Objects.equals(newParentId, category.getId())) {
                throw new IllegalArgumentException("上级分类不能是自己");
            }
            if (isDescendant(category.getId(), newParentId)) {
                throw new IllegalArgumentException("上级分类不能是自己的下级");
            }
            WarehouseCategory parent = this.getById(newParentId);
            category.setCategoryLevel(parent == null || parent.getCategoryLevel() == null
                    ? 1 : parent.getCategoryLevel() + 1);
        } else {
            category.setCategoryLevel(1);
        }
        category.setParentId(newParentId == null ? 0L : newParentId);
        return this.updateById(category);
    }

    private boolean isDescendant(Long ancestorId, Long candidateId) {
        List<WarehouseCategory> all = this.list();
        Map<Long, Long> parentMap = all.stream()
                .collect(Collectors.toMap(WarehouseCategory::getId,
                        c -> c.getParentId() == null ? 0L : c.getParentId(), (a, b) -> a));
        Long cursor = candidateId;
        int guard = 0;
        while (cursor != null && cursor != 0L && guard++ < 50) {
            if (Objects.equals(cursor, ancestorId)) {
                return true;
            }
            cursor = parentMap.get(cursor);
        }
        return false;
    }

    @Override
    public boolean removeCategory(Long id) {
        long childCount = this.count(new LambdaQueryWrapper<WarehouseCategory>().eq(WarehouseCategory::getParentId, id));
        if (childCount > 0) {
            throw new IllegalArgumentException("该分类下存在子分类，请先删除子分类");
        }
        long refCount = warehouseMapper.selectCount(new QueryWrapper<Warehouse>()
                .eq("deleted", 0)
                .eq("category_id", id));
        if (refCount > 0) {
            throw new IllegalArgumentException("该分类下存在仓库，无法删除");
        }
        return this.removeById(id);
    }
}

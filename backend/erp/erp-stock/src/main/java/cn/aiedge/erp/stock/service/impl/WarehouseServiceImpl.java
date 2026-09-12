package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.dto.WarehouseQuery;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 仓库ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^ck(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final StockMapper stockMapper;

    @Override
    public List<Warehouse> getWarehouseList() {
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ── 仓库规划列表 ─────────────────────────────────────────────────────────

    private LambdaQueryWrapper<Warehouse> buildWrapper(WarehouseQuery query) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText(query.getKeyword())) {
                String kw = query.getKeyword().trim();
                wrapper.and(w -> w.like(Warehouse::getWarehouseCode, kw)
                        .or().like(Warehouse::getWarehouseName, kw)
                        .or().like(Warehouse::getContactPerson, kw)
                        .or().like(Warehouse::getContactPhone, kw)
                        .or().like(Warehouse::getAddress, kw)
                        .or().like(Warehouse::getEasyCode, kw));
            }
            if (query.getCategoryId() != null && query.getCategoryId() != 0L) {
                wrapper.eq(Warehouse::getCategoryId, query.getCategoryId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(Warehouse::getStatus, query.getStatus());
            } else if (!Boolean.TRUE.equals(query.getShowDisabled())) {
                // 对标默认：不勾选「显示停用」时只展示启用仓库
                wrapper.eq(Warehouse::getStatus, 1);
            }
        }
        wrapper.orderByAsc(Warehouse::getSortOrder).orderByAsc(Warehouse::getId);
        return wrapper;
    }

    /** 按层级展开排序：顶级在前，其子级紧随其后（对标「显示层次结构」） */
    private List<Warehouse> orderByHierarchy(List<Warehouse> all) {
        Map<Long, List<Warehouse>> childrenMap = new LinkedHashMap<>();
        for (Warehouse w : all) {
            Long pid = w.getParentId() == null ? 0L : w.getParentId();
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(w);
        }
        Set<Long> ids = all.stream().map(Warehouse::getId).collect(Collectors.toSet());
        List<Warehouse> ordered = new ArrayList<>(all.size());
        Set<Long> visited = new HashSet<>();
        // 顶级 = parentId 为空/0，或父节点不在当前结果集中
        all.stream()
                .filter(w -> {
                    Long pid = w.getParentId() == null ? 0L : w.getParentId();
                    return pid == 0L || !ids.contains(pid);
                })
                .forEach(w -> appendRecursive(w, childrenMap, ordered, visited));
        // 兜底：环状数据
        all.forEach(w -> {
            if (!visited.contains(w.getId())) {
                appendRecursive(w, childrenMap, ordered, visited);
            }
        });
        return ordered;
    }

    private void appendRecursive(Warehouse node, Map<Long, List<Warehouse>> childrenMap,
                                 List<Warehouse> out, Set<Long> visited) {
        if (node == null || visited.contains(node.getId())) {
            return;
        }
        visited.add(node.getId());
        out.add(node);
        List<Warehouse> children = childrenMap.get(node.getId());
        if (children != null) {
            children.forEach(c -> appendRecursive(c, childrenMap, out, visited));
        }
    }

    @Override
    public Page<Warehouse> pageWarehouse(WarehouseQuery query) {
        int pageNum = query == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query == null || query.getPageSize() < 1 ? 20 : query.getPageSize();

        List<Warehouse> all = this.list(buildWrapper(query));
        List<Warehouse> ordered = query != null && Boolean.TRUE.equals(query.getShowHierarchy())
                ? orderByHierarchy(all)
                : all;

        int total = ordered.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Page<Warehouse> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(new ArrayList<>(ordered.subList(from, to)));
        return page;
    }

    @Override
    public List<Warehouse> listForExport(WarehouseQuery query) {
        List<Warehouse> all = this.list(buildWrapper(query));
        return query != null && Boolean.TRUE.equals(query.getShowHierarchy()) ? orderByHierarchy(all) : all;
    }

    @Override
    public Warehouse getWarehouseDetail(Long id) {
        return this.getById(id);
    }

    @Override
    public Warehouse createWarehouse(Warehouse warehouse) {
        validate(warehouse, null);
        if (!StringUtils.hasText(warehouse.getWarehouseCode())) {
            warehouse.setWarehouseCode(nextCode());
        }
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(1);
        }
        if (warehouse.getSortOrder() == null) {
            warehouse.setSortOrder(0);
        }
        if (warehouse.getParentId() == null) {
            warehouse.setParentId(0L);
        }
        if (warehouse.getCategoryId() == null) {
            warehouse.setCategoryId(0L);
        }
        warehouse.setId(null);
        this.save(warehouse);
        return warehouse;
    }

    @Override
    public boolean updateWarehouse(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        Warehouse exist = this.getById(warehouse.getId());
        if (exist == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        validate(warehouse, warehouse.getId());
        if (warehouse.getParentId() != null && Objects.equals(warehouse.getParentId(), warehouse.getId())) {
            throw new IllegalArgumentException("上级仓库不能是自己");
        }
        return this.updateById(warehouse);
    }

    /** 名称必填 + 编号同租户唯一 */
    private void validate(Warehouse warehouse, Long selfId) {
        if (!StringUtils.hasText(warehouse.getWarehouseName())) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        if (StringUtils.hasText(warehouse.getWarehouseCode())) {
            long dup = this.count(new LambdaQueryWrapper<Warehouse>()
                    .eq(Warehouse::getWarehouseCode, warehouse.getWarehouseCode().trim())
                    .ne(selfId != null, Warehouse::getId, selfId));
            if (dup > 0) {
                throw new IllegalArgumentException("仓库编号已存在：" + warehouse.getWarehouseCode());
            }
        }
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Warehouse exist = this.getById(id);
        if (exist == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        Warehouse update = new Warehouse();
        update.setId(id);
        update.setStatus(status == null ? 1 : status);
        return this.updateById(update);
    }

    @Override
    public boolean removeWarehouse(Long id) {
        Warehouse exist = this.getById(id);
        if (exist == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        long childCount = this.count(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getParentId, id));
        if (childCount > 0) {
            throw new IllegalArgumentException("该仓库下存在下级仓库，无法删除");
        }
        Long stockCount = stockMapper.selectCount(new QueryWrapper<Stock>()
                .eq("warehouse_id", id)
                .ne("quantity", BigDecimal.ZERO));
        if (stockCount != null && stockCount > 0) {
            throw new IllegalArgumentException("该仓库存在库存，无法删除，请改用停用");
        }
        return this.removeById(id);
    }

    @Override
    public String nextCode() {
        List<Warehouse> all = this.list(new LambdaQueryWrapper<Warehouse>().select(Warehouse::getWarehouseCode));
        int max = 0;
        for (Warehouse w : all) {
            if (!StringUtils.hasText(w.getWarehouseCode())) {
                continue;
            }
            Matcher m = CODE_PATTERN.matcher(w.getWarehouseCode().trim());
            if (m.matches()) {
                try {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) {
                    // 超长编号忽略
                }
            }
        }
        return String.format("ck%03d", max + 1);
    }
}

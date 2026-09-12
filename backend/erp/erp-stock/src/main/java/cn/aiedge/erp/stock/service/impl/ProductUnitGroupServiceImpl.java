package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.dto.ProductUnitGroupDTO;
import cn.aiedge.erp.stock.dto.ProductUnitGroupItemDTO;
import cn.aiedge.erp.stock.dto.ProductUnitGroupVO;
import cn.aiedge.erp.stock.entity.ProductUnitGroup;
import cn.aiedge.erp.stock.entity.ProductUnitGroupItem;
import cn.aiedge.erp.stock.mapper.ProductUnitGroupItemMapper;
import cn.aiedge.erp.stock.mapper.ProductUnitGroupMapper;
import cn.aiedge.erp.stock.service.ProductUnitGroupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品单位组ServiceImpl
 *
 * <p>形态对标 ql361 实测（2026-09-11）：「单位组新增编辑」为固定 3 行
 * （小单位 / 中单位 / 大单位 + 换算关系），列表展示单位名串与换算关系串。
 * 小单位是换算基准，换算关系固定 1；中/大单位换算关系必须大于 1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUnitGroupServiceImpl extends ServiceImpl<ProductUnitGroupMapper, ProductUnitGroup>
        implements ProductUnitGroupService {

    /** 对标表单固定行序：小单位 → 中单位 → 大单位 */
    private static final List<String> UNIT_TYPES = Arrays.asList("SMALL", "MEDIUM", "LARGE");

    private final ProductUnitGroupItemMapper itemMapper;

    @Override
    public IPage<ProductUnitGroupVO> getPage(Long tenantId, String keyword, Integer status, int pageNum, int pageSize) {
        Page<ProductUnitGroup> page = new Page<>(pageNum, pageSize);
        IPage<ProductUnitGroup> groupPage = baseMapper.selectPage(page, tenantId, keyword, status);
        IPage<ProductUnitGroupVO> result = new Page<>(groupPage.getCurrent(), groupPage.getSize(), groupPage.getTotal());
        result.setRecords(toVoList(tenantId, groupPage.getRecords()));
        return result;
    }

    @Override
    public ProductUnitGroupVO getDetail(Long tenantId, Long id) {
        ProductUnitGroup group = baseMapper.selectById(id);
        if (group == null || !tenantId.equals(group.getTenantId())) {
            throw new IllegalArgumentException("单位组不存在");
        }
        List<ProductUnitGroupVO> list = toVoList(tenantId, Collections.singletonList(group));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(Long tenantId, ProductUnitGroupDTO dto) {
        List<ProductUnitGroupItemDTO> items = validate(dto);
        ProductUnitGroup group = new ProductUnitGroup();
        group.setTenantId(tenantId);
        group.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        baseMapper.insert(group);
        saveItems(tenantId, group.getId(), items);
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long tenantId, Long id, ProductUnitGroupDTO dto) {
        List<ProductUnitGroupItemDTO> items = validate(dto);
        ProductUnitGroup exist = baseMapper.selectById(id);
        if (exist == null || !tenantId.equals(exist.getTenantId())) {
            throw new IllegalArgumentException("单位组不存在");
        }
        ProductUnitGroup group = new ProductUnitGroup();
        group.setId(id);
        group.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        baseMapper.updateById(group);
        // 明细全量覆盖
        itemMapper.delete(new LambdaQueryWrapper<ProductUnitGroupItem>()
                .eq(ProductUnitGroupItem::getGroupId, id));
        saveItems(tenantId, id, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long tenantId, Long id) {
        ProductUnitGroup exist = baseMapper.selectById(id);
        if (exist == null || !tenantId.equals(exist.getTenantId())) {
            throw new IllegalArgumentException("单位组不存在");
        }
        baseMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<ProductUnitGroupItem>()
                .eq(ProductUnitGroupItem::getGroupId, id));
    }

    @Override
    public void updateStatus(Long tenantId, Long id, Integer status) {
        ProductUnitGroup exist = baseMapper.selectById(id);
        if (exist == null || !tenantId.equals(exist.getTenantId())) {
            throw new IllegalArgumentException("单位组不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值不合法");
        }
        ProductUnitGroup update = new ProductUnitGroup();
        update.setId(id);
        update.setStatus(status);
        baseMapper.updateById(update);
    }

    // ── 私有辅助 ──

    /** 校验并归一化明细：单位名必填、类型固定 3 行、小单位换算关系恒为 1 */
    private List<ProductUnitGroupItemDTO> validate(ProductUnitGroupDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("请至少填写一个小单位");
        }
        Map<String, ProductUnitGroupItemDTO> byType = new LinkedHashMap<>();
        for (ProductUnitGroupItemDTO item : dto.getItems()) {
            if (item == null || item.getUnitName() == null || item.getUnitName().trim().isEmpty()) {
                continue;
            }
            String type = item.getUnitType() == null ? "" : item.getUnitType().trim().toUpperCase();
            if (!UNIT_TYPES.contains(type)) {
                throw new IllegalArgumentException("单位类型不合法");
            }
            if (byType.containsKey(type)) {
                throw new IllegalArgumentException("同一类型的单位只能填写一个");
            }
            byType.put(type, item);
        }
        ProductUnitGroupItemDTO small = byType.get("SMALL");
        if (small == null) {
            throw new IllegalArgumentException("请填写小单位");
        }
        small.setConversionRate(BigDecimal.ONE);
        for (String type : Arrays.asList("MEDIUM", "LARGE")) {
            ProductUnitGroupItemDTO item = byType.get(type);
            if (item == null) {
                continue;
            }
            BigDecimal rate = item.getConversionRate();
            // 对齐对标校验文案与口径（GoodsUnitTemplateEditor：validate min:0, excMin → 必须大于 0）
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("单位关系必须大于0");
            }
        }
        List<ProductUnitGroupItemDTO> normalized = new ArrayList<>();
        for (int i = 0; i < UNIT_TYPES.size(); i++) {
            ProductUnitGroupItemDTO item = byType.get(UNIT_TYPES.get(i));
            if (item == null) {
                continue;
            }
            item.setUnitName(item.getUnitName().trim());
            item.setSortOrder(i + 1);
            normalized.add(item);
        }
        return normalized;
    }

    private void saveItems(Long tenantId, Long groupId, List<ProductUnitGroupItemDTO> items) {
        for (ProductUnitGroupItemDTO d : items) {
            ProductUnitGroupItem item = new ProductUnitGroupItem();
            item.setTenantId(tenantId);
            item.setGroupId(groupId);
            item.setUnitId(d.getUnitId());
            item.setUnitType(d.getUnitType());
            item.setUnitName(d.getUnitName());
            item.setConversionRate(d.getConversionRate() == null ? BigDecimal.ONE : d.getConversionRate());
            item.setSortOrder(d.getSortOrder() == null ? 0 : d.getSortOrder());
            itemMapper.insert(item);
        }
    }

    private List<ProductUnitGroupVO> toVoList(Long tenantId, List<ProductUnitGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = groups.stream().map(ProductUnitGroup::getId).collect(Collectors.toList());
        Map<Long, List<ProductUnitGroupItem>> itemMap = itemMapper.selectList(
                        new LambdaQueryWrapper<ProductUnitGroupItem>()
                                .in(ProductUnitGroupItem::getGroupId, ids)
                                .eq(ProductUnitGroupItem::getTenantId, tenantId)
                                .orderByAsc(ProductUnitGroupItem::getSortOrder)
                                .orderByAsc(ProductUnitGroupItem::getId))
                .stream()
                .collect(Collectors.groupingBy(ProductUnitGroupItem::getGroupId, LinkedHashMap::new, Collectors.toList()));

        List<ProductUnitGroupVO> vos = new ArrayList<>(groups.size());
        for (ProductUnitGroup g : groups) {
            ProductUnitGroupVO vo = new ProductUnitGroupVO();
            vo.setId(g.getId());
            vo.setStatus(g.getStatus());
            List<ProductUnitGroupItem> items = itemMap.getOrDefault(g.getId(), Collections.emptyList());
            List<ProductUnitGroupItemDTO> itemDtos = items.stream().map(it -> {
                ProductUnitGroupItemDTO d = new ProductUnitGroupItemDTO();
                d.setUnitId(it.getUnitId());
                d.setUnitType(it.getUnitType());
                d.setUnitName(it.getUnitName());
                d.setConversionRate(it.getConversionRate());
                d.setSortOrder(it.getSortOrder());
                return d;
            }).collect(Collectors.toList());
            vo.setItems(itemDtos);
            vo.setUnitNames(itemDtos.stream().map(ProductUnitGroupItemDTO::getUnitName)
                    .collect(Collectors.joining(",")));
            vo.setUnitRates(itemDtos.stream().map(d -> formatRate(d.getConversionRate()))
                    .collect(Collectors.joining(":")));
            vos.add(vo);
        }
        return vos;
    }

    /** 换算关系展示：去掉无意义小数尾零（12.00 → 12） */
    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "1";
        }
        return rate.stripTrailingZeros().toPlainString();
    }
}

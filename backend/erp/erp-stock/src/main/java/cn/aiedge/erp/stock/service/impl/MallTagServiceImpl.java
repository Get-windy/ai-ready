package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.dto.MallTagVO;
import cn.aiedge.erp.stock.entity.MallTag;
import cn.aiedge.erp.stock.mapper.MallTagMapper;
import cn.aiedge.erp.stock.service.MallTagService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商城标签ServiceImpl
 */
@Slf4j
@Service
public class MallTagServiceImpl extends ServiceImpl<MallTagMapper, MallTag> implements MallTagService {

    /** 「对应商品」聚合串最多保留字符数，超出截断（对标系统列内同样省略） */
    private static final int PRODUCT_NAMES_MAX_LEN = 400;

    /** 标准标签槽位数量（TAG_1..TAG_20；昵称由用户自定义，槽位本身固定） */
    private static final int STANDARD_SLOT_COUNT = 20;

    @Override
    public List<MallTag> getByTenantId(Long tenantId) {
        return baseMapper.selectByTenantId(tenantId);
    }

    @Override
    public IPage<MallTagVO> getPage(Long tenantId, String keyword, int pageNum, int pageSize) {
        Page<MallTag> page = new Page<>(pageNum, pageSize);
        IPage<MallTag> tagPage = baseMapper.selectPage(page, tenantId, keyword);

        Map<String, TagAgg> aggMap = aggregateProducts(tenantId);

        IPage<MallTagVO> result = new Page<>(tagPage.getCurrent(), tagPage.getSize(), tagPage.getTotal());
        List<MallTagVO> vos = new ArrayList<>(tagPage.getRecords().size());
        for (MallTag tag : tagPage.getRecords()) {
            MallTagVO vo = new MallTagVO();
            vo.setId(tag.getId());
            vo.setTagCode(tag.getTagCode());
            vo.setTagName(tag.getTagName());
            vo.setSortOrder(tag.getSortOrder());
            vo.setStatus(tag.getStatus() == null ? 1 : tag.getStatus());
            // 商品侧存的是槽位编码；历史数据（标签名）回退按名匹配
            TagAgg agg = tag.getTagCode() == null ? null : aggMap.get(tag.getTagCode());
            if (agg == null) {
                agg = aggMap.get(tag.getTagName());
            }
            vo.setProductCount(agg == null ? 0 : agg.count);
            vo.setProductNames(agg == null ? "" : agg.names.toString());
            vos.add(vo);
        }
        result.setRecords(vos);
        return result;
    }

    @Override
    public void updateStatus(Long tenantId, Long id, Integer status) {
        MallTag tag = baseMapper.selectById(id);
        if (tag == null || !tenantId.equals(tag.getTenantId())) {
            throw new IllegalArgumentException("标签不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为 1(启用) 或 0(停用)");
        }
        MallTag update = new MallTag();
        update.setId(id);
        update.setStatus(status);
        baseMapper.updateById(update);
    }

    @Override
    public boolean createTag(MallTag tag) {
        if (tag.getTenantId() == null) {
            throw new IllegalArgumentException("缺少租户");
        }
        if (tag.getTagCode() == null || tag.getTagCode().trim().isEmpty()) {
            tag.setTagCode(nextTagCode(tag.getTenantId()));
        }
        if (tag.getStatus() == null) {
            tag.setStatus(1);
        }
        if (tag.getSortOrder() == null) {
            tag.setSortOrder(slotNo(tag.getTagCode()));
        }
        tag.setId(null);
        return save(tag);
    }

    @Override
    public List<MallTag> ensureStandardSlots(Long tenantId) {
        List<MallTag> tags = baseMapper.selectByTenantId(tenantId);
        Set<String> exists = new HashSet<>();
        for (MallTag t : tags) {
            if (t.getTagCode() != null && !t.getTagCode().isEmpty()) {
                exists.add(t.getTagCode());
            }
        }
        if (exists.size() >= STANDARD_SLOT_COUNT) {
            return tags;
        }
        // 补齐缺失的标准槽位（昵称默认「标签N」）
        for (int i = 1; i <= STANDARD_SLOT_COUNT; i++) {
            String code = "TAG_" + i;
            if (exists.contains(code)) {
                continue;
            }
            MallTag t = new MallTag();
            t.setTenantId(tenantId);
            t.setTagCode(code);
            t.setTagName("标签" + i);
            t.setSortOrder(i - 1);
            t.setStatus(1);
            try {
                save(t);
            } catch (Exception e) {
                // 并发首次访问时可能两个请求同时补同一槽位，唯一索引冲突直接忽略
                log.warn("[商城标签] 槽位 {} 初始化冲突，已忽略：{}", code, e.getMessage());
            }
        }
        return baseMapper.selectByTenantId(tenantId);
    }

    /** 取下一个可用标准槽位编码（TAG_N，N = 当前最大槽位号 + 1） */
    private String nextTagCode(Long tenantId) {
        int max = 0;
        for (MallTag t : baseMapper.selectByTenantId(tenantId)) {
            int n = slotNo(t.getTagCode());
            if (n > max) {
                max = n;
            }
        }
        return "TAG_" + (max + 1);
    }

    /** 从槽位编码取槽位号（TAG_12 → 12；不合法返回 0） */
    private int slotNo(String tagCode) {
        if (tagCode == null) {
            return 0;
        }
        try {
            return Integer.parseInt(tagCode.replace("TAG_", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 按标签槽位聚合商品名。
     * erp_product.mall_tags 存的是逗号分隔的**槽位编码**（TAG_1,TAG_5…）；
     * 历史数据若仍是标签名，则按名匹配（两条路径都以串内值为 key，调用方按 code 查、回退按名查）。
     */
    private Map<String, TagAgg> aggregateProducts(Long tenantId) {
        Map<String, TagAgg> map = new HashMap<>();
        List<Map<String, Object>> rows = baseMapper.selectTaggedProducts(tenantId);
        for (Map<String, Object> row : rows) {
            Object nameObj = row.get("productName");
            Object tagsObj = row.get("mallTags");
            if (tagsObj == null || nameObj == null) {
                continue;
            }
            String productName = String.valueOf(nameObj);
            for (String raw : String.valueOf(tagsObj).split("[,，]")) {
                String tagName = raw.trim();
                if (tagName.isEmpty()) {
                    continue;
                }
                TagAgg agg = map.computeIfAbsent(tagName, k -> new TagAgg());
                agg.count++;
                if (agg.names.length() < PRODUCT_NAMES_MAX_LEN) {
                    if (agg.names.length() > 0) {
                        agg.names.append(',');
                    }
                    agg.names.append(productName);
                }
            }
        }
        return map;
    }

    private static class TagAgg {
        private int count;
        private final StringBuilder names = new StringBuilder();
    }
}

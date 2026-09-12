package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.CustomerRegion;
import cn.aiedge.erp.party.mapper.CustomerRegionMapper;
import cn.aiedge.erp.party.service.CustomerRegionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerRegionServiceImpl extends ServiceImpl<CustomerRegionMapper, CustomerRegion>
        implements CustomerRegionService {

    @Override
    public List<CustomerRegion> getRegionTree(String keyword, Boolean showHierarchy) {
        return getRegionTree(keyword, showHierarchy, null);
    }

    @Override
    public List<CustomerRegion> getRegionTree(String keyword, Boolean showHierarchy, Long regionId) {
        LambdaQueryWrapper<CustomerRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerRegion::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CustomerRegion::getRegionCode, keyword)
                    .or().like(CustomerRegion::getRegionName, keyword)
                    .or().like(CustomerRegion::getRemark, keyword));
        }
        wrapper.orderByAsc(CustomerRegion::getSortOrder).orderByAsc(CustomerRegion::getId);
        List<CustomerRegion> all = this.list(wrapper);

        // 左侧「地区分类」树选中节点 → 仅保留该节点及其全部后代
        if (regionId != null && regionId != 0L) {
            all = filterSubtree(all, regionId);
        }

        // 「显示层次结构」= 以树返回；否则平铺（与对标「显示层次结构」勾选联动）
        if (Boolean.TRUE.equals(showHierarchy)) {
            return buildTree(all);
        }
        return all;
    }

    /** 保留 regionId 节点及其全部后代（保留原排序） */
    private List<CustomerRegion> filterSubtree(List<CustomerRegion> all, Long regionId) {
        Set<Long> keep = new HashSet<>();
        keep.add(regionId);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (CustomerRegion node : all) {
                if (node.getParentId() != null && keep.contains(node.getParentId()) && keep.add(node.getId())) {
                    grew = true;
                }
            }
        }
        List<CustomerRegion> filtered = new ArrayList<>();
        for (CustomerRegion node : all) {
            if (keep.contains(node.getId())) {
                node.setChildren(null);
                filtered.add(node);
            }
        }
        return filtered;
    }

    @Override
    public String checkDeletable(Long id) {
        LambdaQueryWrapper<CustomerRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerRegion::getParentId, id);
        wrapper.eq(CustomerRegion::getDeleted, 0);
        if (this.count(wrapper) > 0) {
            return "请先删除子区域";
        }
        return null;
    }

    /** 平铺列表 → 树（parentId 为 null/0 视为根） */
    private List<CustomerRegion> buildTree(List<CustomerRegion> all) {
        List<CustomerRegion> roots = new ArrayList<>();
        for (CustomerRegion node : all) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            } else {
                for (CustomerRegion parent : all) {
                    if (parent.getId().equals(node.getParentId())) {
                        if (parent.getChildren() == null) {
                            parent.setChildren(new ArrayList<>());
                        }
                        parent.getChildren().add(node);
                        break;
                    }
                }
            }
        }
        return roots;
    }
}

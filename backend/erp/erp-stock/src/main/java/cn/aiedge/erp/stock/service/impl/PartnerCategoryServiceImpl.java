package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerCategory;
import cn.aiedge.erp.stock.mapper.PartnerCategoryMapper;
import cn.aiedge.erp.stock.service.PartnerCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerCategoryServiceImpl extends ServiceImpl<PartnerCategoryMapper, PartnerCategory>
        implements PartnerCategoryService {

    @Override
    public List<PartnerCategory> getTree(String categoryType) {
        QueryWrapper<PartnerCategory> wrapper = new QueryWrapper<PartnerCategory>()
                .eq("deleted", 0);
        if (categoryType != null) {
            wrapper.eq("category_type", categoryType);
        }
        wrapper.orderByAsc("sort_order");
        List<PartnerCategory> all = list(wrapper);
        return buildTree(all, 0L);
    }

    private List<PartnerCategory> buildTree(List<PartnerCategory> all, Long parentId) {
        List<PartnerCategory> children = all.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .peek(c -> c.setChildren(buildTree(all, c.getId())))
                .collect(Collectors.toList());
        return children.isEmpty() ? new ArrayList<>() : children;
    }
}

package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.PartyCategory;
import cn.aiedge.erp.party.mapper.PartyCategoryMapper;
import cn.aiedge.erp.party.service.IPartyCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartyCategoryServiceImpl extends ServiceImpl<PartyCategoryMapper, PartyCategory> implements IPartyCategoryService {

    @Override
    public List<PartyCategory> getCategoryTree(Integer categoryType) {
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyCategory::getPartyType, categoryType)
               .eq(PartyCategory::getDeleted, 0)
               .orderByAsc(PartyCategory::getSortOrder);

        List<PartyCategory> categories = list(wrapper);

        // 构建树形结构
        return buildTree(categories, 0L);
    }

    @Override
    public List<PartyCategory> getCategoryListByType(Integer categoryType) {
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyCategory::getPartyType, categoryType)
               .eq(PartyCategory::getDeleted, 0)
               .orderByAsc(PartyCategory::getSortOrder);

        return list(wrapper);
    }

    private List<PartyCategory> buildTree(List<PartyCategory> allCategories, Long parentId) {
        return allCategories.stream()
                .filter(cat -> parentId.equals(cat.getParentId()))
                .sorted(Comparator.comparing(PartyCategory::getSortOrder))
                .peek(cat -> cat.setChildren(buildTree(allCategories, cat.getId())))
                .collect(Collectors.toList());
    }
}
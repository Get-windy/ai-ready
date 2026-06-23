package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.PartyCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPartyCategoryService extends IService<PartyCategory> {

    /**
     * 根据类型获取分类树
     */
    List<PartyCategory> getCategoryTree(Integer categoryType);

    /**
     * 根据类型获取分类列表
     */
    List<PartyCategory> getCategoryListByType(Integer categoryType);
}
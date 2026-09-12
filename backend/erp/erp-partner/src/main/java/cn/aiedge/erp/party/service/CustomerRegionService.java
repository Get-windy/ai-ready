package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.CustomerRegion;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 客户区域服务（区域管理子标签）
 */
public interface CustomerRegionService extends IService<CustomerRegion> {

    /** 区域树（含根节点「全部区域」下的全部层级） */
    List<CustomerRegion> getRegionTree(String keyword, Boolean showHierarchy);

    /**
     * 区域树 + 按左侧「地区分类」树选中节点过滤（对该节点及其全部后代生效）
     *
     * @param regionId 选中区域 id；为 null/0 时等价于根节点「全部」
     */
    List<CustomerRegion> getRegionTree(String keyword, Boolean showHierarchy, Long regionId);

    /** 删除前置校验：存在子区域时返回提示语，可删除时返回 null */
    String checkDeletable(Long id);
}

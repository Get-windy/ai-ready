package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.MallTagVO;
import cn.aiedge.erp.stock.entity.MallTag;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商城标签Service接口
 */
public interface MallTagService extends IService<MallTag> {

    /**
     * 获取租户下的所有标签
     */
    List<MallTag> getByTenantId(Long tenantId);

    /**
     * 分页查询标签（含「对应商品」聚合）
     */
    IPage<MallTagVO> getPage(Long tenantId, String keyword, int pageNum, int pageSize);

    /**
     * 启用/停用标签（对标 Tab3 行内「停用」）
     */
    void updateStatus(Long tenantId, Long id, Integer status);

    /**
     * 创建标签：槽位编码由系统按 TAG_N 递增分配（标准槽位），tagName 为用户昵称
     */
    boolean createTag(MallTag tag);

    /**
     * 确保该租户已初始化 20 个标准标签槽位（TAG_1..TAG_20，默认昵称「标签N」），
     * 返回初始化后的标签列表（按 sortOrder 升序）。
     */
    List<MallTag> ensureStandardSlots(Long tenantId);
}

package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.MallTag;
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
}

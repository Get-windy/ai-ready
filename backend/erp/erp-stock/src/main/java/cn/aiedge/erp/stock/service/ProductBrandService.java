package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductBrand;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品品牌Service接口
 */
public interface ProductBrandService extends IService<ProductBrand> {

    /**
     * 分页查询品牌列表
     */
    IPage<ProductBrand> getPage(Long tenantId, String keyword, int pageNum, int pageSize);

    /**
     * 获取租户下所有品牌
     */
    List<ProductBrand> getByTenantId(Long tenantId);
}

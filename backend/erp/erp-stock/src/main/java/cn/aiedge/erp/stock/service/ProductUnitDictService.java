package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductUnitDict;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品单位字典Service接口
 */
public interface ProductUnitDictService extends IService<ProductUnitDict> {

    IPage<ProductUnitDict> getPage(Long tenantId, String keyword, int pageNum, int pageSize);

    List<ProductUnitDict> getByTenantId(Long tenantId);
}

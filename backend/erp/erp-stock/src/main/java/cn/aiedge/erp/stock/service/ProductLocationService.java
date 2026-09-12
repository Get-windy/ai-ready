package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.ProductLocationQuery;
import cn.aiedge.erp.stock.dto.ProductLocationSetDTO;
import cn.aiedge.erp.stock.dto.ProductLocationVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 商品货位设置（商品 × 仓库 → 推荐货位）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ProductLocationService {

    /** 分页查询：以商品为主体，带出所选仓库下的推荐货位 */
    IPage<ProductLocationVO> page(ProductLocationQuery query);

    /** 列表查询（不分页，导出用） */
    List<ProductLocationVO> list(ProductLocationQuery query);

    /** 为若干商品设置推荐货位（同一仓库），返回成功条数 */
    int setLocation(ProductLocationSetDTO dto);

    /** 解除若干商品的推荐货位绑定（同一仓库），返回成功条数 */
    int removeLocation(ProductLocationSetDTO dto);
}

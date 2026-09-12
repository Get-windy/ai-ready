package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.CustomerProductPrice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户指定价设置 Mapper（复用 erp_customer_product_price 表）
 */
@Mapper
public interface CustomerProductPriceMapper extends BaseMapper<CustomerProductPrice> {
}

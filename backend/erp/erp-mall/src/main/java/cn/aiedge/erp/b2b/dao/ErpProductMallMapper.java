package cn.aiedge.erp.b2b.dao;

import cn.aiedge.erp.b2b.dao.ErpProductMall;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * erp_product 表 Mapper（商城只读）
 * 查询 v_mall_product 视图，数据源为 erp_product + erp_stock
 */
@Mapper
public interface ErpProductMallMapper extends BaseMapper<ErpProductMall> {
}

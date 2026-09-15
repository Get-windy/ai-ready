package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * erp_product 表写入 Mapper（商城商品上架页编辑弹窗保存目标）。
 *
 * <p>只用于 UPDATE 既有权威商品行；视图 {@code v_mall_product}（{@link ErpProductMallMapper}）不可写，
 * 已废弃的 {@code mall_product} 表（{@code MallProductMapper}）不可作为保存目标。</p>
 */
@Mapper
public interface ErpProductWriteMapper extends BaseMapper<ErpProductWrite> {
}

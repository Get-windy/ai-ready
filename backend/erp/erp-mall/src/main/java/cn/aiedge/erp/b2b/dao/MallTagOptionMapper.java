package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商城商品标签只读 Mapper（{@code erp_mall_tag}）。
 *
 * <p>权威写入方在 erp-stock（管理端「商品上架 → 商品标签」），本模块只读，见 {@link MallTagOption}。</p>
 */
@Mapper
public interface MallTagOptionMapper extends BaseMapper<MallTagOption> {
}

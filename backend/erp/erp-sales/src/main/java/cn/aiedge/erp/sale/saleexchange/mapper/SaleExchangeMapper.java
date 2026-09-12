package cn.aiedge.erp.sale.saleexchange.mapper;

import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleExchangeMapper extends BaseMapper<SaleExchange> {

    /**
     * 取当天最后一张单据编号（号段自增用）。
     *
     * <p>刻意绕过 {@code @TableLogic} 逻辑删除过滤：已软删除的单据仍占用编号唯一索引，
     * 若按 deleted=0 过滤会导致号段回退并撞唯一键。</p>
     */
    @Select("SELECT exchange_no FROM erp_sale_exchange WHERE exchange_no LIKE CONCAT(#{prefix}, '%') "
            + "ORDER BY exchange_no DESC LIMIT 1")
    String selectLastExchangeNo(@Param("prefix") String prefix);
}

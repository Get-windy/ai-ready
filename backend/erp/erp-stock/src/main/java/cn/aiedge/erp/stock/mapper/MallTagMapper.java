package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.MallTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商城标签Mapper
 */
public interface MallTagMapper extends BaseMapper<MallTag> {

    /**
     * 查询租户下的所有标签（按排序）
     */
    @Select("SELECT * FROM erp_mall_tag WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<MallTag> selectByTenantId(Long tenantId);
}

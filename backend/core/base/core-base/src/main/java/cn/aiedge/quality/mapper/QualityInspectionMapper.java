package cn.aiedge.quality.mapper;

import cn.aiedge.quality.entity.QualityInspection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityInspectionMapper extends BaseMapper<QualityInspection> {

    /**
     * 按检验记录ID查询来源业务单号（仅选单列，避免质检单实体继承 BaseEntity
     * 后对缺失 create_by/update_by/version 列的表执行全列查询时报错）
     */
    @Select("SELECT biz_no FROM quality_inspection WHERE id = #{id} AND deleted = 0")
    String selectBizNoById(@Param("id") Long id);

    /**
     * 按检验记录ID查询处置定位信息（产品/批次/仓库，仅选部分列，供缺陷处置事件携带，
     * 规避质检单表缺失 BaseEntity 审计列导致的全列查询报错）
     */
    @Select("SELECT product_id, batch_no, warehouse_id, warehouse_name FROM quality_inspection WHERE id = #{id} AND deleted = 0")
    QualityInspection selectDispositionInfo(@Param("id") Long id);
}
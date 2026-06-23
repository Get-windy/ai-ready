package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseDemand;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 采购需求Mapper接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseDemandMapper extends BaseMapper<PurchaseDemand> {
    
    /**
     * 分页查询采购需求
     * 
     * @param page 分页参数
     * @param wrapper 查询条件
     * @return 分页结果
     */
    IPage<PurchaseDemand> selectPageVo(Page<PurchaseDemand> page, @Param(Constants.WRAPPER) Wrapper<PurchaseDemand> wrapper);
    
    /**
     * 根据物料ID查询最新的采购需求
     * 
     * @param materialId 物料ID
     * @return 采购需求列表
     */
    @Select("SELECT * FROM purchase_demand WHERE material_id = #{materialId} AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT 5")
    List<PurchaseDemand> selectLatestByMaterialId(@Param("materialId") Long materialId);
    
    /**
     * 获取采购需求统计信息
     * 
     * @param tenantId 租户ID
     * @return 统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as draft_count, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as submitted_count, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as approved_count, " +
            "SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as converted_count, " +
            "SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as cancelled_count " +
            "FROM purchase_demand WHERE tenant_id = #{tenantId} AND deleted = 0")
    Map<String, Object> selectDemandStatistics(@Param("tenantId") Long tenantId);
    
    /**
     * 根据需求编号查询采购需求
     * 
     * @param demandNo 需求编号
     * @return 采购需求
     */
    @Select("SELECT * FROM purchase_demand WHERE demand_no = #{demandNo} AND deleted = 0")
    PurchaseDemand selectByDemandNo(@Param("demandNo") String demandNo);
    
    /**
     * 更新采购需求状态
     * 
     * @param id 需求ID
     * @param status 状态值
     * @return 更新条数
     */
    @Select("UPDATE purchase_demand SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 批量更新采购需求状态为已转为询价
     * 
     * @param demandIds 需求ID列表
     * @param inquiryId 询价单ID
     * @return 更新条数
     */
    @Select("<script>" +
            "UPDATE purchase_demand SET status = 4, inquiry_id = #{inquiryId}, update_time = NOW() " +
            "WHERE id IN " +
            "<foreach collection='demandIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchConvertToInquiry(@Param("demandIds") List<Long> demandIds, @Param("inquiryId") Long inquiryId);
}
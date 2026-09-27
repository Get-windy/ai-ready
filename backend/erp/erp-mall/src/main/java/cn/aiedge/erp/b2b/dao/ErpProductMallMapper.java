package cn.aiedge.erp.b2b.dao;

import cn.aiedge.erp.b2b.dao.ErpProductMall;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * erp_product 表 Mapper（商城只读）
 * 查询 v_mall_product 视图，数据源为 erp_product + erp_stock
 */
@Mapper
public interface ErpProductMallMapper extends BaseMapper<ErpProductMall> {

    /**
     * 按分类 id 批量取**权威分类名**（来自分类树 {@code erp_product_category}）。
     *
     * <p><b>为什么不能直接用商品的 {@code category_name}</b>：视图里的
     * {@code category_name = COALESCE(p.mall_category_name, p.category)} —— 这是
     * **商品表上的冗余列**，实测经常为空（真库 6 个商品两列皆空），
     * 而它们挂的 {@code category_id} 在分类树里**是有名字的**
     * （如 {@code 2072844513319059457 = 饮品原料}）。直接用商品列的结果是
     * 界面显示一长串裸 id / 前端被迫兜成「未分类」—— 典型的"名称没同步"而非"没数据"。
     * 所以这里回到分类树取权威名称。</p>
     *
     * <p>⚠️ 手写 SQL 不会被租户插件改写（见《MyBatis 两个静默陷阱》），
     * 故 {@code tenant_id} 条件**显式写死**在语句里。</p>
     *
     * @param ids 分类 id 列表（非空；调用方需先判空，避免 IN () 语法错）
     */
    @Select("<script>SELECT id::text AS category_id, category_name, category_level, parent_id::text AS parent_id, sort_order "
            + "FROM erp_product_category "
            + "WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = 1 "
            // ⚠️ 必须 id::text：入参是 String，而 id 是 bigint，PG 没有
            //    bigint = character varying 操作符 ⇒ 直接比会整条查询 500（真机实踩）
            + "AND id::text IN <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectCategoryNames(@Param("tenantId") Long tenantId,
                                                  @Param("ids") List<String> ids);
}

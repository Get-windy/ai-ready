package cn.aiedge.erp.finance.initial.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 库存期初金额只读查询 Mapper —— 财务期初「存货对平检查」的库存侧取数。
 *
 * <p><b>跨模块说明</b>：库存期初台账 {@code erp_stock.is_initial = 1} 归属 erp-stock 模块，
 * 而 erp-finance 的 pom **不依赖** erp-stock（依赖方向不允许反向）→ 本处按
 * 《库存期初开发文档》§7.4 路线 A 的同一口径，用**只读手写 SQL** 直接取合计，
 * **不新增冗余列、不改写任何一侧数据**（本检查只报数）。</p>
 *
 * <p><b>为什么金额是 SUM(quantity × unit_price)</b>：库存期初的金额在其中是派生值、
 * **不落库**（见 {@code cn.aiedge.erp.stock.mapper.InitialStockQueryMapper} 的口径：
 * 避免数量/单价改了金额没改），故此处按同一公式现算。</p>
 *
 * <p><b>租户条件必须自己拼</b>：本 Mapper 用
 * {@code @InterceptorIgnore(tenantLine = "true")} 关闭自动租户注入（避免对未加别名的表重复注入），
 * 因此 SQL 里显式带 {@code tenant_id} 条件；传 null 表示「无租户上下文 → 平台级可见」，
 * 与租户拦截器 {@code AiReadyTenantLineInnerInterceptor} 的跳过语义一致。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface InitialStockOpeningMapper {

    /**
     * 库存期初金额合计 = Σ(期初数量 × 期初成本单价)。
     *
     * <p>金额口径为 {@code numeric(18,2)}，无行时返回 0（不是 null），便于直接参与差额计算。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(SUM(s.quantity * s.unit_price), 0)"
            + " FROM erp_stock s"
            + " WHERE s.deleted = 0 AND s.is_initial = 1"
            + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))")
    BigDecimal sumOpeningAmount(@Param("tenantId") Long tenantId);

    /** 库存期初行数（用于页面提示「库存侧共 N 行」，0 行时说明库存期初还没录） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*)"
            + " FROM erp_stock s"
            + " WHERE s.deleted = 0 AND s.is_initial = 1"
            + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))")
    long countOpeningRows(@Param("tenantId") Long tenantId);
}

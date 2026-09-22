package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.dto.NameRow;
import cn.aiedge.agreement.entity.Agreement;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 协议主档 Mapper。
 *
 * <p>{@code agreement} 已登记进 {@code IGNORE_TENANT_TABLES}（裁定⑥）：租户拦截器对本表
 * <b>不注入任何条件</b>，可见性由 {@code AgreementVisibility} 显式构造 —— 这是刻意的设计，
 * 见该类的注释。</p>
 */
@Mapper
public interface AgreementMapper extends BaseMapper<Agreement> {

    /**
     * 批量取往来单位名称（仅用于展示）。
     *
     * <p><b>为什么必须 {@code @InterceptorIgnore(tenantLine = "true")}</b>：
     * 一份协议的两端属于**不同租户**，而 {@code biz_party} 是租户维度表。
     * 不跳过注入时，会话租户只能读到"本租户的往来单位"，**另一端的名称永远查不到** ——
     * 表现为协议列表里对方主体显示"（未指定主体）"（SQL 不报错、极难定位）。
     * 名称只是展示，不构成访问控制；真正的访问控制在 {@code AgreementVisibility}。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
            + "SELECT id AS id, party_name AS name FROM biz_party WHERE deleted = 0 AND id IN "
            + "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>"
            + "</script>")
    List<NameRow> selectPartyNames(@Param("ids") Collection<Long> ids);

    /**
     * 批量取租户名称（仅用于展示）。{@code sys_tenant} 本身就在忽略清单里，无需再跳过。
     */
    @Select("<script>"
            + "SELECT id AS id, tenant_name AS name FROM sys_tenant WHERE deleted = 0 AND id IN "
            + "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>"
            + "</script>")
    List<NameRow> selectTenantNames(@Param("ids") Collection<Long> ids);
}

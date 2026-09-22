package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTemplate;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 契约模板主档 Mapper。
 *
 * <p><b>⚠️ 本表是"两级混合归属"</b>：平台模板 {@code tenant_id = 0}、租户模板 {@code tenant_id = 本租户}
 * ⇒ <b>不能</b>靠租户拦截器自动过滤（那会让平台模板对租户全部消失）。故关闭自动注入，
 * 可见性显式写在 {@code AgreementTemplateVisibility}（唯一构造处）：
 * 平台可读全部（合规抽查读，§13.9），租户只读"自己的 + 平台模板"。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementTemplateMapper extends BaseMapper<AgreementTemplate> {
}

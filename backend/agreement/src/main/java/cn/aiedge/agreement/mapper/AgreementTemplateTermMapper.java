package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTemplateTerm;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板预填条款 Mapper。取值前必须先经模板主档的可见性判定
 * （{@code AgreementTemplateVisibility}），子表自身不带归属条件。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementTemplateTermMapper extends BaseMapper<AgreementTemplateTerm> {
}

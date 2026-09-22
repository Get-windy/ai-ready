package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTemplateNarrative;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板预填文字条款 Mapper。
 *
 * <p>⚠️ 同 {@link AgreementTemplateSettingMapper}：模板里的文字**不是**协议文字版，
 * 只有写进正式版本、双方确认之后才算数；设定版与文字版的读取入口都不读模板。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementTemplateNarrativeMapper extends BaseMapper<AgreementTemplateNarrative> {
}

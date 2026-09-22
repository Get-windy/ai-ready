package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTemplateSetting;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板预填设定 Mapper。
 *
 * <p>⚠️ <b>{@code AgreementRuntime} 不得依赖本 Mapper</b>：设定版的读取入口只认
 * {@code agreement_setting}（双方签署的那一版），模板项**不是默认值**（㉜）。
 * 这一条有单测钉死（反射断言 + 调用路径 {@code verify(never())}）。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementTemplateSettingMapper extends BaseMapper<AgreementTemplateSetting> {
}

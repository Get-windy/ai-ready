package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementSettingDef;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段元数据 Mapper。
 *
 * <p>本表是**平台级参考数据**（{@code tenant_id} 恒为 0，全租户共用同一份"有哪些设定项"），
 * 与 {@code sys_permission} / {@code sys_module_permission} 同性质：按会话租户过滤会让
 * 所有租户都读不到元数据，表现为"界面上一项都显示不出来"。故同样以 Mapper 级
 * {@code @InterceptorIgnore(tenantId = "true")} 关掉自动注入，读写条件自己写。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementSettingDefMapper extends BaseMapper<AgreementSettingDef> {
}

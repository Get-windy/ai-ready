package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTermOption;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台条款字典 Mapper。
 *
 * <p>字典是**平台级参考数据**（只有 {@code agreement:platform:term-option:manage} 能写，
 * 该码经最长前缀映射落到「系统」模块 ⇒ 只有系统租户能写，见裁定⑤）。
 * 本表同属协议四表，已登记进 {@code IGNORE_TENANT_TABLES}。</p>
 */
@Mapper
public interface AgreementTermOptionMapper extends BaseMapper<AgreementTermOption> {
}

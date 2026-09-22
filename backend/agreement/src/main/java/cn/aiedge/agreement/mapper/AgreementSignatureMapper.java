package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementSignature;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签署留痕 Mapper。
 *
 * <p>本表已登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}（裁定⑥）：
 * "谁能看这份签署记录"由父协议的 {@code AgreementVisibility} 显式判定，
 * 租户拦截器不参与 —— 协议天然跨租户，两端的签署记录必须双方都看得到（否则双签无从核对）。</p>
 */
@Mapper
public interface AgreementSignatureMapper extends BaseMapper<AgreementSignature> {
}

package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTermination;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 终止留痕 Mapper。
 *
 * <p>本表已登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}（裁定⑥）：
 * 终止记录要双方都看得到（否则"对方什么时候终止的"要靠猜），
 * 可见性由父协议的 {@code AgreementVisibility} 显式判定。</p>
 */
@Mapper
public interface AgreementTerminationMapper extends BaseMapper<AgreementTermination> {
}

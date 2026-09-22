package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementInvite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 唯一送达邀请 Mapper。
 *
 * <p>本表已登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}（与协议主档同口径，裁定⑥）：
 * 租户拦截器在它上面**不注入任何条件**，"谁能打开这份邀请"完全由
 * {@code AgreementInviteGuard} 的五绑定判定 + 父协议的
 * {@code AgreementVisibility} 显式判定，不许在这里手写租户条件。</p>
 */
@Mapper
public interface AgreementInviteMapper extends BaseMapper<AgreementInvite> {
}

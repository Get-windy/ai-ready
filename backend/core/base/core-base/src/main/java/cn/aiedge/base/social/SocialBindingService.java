package cn.aiedge.base.social;

import cn.aiedge.base.entity.SysUserSocialBinding;
import cn.aiedge.base.mapper.SysUserSocialBindingMapper;
import cn.aiedge.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 三方账号绑定服务。
 *
 * <p>绑定关系是「系统账号 ↔ 三方身份」的一对一映射，用于两件事：
 * ① 扫码登录时按三方身份反查系统账号；② 在个人中心展示/管理绑定。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialBindingService {

    private final SysUserSocialBindingMapper bindingMapper;

    /**
     * 把三方身份绑定到指定系统账号。
     *
     * <p>双向防串号：既不允许「一个三方身份绑多个账号」，也不允许「一个账号在同一平台绑多个」——
     * 后者看着无害，但会让登录时"该用哪个身份"变得有歧义。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUserSocialBinding bind(Long userId, SocialUser social) {
        String platformName = platformName(social.platform());

        // 1. 该三方身份是否已被别的账号绑走
        SysUserSocialBinding existing = bindingMapper.selectByIdentity(
                social.platform(), social.corpId(), social.openId());
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                throw BusinessException.badRequest("该" + platformName + "账号已绑定到当前账号");
            }
            log.warn("三方绑定被拒（身份已属于其他账号）: platform={}, openId={}, ownerUserId={}, currentUserId={}",
                    social.platform(), social.openId(), existing.getUserId(), userId);
            throw BusinessException.badRequest("该" + platformName + "账号已绑定到其他系统账号，请先解绑");
        }

        // 2. 当前账号在该平台是否已绑过
        if (bindingMapper.selectByUserAndPlatform(userId, social.platform()) != null) {
            throw BusinessException.badRequest("当前账号已绑定过" + platformName + "，请先解绑再换绑");
        }

        SysUserSocialBinding binding = new SysUserSocialBinding()
                .setUserId(userId)
                .setPlatform(social.platform())
                .setCorpId(social.corpId())
                .setOpenId(social.openId())
                .setUnionId(social.unionId())
                .setCorpUserId(social.corpUserId())
                .setNickname(social.nickname())
                .setAvatar(social.avatar())
                .setBindTime(LocalDateTime.now())
                .setDeleted(0);
        bindingMapper.insert(binding);

        log.info("三方账号绑定成功: userId={}, platform={}, openId={}",
                userId, social.platform(), social.openId());
        return binding;
    }

    /**
     * 按三方身份查绑定（三方登录用）；未绑定返回 {@code null}
     */
    public SysUserSocialBinding findByIdentity(SocialUser social) {
        return bindingMapper.selectByIdentity(social.platform(), social.corpId(), social.openId());
    }

    /** 某用户的全部三方绑定 */
    public List<SysUserSocialBinding> listByUser(Long userId) {
        return bindingMapper.selectListByUser(userId);
    }

    /**
     * 解绑（逻辑删除：保留审计痕迹，且唯一索引带 {@code deleted = 0} 条件，
     * 解绑后该三方身份可被其他账号重新绑定）
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId, String platform) {
        SysUserSocialBinding binding = bindingMapper.selectByUserAndPlatform(userId, platform);
        if (binding == null) {
            throw BusinessException.notFound("当前账号未绑定" + platformName(platform));
        }
        binding.setDeleted(1).setUpdateTime(LocalDateTime.now());
        bindingMapper.updateById(binding);
        log.info("三方账号解绑: userId={}, platform={}", userId, platform);
    }

    /** 记录「上次用该三方账号登录」的时间 */
    public void touchLastLogin(Long bindingId) {
        bindingMapper.updateById(new SysUserSocialBinding()
                .setId(bindingId)
                .setLastLoginTime(LocalDateTime.now()));
    }

    /** 平台展示名 */
    public String platformName(String platform) {
        return switch (platform) {
            case "dingtalk" -> "钉钉";
            case "wecom" -> "企业微信";
            case "feishu" -> "飞书";
            default -> platform;
        };
    }
}

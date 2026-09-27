package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysUserSocialBinding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户三方账号绑定 Mapper
 *
 * <p>{@code sys_user_social_binding} 已在租户插件忽略清单中登记（表无 tenant_id），
 * 故此处无需 {@code @InterceptorIgnore}。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Mapper
public interface SysUserSocialBindingMapper extends BaseMapper<SysUserSocialBinding> {

    /**
     * 按三方身份查绑定关系（三方登录用）。
     * <p>corp_id 可能为空，故两侧都做 COALESCE 后再比，与建表时的唯一索引口径一致。</p>
     */
    @Select("SELECT * FROM sys_user_social_binding " +
            "WHERE platform = #{platform} " +
            "  AND COALESCE(corp_id, '') = COALESCE(#{corpId}, '') " +
            "  AND open_id = #{openId} " +
            "  AND deleted = 0 LIMIT 1")
    SysUserSocialBinding selectByIdentity(@Param("platform") String platform,
                                          @Param("corpId") String corpId,
                                          @Param("openId") String openId);

    /** 查某用户在指定平台的绑定 */
    @Select("SELECT * FROM sys_user_social_binding " +
            "WHERE user_id = #{userId} AND platform = #{platform} AND deleted = 0 LIMIT 1")
    SysUserSocialBinding selectByUserAndPlatform(@Param("userId") Long userId,
                                                 @Param("platform") String platform);

    /** 查某用户的全部三方绑定 */
    @Select("SELECT * FROM sys_user_social_binding WHERE user_id = #{userId} AND deleted = 0")
    List<SysUserSocialBinding> selectListByUser(@Param("userId") Long userId);
}

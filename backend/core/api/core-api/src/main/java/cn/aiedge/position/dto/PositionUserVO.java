package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位下的人员VO（岗位详情只读展示用）
 *
 * <p>字段全部来自 {@code sys_user_position} 与 {@code sys_user}，不额外派生统计值。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "岗位下的人员")
public class PositionUserVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称（页面展示的姓名）")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "是否主岗位：0-否，1-是")
    private Integer isPrimary;

    @Schema(description = "用户状态：0-正常，1-停用")
    private Integer status;
}

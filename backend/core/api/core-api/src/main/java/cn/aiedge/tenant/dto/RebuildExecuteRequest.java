package cn.aiedge.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 「系统重建」执行请求（设置 → 账套操作 → 系统重建，菜单 70560）。
 *
 * <p>对标 ql361 实测形态：勾选清除范围（12 项中的若干）+ 输入登录密码 → 点「确定」。</p>
 *
 * <p>⚠️ {@code password} 是<b>明文登录密码</b>，只用于本次请求的服务端二次校验。
 * 因此：① 审计日志<b>绝不</b>写入请求参数（见 {@code SetRebuildController} 的手工审计日志）；
 * ② 接口响应体<b>不</b>回显该字段；③ 不落库、不缓存。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "系统重建执行请求")
public class RebuildExecuteRequest {

    /** 勾选的范围键（对应 RebuildScopeRegistry 的 scope key）；至少 1 项 */
    @Schema(description = "勾选的范围键列表，如 [\"product\",\"warehouse_region\"]")
    private List<String> options;

    /** 登录密码（明文，仅本请求内使用，校完即弃） */
    @Schema(description = "当前登录账号的登录密码")
    private String password;
}

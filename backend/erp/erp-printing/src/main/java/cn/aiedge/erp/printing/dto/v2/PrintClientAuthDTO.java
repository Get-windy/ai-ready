package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 打印客户端认证 DTO
 */
@Data
public class PrintClientAuthDTO {

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 是否记住密码（自动登录） */
    private Boolean rememberMe = false;

    /** 客户端机器标识（首次启动时生成，用于识别同一台机器） */
    private String machineId;
}

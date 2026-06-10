package cn.aiedge.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 租户注册请求 DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class TenantRegisterDTO {

    /**
     * 租户注册请求
     */
    public record Register(
            @NotBlank(message = "租户名称不能为空")
            @Size(min = 2, max = 50, message = "租户名称长度 2-50 个字符")
            String tenantName,

            @NotBlank(message = "租户编码不能为空")
            @Pattern(regexp = "^[a-zA-Z0-9_-]{2,30}$", message = "租户编码限 2-30 位字母/数字/下划线/中划线")
            String tenantCode,

            @NotBlank(message = "联系人不能为空")
            @Size(max = 20, message = "联系人姓名最长 20 个字符")
            String contactPerson,

            @NotBlank(message = "联系电话不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
            String contactPhone,

            @NotBlank(message = "联系邮箱不能为空")
            @jakarta.validation.constraints.Email(message = "请输入有效的邮箱地址")
            String contactEmail,

            @NotBlank(message = "管理员用户名不能为空")
            @Size(min = 3, max = 20, message = "用户名长度 3-20 个字符")
            String adminUsername,

            @NotBlank(message = "管理员密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度 8-64 个字符")
            String adminPassword,

            @NotBlank(message = "管理员邮箱不能为空")
            @jakarta.validation.constraints.Email(message = "请输入有效的邮箱地址")
            String adminEmail
    ) {}

    /**
     * 租户审批请求
     */
    public record Approve(
            @Size(max = 200, message = "审批备注最长 200 个字符")
            String remark
    ) {}

    /**
     * 租户驳回请求
     */
    public record Reject(
            @NotBlank(message = "驳回原因不能为空")
            @Size(max = 200, message = "驳回原因最长 200 个字符")
            String reason
    ) {}
}

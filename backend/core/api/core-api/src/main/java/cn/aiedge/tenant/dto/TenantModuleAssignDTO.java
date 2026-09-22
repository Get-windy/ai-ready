package cn.aiedge.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 租户模块开通请求 DTO（平台侧，`POST /api/tenant-module/assign`）。
 *
 * <p>JSON 契约由产品/前端共同定死，字段名不得更改：
 * {@code {"tenantId": <Long>, "moduleCode": "<String>", "purchaseType": "<可选>", "expireTime": "<可选, ISO 时间>"}}。
 * 因此每个字段都显式标注 {@code @JsonProperty}，避免编译参数变化（如 {@code -parameters} 被去掉）导致绑定失败。</p>
 *
 * <p>{@code expireTime} 刻意用 String 接收、由控制器解析：本仓 Jackson 的 LocalDateTime
 * 反序列化格式取决于 Spring 默认配置，而契约要求「ISO 时间」；用 String + 显式解析
 * 可以把「格式不对」变成一条明确的中文 400 提示，而不是落到兜底 advice 的「系统异常」。</p>
 *
 * <p>{@code moduleName} 不在请求里：模块名一律取 {@code sys_module} 目录中的权威值
 * （见 {@code TenantModuleService#assignModule}），不信任调用方传入。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
public class TenantModuleAssignDTO {

    /** 开通请求体 */
    public record Assign(
            @NotNull(message = "租户ID不能为空")
            @JsonProperty("tenantId")
            Long tenantId,

            @NotBlank(message = "模块编码不能为空")
            @JsonProperty("moduleCode")
            String moduleCode,

            /** 购买类型：permanent / auto_renew / manual；缺省 permanent */
            @JsonProperty("purchaseType")
            String purchaseType,

            /** 到期时间（ISO 时间字符串，如 2027-01-01T00:00:00）；缺省 = 不过期 */
            @JsonProperty("expireTime")
            String expireTime
    ) {
    }
}

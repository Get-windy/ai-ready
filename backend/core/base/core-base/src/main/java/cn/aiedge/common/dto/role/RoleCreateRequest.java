package cn.aiedge.common.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求DTO
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码必须大写字母开头，只能包含大写字母、数字和下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    private String roleName;

    @Size(max = 20, message = "角色类型长度不能超过20")
    private String roleType;

    /**
     * 角色作用域：PLATFORM-平台级 TENANT-租户级（默认）
     */
    private String scope = "TENANT";

    private Integer dataScope = 1;

    private Long parentId;

    private Integer sort = 0;

    private Integer status = 1;

    private List<Long> permissionIds;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

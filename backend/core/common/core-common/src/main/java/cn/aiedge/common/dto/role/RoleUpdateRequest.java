package cn.aiedge.common.dto.role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求DTO
 */
@Data
public class RoleUpdateRequest {

    @NotNull(message = "角色ID不能为空")
    private Long id;

    @Size(max = 50, message = "角色名称长度不能超过50")
    private String roleName;

    @Size(max = 20, message = "角色类型长度不能超过20")
    private String roleType;

    private Integer dataScope;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private List<Long> permissionIds;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

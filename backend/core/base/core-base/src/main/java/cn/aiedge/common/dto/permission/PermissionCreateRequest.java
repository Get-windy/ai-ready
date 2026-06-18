package cn.aiedge.common.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限创建请求DTO
 */
@Data
public class PermissionCreateRequest {

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50")
    private String permissionName;

    @NotBlank(message = "权限类型不能为空")
    private String permissionType;

    private Long parentId;

    @Size(max = 255, message = "菜单路径长度不能超过255")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过255")
    private String component;

    @Size(max = 100, message = "图标长度不能超过100")
    private String icon;

    private Integer sort = 0;

    private Boolean visible = true;

    private Integer status = 1;

    @Size(max = 255, message = "API路径长度不能超过255")
    private String apiPath;

    @Size(max = 20, message = "请求方法长度不能超过20")
    private String method;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

package cn.aiedge.common.dto.permission;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限响应VO
 */
@Data
public class PermissionDetailVO {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private Long parentId;

    private String parentName;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Boolean visible;

    private Integer status;

    private String apiPath;

    private String method;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String remark;

    /**
     * 子权限列表（用于树形结构）
     */
    private List<PermissionDetailVO> children;
}

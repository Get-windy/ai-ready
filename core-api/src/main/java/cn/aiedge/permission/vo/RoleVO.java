package cn.aiedge.permission.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息VO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class RoleVO {
    
    /**
     * 角色ID
     */
    private Long id;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 角色类型
     */
    private Integer roleType;
    
    /**
     * 数据权限类型
     */
    private Integer dataScope;
    
    /**
     * 排序号
     */
    private Integer sort;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}

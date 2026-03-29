package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 菜单数据传输对象
 * 使用 Java 17 Record 简化代码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public sealed interface MenuDTO permits MenuDTO.Create, MenuDTO.Update, MenuDTO.Query {

    /**
     * 创建菜单DTO
     */
    record Create(
            Long tenantId,
            Long parentId,
            @NotBlank(message = "菜单名称不能为空")
            @Size(min = 2, max = 50, message = "菜单名称长度2-50字符")
            String menuName,
            @NotBlank(message = "菜单编码不能为空")
            @Size(min = 2, max = 50, message = "菜单编码长度2-50字符")
            String menuCode,
            Integer menuType,
            String path,
            String component,
            String routeName,
            String redirect,
            String icon,
            Integer sort,
            Integer isExternal,
            Integer isCache,
            Integer visible,
            String remark
    ) implements MenuDTO {}

    /**
     * 更新菜单DTO
     */
    record Update(
            Long id,
            Long parentId,
            String menuName,
            String menuCode,
            Integer menuType,
            String path,
            String component,
            String routeName,
            String redirect,
            String icon,
            Integer sort,
            Integer isExternal,
            Integer isCache,
            Integer visible,
            String remark
    ) implements MenuDTO {}

    /**
     * 查询菜单DTO
     */
    record Query(
            Long tenantId,
            Long parentId,
            String menuName,
            Integer menuType,
            Integer status
    ) implements MenuDTO {}
}
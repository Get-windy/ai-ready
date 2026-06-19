package cn.aiedge.base.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 权限变更事件
 * 当角色、菜单、权限发生增删改时触发，用于：
 * 1. 通过 Redis Pub/Sub 通知所有实例清除缓存
 * 2. 通过 SSE 实时推送受影响用户刷新权限
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class PermissionChangeEvent extends ApplicationEvent {

    /**
     * 变更类型
     */
    public enum ChangeType {
        ROLE_CREATED,
        ROLE_UPDATED,
        ROLE_DELETED,
        ROLE_PERMISSION_ASSIGNED,
        ROLE_MENU_ASSIGNED,
        MENU_CREATED,
        MENU_UPDATED,
        MENU_DELETED,
        PERMISSION_CREATED,
        PERMISSION_UPDATED,
        PERMISSION_DELETED,
        USER_ROLE_ASSIGNED,
        TENANT_MENU_ASSIGNED
    }

    private final ChangeType changeType;

    /**
     * 受影响的用户ID集合（null 表示广播给所有用户）
     */
    private final Set<Long> affectedUserIds;

    /**
     * 关联的租户ID（null 表示平台级变更）
     */
    private final Long tenantId;

    /**
     * 变更描述
     */
    private final String description;

    public PermissionChangeEvent(Object source, ChangeType changeType, Set<Long> affectedUserIds,
                                  Long tenantId, String description) {
        super(source);
        this.changeType = changeType;
        this.affectedUserIds = affectedUserIds;
        this.tenantId = tenantId;
        this.description = description;
    }

    /**
     * 创建广播事件（影响所有用户）
     */
    public static PermissionChangeEvent broadcast(Object source, ChangeType changeType,
                                                    Long tenantId, String description) {
        return new PermissionChangeEvent(source, changeType, null, tenantId, description);
    }

    /**
     * 创建定向事件（只影响指定用户）
     */
    public static PermissionChangeEvent targeted(Object source, ChangeType changeType,
                                                   Set<Long> userIds, Long tenantId, String description) {
        return new PermissionChangeEvent(source, changeType, userIds, tenantId, description);
    }
}

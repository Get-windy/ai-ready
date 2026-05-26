package cn.aiedge.gateway.service;

import org.springframework.cloud.gateway.route.RouteDefinition;
import java.util.List;

/**
 * 路由管理服务接口
 * 提供动态路由配置管理功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface RouteManagementService {

    /**
     * 获取所有路由定义
     * 
     * @return 路由定义列表
     */
    List<RouteDefinition> getAllRoutes();

    /**
     * 添加路由
     * 
     * @param routeDefinition 路由定义
     * @return 是否添加成功
     */
    Boolean addRoute(RouteDefinition routeDefinition);

    /**
     * 更新路由
     * 
     * @param routeDefinition 路由定义
     * @return 是否更新成功
     */
    Boolean updateRoute(RouteDefinition routeDefinition);

    /**
     * 删除路由
     * 
     * @param routeId 路由ID
     * @return 是否删除成功
     */
    Boolean deleteRoute(String routeId);

    /**
     * 刷新路由配置
     */
    void refreshRoutes();

    /**
     * 获取网关统计信息
     * 
     * @return 统计信息
     */
    Object getGatewayStats();
}

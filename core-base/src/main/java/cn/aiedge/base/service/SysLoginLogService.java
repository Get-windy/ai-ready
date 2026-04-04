package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysLoginLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录日志服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysLoginLogService extends IService<SysLoginLog> {

    /**
     * 记录登录日志
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param username 用户名
     * @param loginType 登录类型
     * @param loginResult 登录结果
     * @param failReason 失败原因
     * @param loginIp 登录IP
     * @param userAgent User-Agent
     * @param tokenId Token ID
     * @return 日志ID
     */
    Long recordLogin(Long tenantId, Long userId, String username, Integer loginType,
                     Integer loginResult, String failReason, String loginIp, 
                     String userAgent, String tokenId);

    /**
     * 记录登出日志
     * 
     * @param tokenId Token ID
     */
    void recordLogout(String tokenId);

    /**
     * 分页查询登录日志
     */
    Page<SysLoginLog> pageLoginLogs(Page<SysLoginLog> page, Long tenantId, 
                                    String username, Integer loginResult, 
                                    LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户最近登录记录
     */
    List<SysLoginLog> getRecentLogins(Long userId, int limit);

    /**
     * 统计登录次数（按日期）
     */
    List<Map<String, Object>> countByDate(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检测异常登录（同一IP多次失败）
     */
    List<Map<String, Object>> detectAbnormalLogin(int threshold, int minutes);

    /**
     * 解析 User-Agent
     */
    Map<String, String> parseUserAgent(String userAgent);

    /**
     * 根据IP获取地理位置
     */
    String getLocationByIp(String ip);
}
package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysLoginLog;
import cn.aiedge.base.mapper.SysLoginLogMapper;
import cn.aiedge.base.service.SysLoginLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录日志服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> 
        implements SysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    @Override
    public Long recordLogin(Long tenantId, Long userId, String username, Integer loginType,
                            Integer loginResult, String failReason, String loginIp,
                            String userAgent, String tokenId) {
        // 解析 User-Agent
        Map<String, String> uaInfo = parseUserAgent(userAgent);

        SysLoginLog loginLog = new SysLoginLog()
                .setTenantId(tenantId)
                .setUserId(userId)
                .setUsername(username)
                .setLoginType(loginType)
                .setLoginResult(loginResult)
                .setFailReason(failReason)
                .setLoginIp(loginIp)
                .setLoginLocation(getLocationByIp(loginIp))
                .setBrowser(uaInfo.get("browser"))
                .setOs(uaInfo.get("os"))
                .setDeviceType(uaInfo.get("deviceType"))
                .setLoginTime(LocalDateTime.now())
                .setTokenId(tokenId);

        save(loginLog);
        
        log.info("记录登录日志: userId={}, username={}, result={}, ip={}", 
                userId, username, loginResult == 0 ? "成功" : "失败", loginIp);
        
        return loginLog.getId();
    }

    @Override
    public void recordLogout(String tokenId) {
        if (tokenId != null && !tokenId.isEmpty()) {
            loginLogMapper.updateLogoutTime(tokenId, LocalDateTime.now());
            log.info("记录登出日志: tokenId={}", tokenId);
        }
    }

    @Override
    public Page<SysLoginLog> pageLoginLogs(Page<SysLoginLog> page, Long tenantId,
                                           String username, Integer loginResult,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, SysLoginLog::getTenantId, tenantId)
               .like(username != null && !username.isEmpty(), SysLoginLog::getUsername, username)
               .eq(loginResult != null, SysLoginLog::getLoginResult, loginResult)
               .ge(startTime != null, SysLoginLog::getLoginTime, startTime)
               .le(endTime != null, SysLoginLog::getLoginTime, endTime)
               .orderByDesc(SysLoginLog::getLoginTime);

        return page(page, wrapper);
    }

    @Override
    public List<SysLoginLog> getRecentLogins(Long userId, int limit) {
        return loginLogMapper.selectRecentByUserId(userId, limit);
    }

    @Override
    public List<Map<String, Object>> countByDate(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.countByDate(tenantId, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> detectAbnormalLogin(int threshold, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginLogMapper.countFailedByIp(since, threshold);
    }

    @Override
    public Map<String, String> parseUserAgent(String userAgent) {
        Map<String, String> result = new HashMap<>();
        
        if (userAgent == null || userAgent.isEmpty()) {
            result.put("browser", "Unknown");
            result.put("os", "Unknown");
            result.put("deviceType", "Unknown");
            return result;
        }

        // 解析浏览器
        if (userAgent.contains("Edge")) {
            result.put("browser", "Edge");
        } else if (userAgent.contains("Chrome")) {
            result.put("browser", "Chrome");
        } else if (userAgent.contains("Firefox")) {
            result.put("browser", "Firefox");
        } else if (userAgent.contains("Safari")) {
            result.put("browser", "Safari");
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            result.put("browser", "IE");
        } else {
            result.put("browser", "Other");
        }

        // 解析操作系统
        if (userAgent.contains("Windows")) {
            result.put("os", "Windows");
        } else if (userAgent.contains("Mac")) {
            result.put("os", "MacOS");
        } else if (userAgent.contains("Linux")) {
            result.put("os", "Linux");
        } else if (userAgent.contains("Android")) {
            result.put("os", "Android");
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            result.put("os", "iOS");
        } else {
            result.put("os", "Other");
        }

        // 解析设备类型
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            result.put("deviceType", "Mobile");
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            result.put("deviceType", "Tablet");
        } else {
            result.put("deviceType", "PC");
        }

        return result;
    }

    @Override
    public String getLocationByIp(String ip) {
        // 简化实现，实际应调用 IP 地理位置服务
        if (ip == null || ip.isEmpty() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "本地";
        }
        
        // TODO: 集成 IP 地理位置服务（如高德、百度等）
        return "未知";
    }
}
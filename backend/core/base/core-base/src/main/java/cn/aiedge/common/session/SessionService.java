package cn.aiedge.common.session;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 会话服务
 * 管理用户会话和登录状态
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = false)
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY = "user:sessions:";
    private static final long SESSION_TIMEOUT_HOURS = 24;

    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 创建会话
     *
     * @param userId   用户ID
     * @param token    Token
     * @param clientIp 客户端IP
     * @param device   设备信息
     * @return 会话信息
     */
    public SessionInfo createSession(Long userId, String token, String clientIp, String device) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        
        SessionInfo session = new SessionInfo();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setToken(token);
        session.setClientIp(clientIp);
        session.setDevice(device);
        session.setCreateTime(new Date());
        session.setLastAccessTime(new Date());
        
        // 存储会话
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        
        // 关联用户会话列表
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        
        log.info("会话创建成功: sessionId={}, userId={}, ip={}", sessionId, userId, clientIp);
        return session;
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    public SessionInfo getSession(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        Object obj = redisTemplate.opsForValue().get(sessionKey);
        return obj instanceof SessionInfo ? (SessionInfo) obj : null;
    }

    /**
     * 更新会话访问时间
     *
     * @param sessionId 会话ID
     */
    public void touchSession(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        SessionInfo session = getSession(sessionId);
        if (session != null) {
            session.setLastAccessTime(new Date());
            redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        }
    }

    /**
     * 销毁会话
     *
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        SessionInfo session = getSession(sessionId);
        if (session != null) {
            // 从用户会话列表移除
            String userSessionsKey = USER_SESSIONS_KEY + session.getUserId();
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            
            // 删除会话
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            
            log.info("会话销毁: sessionId={}, userId={}", sessionId, session.getUserId());
        }
    }

    /**
     * 销毁用户所有会话
     *
     * @param userId 用户ID
     * @return 销毁的会话数量
     */
    public int destroyUserSessions(Long userId) {
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (Object sid : sessionIds) {
            String sessionKey = SESSION_KEY_PREFIX + sid;
            redisTemplate.delete(sessionKey);
            count++;
        }
        
        redisTemplate.delete(userSessionsKey);
        log.info("用户会话全部销毁: userId={}, count={}", userId, count);
        return count;
    }

    /**
     * 获取用户所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<SessionInfo> getUserSessions(Long userId) {
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SessionInfo> sessions = new ArrayList<>();
        for (Object sid : sessionIds) {
            SessionInfo session = getSession((String) sid);
            if (session != null) {
                sessions.add(session);
            }
        }
        
        // 按最后访问时间降序
        sessions.sort((a, b) -> b.getLastAccessTime().compareTo(a.getLastAccessTime()));
        return sessions;
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsLong();
        }
        return null;
    }

    /**
     * 检查会话是否有效
     *
     * @param sessionId 会话ID
     * @return 是否有效
     */
    public boolean isSessionValid(String sessionId) {
        return getSession(sessionId) != null;
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public long getOnlineUserCount() {
        // 获取所有用户会话key
        Set<String> keys = redisTemplate.keys(USER_SESSIONS_KEY + "*");
        return keys != null ? keys.size() : 0;
    }

    /**
     * 会话信息
     */
    @lombok.Data
    public static class SessionInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private String sessionId;
        private Long userId;
        private String token;
        private String clientIp;
        private String device;
        private Date createTime;
        private Date lastAccessTime;
        private String location;  // 地理位置
        private String browser;   // 浏览器
        private String os;        // 操作系统
        
        // 手动添加 getter/setter，以防 @Data 不生效
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getDevice() { return device; }
        public void setDevice(String device) { this.device = device; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public Date getLastAccessTime() { return lastAccessTime; }
        public void setLastAccessTime(Date lastAccessTime) { this.lastAccessTime = lastAccessTime; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }
        public String getOs() { return os; }
        public void setOs(String os) { this.os = os; }
    }
}

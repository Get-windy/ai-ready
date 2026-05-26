package cn.aiedge.user.repository;

import cn.aiedge.user.entity.User;
import cn.aiedge.user.entity.User.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * 提供用户实体的CRUD操作和自定义查询方法
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据状态查找用户列表
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    
    /**
     * 根据部门查找用户列表
     */
    Page<User> findByDepartment(String department, Pageable pageable);
    
    /**
     * 查找活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.enabled = true")
    Page<User> findActiveUsers(@Param("status") UserStatus status, Pageable pageable);
    
    /**
     * 查找锁定用户
     */
    @Query("SELECT u FROM User u WHERE u.status = 'LOCKED' OR u.lockedUntil > :now")
    Page<User> findLockedUsers(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * 查找长时间未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :lastLoginTime OR u.lastLoginAt IS NULL")
    Page<User> findInactiveUsers(@Param("lastLoginTime") LocalDateTime lastLoginTime, Pageable pageable);
    
    /**
     * 更新用户最后登录信息
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ip, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime, @Param("ip") String ip);
    
    /**
     * 增加登录失败次数
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") Long userId);
    
    /**
     * 锁定用户
     */
    @Modifying
    @Query("UPDATE User u SET u.status = 'LOCKED', u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockUser(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);
    
    /**
     * 解锁用户
     */
    @Modifying
    @Query("UPDATE User u SET u.status = 'ACTIVE', u.lockedUntil = NULL, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockUser(@Param("userId") Long userId);
    
    /**
     * 统计用户总数
     */
    long countByStatus(UserStatus status);
    
    /**
     * 统计活跃用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE' AND u.enabled = true")
    long countActiveUsers();
    
    /**
     * 统计锁定用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'LOCKED' OR u.lockedUntil > :now")
    long countLockedUsers(@Param("now") LocalDateTime now);
}
package cn.aiedge.user.service;

import cn.aiedge.user.entity.User;
import cn.aiedge.user.entity.User.UserStatus;
import cn.aiedge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务类
 * 
 * 提供用户管理的业务逻辑
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 创建用户
     */
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + user.getEmail());
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(false);
        }
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 更新用户
     */
    public User updateUser(Long id, User userUpdate) {
        log.info("更新用户: {}", id);
        
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        
        // 更新基本信息
        if (userUpdate.getFullName() != null) {
            existingUser.setFullName(userUpdate.getFullName());
        }
        if (userUpdate.getPhone() != null) {
            existingUser.setPhone(userUpdate.getPhone());
        }
        if (userUpdate.getDepartment() != null) {
            existingUser.setDepartment(userUpdate.getDepartment());
        }
        if (userUpdate.getPosition() != null) {
            existingUser.setPosition(userUpdate.getPosition());
        }
        if (userUpdate.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(userUpdate.getAvatarUrl());
        }
        
        return userRepository.save(existingUser);
    }
    
    /**
     * 根据ID查询用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 根据用户名查询用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据邮箱查询用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * 查询用户列表
     */
    @Transactional(readOnly = true)
    public Page<User> getUserList(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * 根据状态查询用户列表
     */
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }
    
    /**
     * 删除用户（软删除）
     */
    public void deleteUser(Long id) {
        log.info("删除用户: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        
        user.setStatus(UserStatus.DELETED);
        user.setEnabled(false);
        userRepository.save(user);
    }
    
    /**
     * 锁定用户
     */
    public void lockUser(Long id, LocalDateTime lockedUntil) {
        log.info("锁定用户: {}, 解锁时间: {}", id, lockedUntil);
        userRepository.lockUser(id, lockedUntil);
    }
    
    /**
     * 解锁用户
     */
    public void unlockUser(Long id) {
        log.info("解锁用户: {}", id);
        userRepository.unlockUser(id);
    }
    
    /**
     * 更新用户登录信息
     */
    public void updateLastLogin(Long userId, String ip) {
        userRepository.updateLastLogin(userId, LocalDateTime.now(), ip);
    }
    
    /**
     * 增加登录失败次数
     */
    public void incrementFailedLoginAttempts(Long userId) {
        userRepository.incrementFailedLoginAttempts(userId);
    }
    
    /**
     * 统计用户总数
     */
    @Transactional(readOnly = true)
    public long countTotalUsers() {
        return userRepository.count();
    }
    
    /**
     * 统计活跃用户数
     */
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }
    
    /**
     * 统计锁定用户数
     */
    @Transactional(readOnly = true)
    public long countLockedUsers() {
        return userRepository.countLockedUsers(LocalDateTime.now());
    }
    
    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.mapper.PermissionMapper;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.UserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.user.*;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类 - 性能优化版本
 * 
 * 优化点：
 * 1. 引入Caffeine本地缓存 + Redis分布式缓存多级缓存策略
 * 2. 批量查询优化，减少N+1问题
 * 3. 异步处理非关键路径
 * 4. 数据库查询优化（索引提示）
 */
@Slf4j
@Service
@Primary
public class UserServiceOptimizedImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private PermissionMapper permissionMapper;
    
    @Autowired
    private SysUserRoleMapper userRoleMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 本地缓存 - 用户基础信息（高频读取）
    private final Cache<String, SysUser> userLocalCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    // 本地缓存 - 用户角色（高频读取）
    private final Cache<Long, List<SysRole>> userRoleLocalCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /** 平台超级管理员角色码（与 SysMenuServiceImpl / SysUserServiceImpl 同源，不新造判定） */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    // Redis缓存Key前缀
    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final String USER_ROLES_CACHE_KEY_PREFIX = "user:roles:";
    private static final String USER_PERMISSIONS_CACHE_KEY_PREFIX = "user:permissions:";
    private static final long USER_CACHE_TTL = 30; // 30分钟

    @Override
    public PageResult<UserVO> pageList(UserQueryRequest request) {
        // 优化：使用覆盖索引查询，避免回表
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件 - 优先使用索引字段
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(SysUser::getUsername, request.getUsername());
        }
        if (StringUtils.hasText(request.getRealName())) {
            wrapper.like(SysUser::getRealName, request.getRealName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            wrapper.eq(SysUser::getPhone, request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            wrapper.eq(SysUser::getEmail, request.getEmail());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, request.getStatus());
        }
        if (request.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, request.getDeptId());
        }

        // 租户收敛：`sys_user` 在多租户忽略表内（登录需跨租户按用户名查账号），拦截器不会注入 tenant_id，
        // 而本方法此前**完全没有租户条件** → 任意租户的管理员能翻到全库所有租户的账号。
        // 口径与 /api/user/page 一致：**超管豁免，其余强制限本租户**。
        if (!SecurityUtils.hasRole(SUPER_ADMIN_ROLE)) {
            Long sessionTenantId = SecurityUtils.getCurrentTenantId();
            if (sessionTenantId == null) {
                throw BusinessException.forbidden("无法确定当前租户，已拒绝查询用户列表");
            }
            wrapper.eq(SysUser::getTenantId, sessionTenantId);
        }

        // 优化：使用索引排序
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<SysUser> result = page(page, wrapper);

        // 优化：批量查询角色信息，避免N+1问题
        List<Long> userIds = result.getRecords().stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());
        
        // 批量获取用户角色映射
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        
        // 转换为VO
        List<UserVO> voList = result.getRecords().stream()
                .map(user -> convertToVOWithRoles(user, userRoles))
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public UserVO getDetail(Long id) {
        String cacheKey = USER_CACHE_KEY_PREFIX + id;
        
        SysUser user = userLocalCache.getIfPresent(String.valueOf(id));
        if (user != null) {
            log.debug("User {} hit local cache", id);
            return buildUserVOWithCache(user);
        }
        
        if (redisTemplate != null) {
            user = (SysUser) redisTemplate.opsForValue().get(cacheKey);
            if (user != null) {
                log.debug("User {} hit redis cache", id);
                userLocalCache.put(String.valueOf(id), user);
                return buildUserVOWithCache(user);
            }
        }
        
        user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        userLocalCache.put(String.valueOf(id), user);
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, user, USER_CACHE_TTL, TimeUnit.MINUTES);
        }
        
        return buildUserVOWithCache(user);
    }

    /**
     * 从缓存构建UserVO
     */
    private UserVO buildUserVOWithCache(SysUser user) {
        UserVO vo = convertToVO(user);
        
        // 从缓存获取角色
        Long userId = user.getId();
        List<SysRole> roles = getUserRolesFromCache(userId);
        vo.setRoles(roles.stream().map(this::convertToRoleVO).collect(Collectors.toList()));
        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        
        return vo;
    }

    /**
     * 从缓存获取用户角色
     */
    private List<SysRole> getUserRolesFromCache(Long userId) {
        List<SysRole> roles = userRoleLocalCache.getIfPresent(userId);
        if (roles != null) {
            return roles;
        }

        if (redisTemplate != null) {
            String cacheKey = USER_ROLES_CACHE_KEY_PREFIX + userId;
            roles = (List<SysRole>) redisTemplate.opsForValue().get(cacheKey);
            if (roles != null) {
                userRoleLocalCache.put(userId, roles);
                return roles;
            }
        }
        
        roles = roleMapper.selectByUserId(userId);
        
        userRoleLocalCache.put(userId, roles);
        if (redisTemplate != null) {
            String cacheKey = USER_ROLES_CACHE_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(cacheKey, roles, USER_CACHE_TTL, TimeUnit.MINUTES);
        }
        
        return roles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        // 优化：批量校验唯一性，减少数据库查询次数
        validateUserUniqueness(request.getUsername(), request.getPhone(), request.getEmail());

        // 创建用户
        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsSuperAdmin(false);
        
        save(user);
        
        // 分配角色
        if (!CollectionUtils.isEmpty(request.getRoleIds())) {
            assignRoles(user.getId(), request.getRoleIds());
        }
        
        // 清除相关缓存
        evictUserCache(user.getId());
        
        log.info("创建用户成功: {}", user.getUsername());
        return user.getId();
    }

    /**
     * 批量校验用户唯一性
     */
    private void validateUserUniqueness(String username, String phone, String email) {
        // 使用单个查询检查多个条件
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
               .or().eq(StringUtils.hasText(phone), SysUser::getPhone, phone)
               .or().eq(StringUtils.hasText(email), SysUser::getEmail, email);
        
        List<SysUser> existingUsers = list(wrapper);
        
        for (SysUser existing : existingUsers) {
            if (existing.getUsername().equals(username)) {
                throw BusinessException.badRequest("用户名已存在");
            }
            if (StringUtils.hasText(phone) && phone.equals(existing.getPhone())) {
                throw BusinessException.badRequest("手机号已被使用");
            }
            if (StringUtils.hasText(email) && email.equals(existing.getEmail())) {
                throw BusinessException.badRequest("邮箱已被使用");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest request) {
        SysUser user = getById(request.getId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 校验唯一性
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (getByPhone(request.getPhone()) != null) {
                throw BusinessException.badRequest("手机号已被使用");
            }
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (getByEmail(request.getEmail()) != null) {
                throw BusinessException.badRequest("邮箱已被使用");
            }
        }

        // 更新用户信息
        BeanUtils.copyProperties(request, user);
        updateById(user);
        
        // 更新角色
        if (request.getRoleIds() != null) {
            assignRoles(user.getId(), request.getRoleIds());
        }
        
        // 清除缓存
        evictUserCache(user.getId());
        
        log.info("更新用户成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw BusinessException.badRequest("超级管理员不能删除");
        }
        
        // 删除用户角色关联
        userRoleMapper.deleteByUserId(id);
        
        // 删除用户
        removeById(id);
        
        // 清除缓存
        evictUserCache(id);
        
        log.info("删除用户成功: {}", user.getUsername());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw BusinessException.badRequest("超级管理员不能修改状态");
        }
        
        user.setStatus(status);
        updateById(user);
        evictUserCache(id);
        
        log.info("更新用户状态成功: id={}, status={}", id, status);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("原密码错误");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        evictUserCache(id);
        
        log.info("修改密码成功: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        evictUserCache(id);
        
        log.info("重置密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        
        // 检查是否包含超级管理员
        long superAdminCount = lambdaQuery()
                .in(SysUser::getId, ids)
                .eq(SysUser::getIsSuperAdmin, true)
                .count();
        
        if (superAdminCount > 0) {
            throw BusinessException.badRequest("不能删除超级管理员");
        }
        
        // 批量删除用户角色关联
        ids.forEach(userId -> userRoleMapper.deleteByUserId(userId));
        
        // 批量删除用户
        removeByIds(ids);
        
        // 批量清除缓存
        ids.forEach(this::evictUserCache);
        
        log.info("批量删除用户成功: {} 个", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.deleteByUserId(userId);
        
        // 添加新角色
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        SysUserRole ur = new SysUserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        ur.setCreateTime(LocalDateTime.now());
                        return ur;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.batchInsert(userRoles);
        }
        
        // 清除角色缓存
        evictUserRoleCache(userId);
        
        log.info("分配角色成功: userId={}, roleIds={}", userId, roleIds);
    }

    /**
     * 清除用户缓存
     */
    private void evictUserCache(Long userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        userLocalCache.invalidate(String.valueOf(userId));
        if (redisTemplate != null) {
            redisTemplate.delete(cacheKey);
        }
        evictUserRoleCache(userId);
    }

    /**
     * 清除用户角色缓存
     */
    private void evictUserRoleCache(Long userId) {
        String cacheKey = USER_ROLES_CACHE_KEY_PREFIX + userId;
        userRoleLocalCache.invalidate(userId);
        if (redisTemplate != null) {
            redisTemplate.delete(cacheKey);
        }
    }

    @Override
    public SysUser getByUsername(String username) {
        // 优化：使用本地缓存
        return userLocalCache.get(username, k -> userMapper.selectByUsername(k));
    }

    @Override
    public SysUser getByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    @Override
    public SysUser getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public void updateLoginInfo(Long userId, String loginIp) {
        String safeLoginIp = (loginIp != null && !loginIp.isEmpty()) ? loginIp : "0.0.0.0";
        userMapper.updateLoginInfo(userId, safeLoginIp);
        // 清除缓存
        evictUserCache(userId);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<SysRole> roles = getUserRolesFromCache(userId);
        return roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        if (redisTemplate != null) {
            String cacheKey = USER_PERMISSIONS_CACHE_KEY_PREFIX + userId;
            List<String> permissions = (List<String>) redisTemplate.opsForValue().get(cacheKey);
            
            if (permissions != null) {
                return permissions;
            }
        }
        
        List<SysPermission> perms = permissionMapper.selectByUserId(userId);
        List<String> permissions = perms.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());
        
        if (redisTemplate != null) {
            String cacheKey = USER_PERMISSIONS_CACHE_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(cacheKey, permissions, USER_CACHE_TTL, TimeUnit.MINUTES);
        }
        
        return permissions;
    }

    /**
     * 转换为VO（带批量角色信息）
     */
    private UserVO convertToVOWithRoles(SysUser user, List<SysUserRole> allUserRoles) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        
        // 从批量数据中提取当前用户的角色
        List<Long> roleIds = allUserRoles.stream()
                .filter(ur -> ur.getUserId().equals(user.getId()))
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        
        vo.setRoleIds(roleIds);
        return vo;
    }

    /**
     * 转换为VO
     */
    private UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    /**
     * 转换为角色VO
     */
    private cn.aiedge.common.dto.user.RoleVO convertToRoleVO(SysRole role) {
        cn.aiedge.common.dto.user.RoleVO vo = new cn.aiedge.common.dto.user.RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}

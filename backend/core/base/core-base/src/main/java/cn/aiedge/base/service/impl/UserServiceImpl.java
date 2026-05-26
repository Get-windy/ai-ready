package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.Permission;
import cn.aiedge.base.entity.Role;
import cn.aiedge.base.entity.User;
import cn.aiedge.base.entity.UserRole;
import cn.aiedge.base.mapper.PermissionMapper;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.UserMapper;
import cn.aiedge.base.mapper.UserRoleMapper;
import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.user.*;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> pageList(UserQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        wrapper.like(StringUtils.hasText(request.getUsername()), User::getUsername, request.getUsername())
               .like(StringUtils.hasText(request.getRealName()), User::getRealName, request.getRealName())
               .eq(StringUtils.hasText(request.getPhone()), User::getPhone, request.getPhone())
               .eq(StringUtils.hasText(request.getEmail()), User::getEmail, request.getEmail())
               .eq(request.getStatus() != null, User::getStatus, request.getStatus())
               .eq(request.getDeptId() != null, User::getDeptId, request.getDeptId())
               .eq(request.getGender() != null, User::getGender, request.getGender())
               .orderByDesc(User::getCreateTime);

        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<User> result = page(page, wrapper);

        // 转换为VO
        List<UserVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public UserVO getDetail(Long id) {
        User user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        UserVO vo = convertToVO(user);
        
        // 查询用户角色
        List<Role> roles = roleMapper.selectByUserId(id);
        vo.setRoles(roles.stream().map(this::convertToRoleVO).collect(Collectors.toList()));
        vo.setRoleIds(roles.stream().map(Role::getId).collect(Collectors.toList()));
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        // 校验用户名唯一性
        if (getByUsername(request.getUsername()) != null) {
            throw BusinessException.badRequest("用户名已存在");
        }
        
        // 校验手机号唯一性
        if (StringUtils.hasText(request.getPhone()) && getByPhone(request.getPhone()) != null) {
            throw BusinessException.badRequest("手机号已被使用");
        }
        
        // 校验邮箱唯一性
        if (StringUtils.hasText(request.getEmail()) && getByEmail(request.getEmail()) != null) {
            throw BusinessException.badRequest("邮箱已被使用");
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsSuperAdmin(false);
        
        save(user);
        
        // 分配角色
        if (!CollectionUtils.isEmpty(request.getRoleIds())) {
            assignRoles(user.getId(), request.getRoleIds());
        }
        
        log.info("创建用户成功: {}", user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest request) {
        User user = getById(request.getId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 校验手机号唯一性
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (getByPhone(request.getPhone()) != null) {
                throw BusinessException.badRequest("手机号已被使用");
            }
        }

        // 校验邮箱唯一性
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
        
        log.info("更新用户成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User user = getById(id);
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
        
        log.info("删除用户成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        
        // 检查是否包含超级管理员
        long superAdminCount = lambdaQuery()
                .in(User::getId, ids)
                .eq(User::getIsSuperAdmin, true)
                .count();
        
        if (superAdminCount > 0) {
            throw BusinessException.badRequest("不能删除超级管理员");
        }
        
        // 删除用户角色关联
        ids.forEach(userRoleMapper::deleteByUserId);
        
        // 批量删除用户
        removeByIds(ids);
        
        log.info("批量删除用户成功: {} 个", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("原密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        
        log.info("修改密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        // 重置密码
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        
        log.info("重置密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw BusinessException.badRequest("不能修改超级管理员状态");
        }
        
        user.setStatus(status);
        updateById(user);
        
        log.info("更新用户状态成功: {} -> {}", user.getUsername(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.deleteByUserId(userId);
        
        // 添加新角色
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole ur = new UserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        ur.setCreateTime(LocalDateTime.now());
                        return ur;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.batchInsert(userRoles);
        }
        
        log.info("分配角色成功: userId={}, roleIds={}", userId, roleIds);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User getByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    @Override
    public User getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public void updateLoginInfo(Long userId, String loginIp) {
        userMapper.updateLoginInfo(userId, loginIp);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<Role> roles = roleMapper.selectByUserId(userId);
        return roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    /**
     * 转换为角色VO
     */
    private cn.aiedge.common.dto.user.RoleVO convertToRoleVO(Role role) {
        cn.aiedge.common.dto.user.RoleVO vo = new cn.aiedge.common.dto.user.RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}

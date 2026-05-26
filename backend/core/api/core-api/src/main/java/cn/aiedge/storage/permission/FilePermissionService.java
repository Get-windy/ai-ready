package cn.aiedge.storage.permission;

import cn.aiedge.storage.model.StorageFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件权限服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilePermissionService {

    private final FilePermissionMapper permissionMapper;

    /**
     * 授权文件权限
     *
     * @param fileId 文件ID
     * @param permissionType 权限类型
     * @param principalType 授权对象类型
     * @param principalId 授权对象ID
     * @param grantedBy 授权人ID
     * @param expireTime 过期时间（可选）
     * @return 权限实体
     */
    @Transactional
    public FilePermission grantPermission(Long fileId, 
                                           FilePermission.PermissionType permissionType,
                                           FilePermission.PrincipalType principalType,
                                           Long principalId,
                                           Long grantedBy,
                                           LocalDateTime expireTime) {
        // 检查是否已存在相同权限
        int count = permissionMapper.countPermission(
                fileId, permissionType.getCode(), principalType.getCode(), principalId);
        if (count > 0) {
            log.warn("Permission already exists: fileId={}, type={}, principal={}/{}",
                    fileId, permissionType, principalType, principalId);
            return null;
        }

        FilePermission permission = new FilePermission();
        permission.setFileId(fileId);
        permission.setPermissionType(permissionType.getCode());
        permission.setPrincipalType(principalType.getCode());
        permission.setPrincipalId(principalId);
        permission.setInheritable(true);
        permission.setExpireTime(expireTime);
        permission.setGrantedBy(grantedBy);
        permission.setCreateTime(LocalDateTime.now());

        permissionMapper.insert(permission);
        log.info("Permission granted: fileId={}, type={}, principal={}/{}",
                fileId, permissionType, principalType, principalId);

        return permission;
    }

    /**
     * 批量授权
     *
     * @param fileId 文件ID
     * @param permissionRequests 权限请求列表
     * @param grantedBy 授权人ID
     * @return 授权成功的数量
     */
    @Transactional
    public int grantPermissions(Long fileId, 
                                List<PermissionRequest> permissionRequests,
                                Long grantedBy) {
        int count = 0;
        for (PermissionRequest request : permissionRequests) {
            FilePermission permission = grantPermission(
                    fileId,
                    request.getPermissionType(),
                    request.getPrincipalType(),
                    request.getPrincipalId(),
                    grantedBy,
                    request.getExpireTime()
            );
            if (permission != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 撤销权限
     *
     * @param permissionId 权限ID
     * @return 是否成功
     */
    @Transactional
    public boolean revokePermission(Long permissionId) {
        int rows = permissionMapper.deleteById(permissionId);
        if (rows > 0) {
            log.info("Permission revoked: permissionId={}", permissionId);
            return true;
        }
        return false;
    }

    /**
     * 撤销指定文件的所有权限
     *
     * @param fileId 文件ID
     * @return 撤销的数量
     */
    @Transactional
    public int revokeAllPermissions(Long fileId) {
        List<FilePermission> permissions = permissionMapper.selectByFileId(fileId);
        int count = 0;
        for (FilePermission permission : permissions) {
            if (revokePermission(permission.getId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查用户是否有文件权限
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param permissionType 权限类型
     * @param roleIds 用户角色ID列表
     * @param deptIds 用户部门ID列表
     * @return 是否有权限
     */
    public boolean hasPermission(Long fileId, Long userId, 
                                  FilePermission.PermissionType permissionType,
                                  List<Long> roleIds, List<Long> deptIds) {
        List<FilePermission> permissions = permissionMapper.selectUserPermissions(
                fileId, userId, roleIds, deptIds);

        LocalDateTime now = LocalDateTime.now();
        for (FilePermission permission : permissions) {
            // 检查是否过期
            if (permission.getExpireTime() != null && now.isAfter(permission.getExpireTime())) {
                continue;
            }

            // 检查权限级别
            FilePermission.PermissionType type = FilePermission.PermissionType.valueOf(
                    permission.getPermissionType().toUpperCase());
            if (type.includes(permissionType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取文件的所有权限
     *
     * @param fileId 文件ID
     * @return 权限列表
     */
    public List<FilePermission> getFilePermissions(Long fileId) {
        return permissionMapper.selectByFileId(fileId);
    }

    /**
     * 获取用户对文件的最高权限级别
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param deptIds 部门ID列表
     * @return 最高权限级别，null表示无权限
     */
    public FilePermission.PermissionType getHighestPermission(Long fileId, Long userId,
                                                               List<Long> roleIds, List<Long> deptIds) {
        List<FilePermission> permissions = permissionMapper.selectUserPermissions(
                fileId, userId, roleIds, deptIds);

        LocalDateTime now = LocalDateTime.now();
        FilePermission.PermissionType highest = null;

        for (FilePermission permission : permissions) {
            // 检查是否过期
            if (permission.getExpireTime() != null && now.isAfter(permission.getExpireTime())) {
                continue;
            }

            FilePermission.PermissionType type = FilePermission.PermissionType.valueOf(
                    permission.getPermissionType().toUpperCase());

            if (type == FilePermission.PermissionType.ADMIN) {
                return FilePermission.PermissionType.ADMIN;
            }

            if (highest == null || type.ordinal() > highest.ordinal()) {
                highest = type;
            }
        }

        return highest;
    }

    /**
     * 检查用户是否是文件所有者
     *
     * @param file 文件信息
     * @param userId 用户ID
     * @return 是否是所有者
     */
    public boolean isOwner(StorageFile file, Long userId) {
        return file != null && file.getUploaderId() != null 
                && file.getUploaderId().equals(userId);
    }

    /**
     * 复制文件权限到新文件
     *
     * @param sourceFileId 源文件ID
     * @param targetFileId 目标文件ID
     * @param grantedBy 授权人ID
     * @return 复制的权限数量
     */
    @Transactional
    public int copyPermissions(Long sourceFileId, Long targetFileId, Long grantedBy) {
        List<FilePermission> sourcePermissions = permissionMapper.selectByFileId(sourceFileId);
        int count = 0;

        for (FilePermission source : sourcePermissions) {
            if (Boolean.TRUE.equals(source.getInheritable())) {
                FilePermission target = new FilePermission();
                target.setFileId(targetFileId);
                target.setPermissionType(source.getPermissionType());
                target.setPrincipalType(source.getPrincipalType());
                target.setPrincipalId(source.getPrincipalId());
                target.setInheritable(true);
                target.setExpireTime(source.getExpireTime());
                target.setGrantedBy(grantedBy);
                target.setCreateTime(LocalDateTime.now());

                permissionMapper.insert(target);
                count++;
            }
        }

        return count;
    }

    /**
     * 权限请求DTO
     */
    @lombok.Data
    public static class PermissionRequest {
        private FilePermission.PermissionType permissionType;
        private FilePermission.PrincipalType principalType;
        private Long principalId;
        private LocalDateTime expireTime;
    }
}

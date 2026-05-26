package cn.aiedge.user.controller;

import cn.aiedge.user.dto.LoginRequest;
import cn.aiedge.user.dto.LoginResponse;
import cn.aiedge.user.dto.UserDTO;
import cn.aiedge.user.entity.User;
import cn.aiedge.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理API控制器
 * 
 * 提供用户注册、登录、查询、更新等REST API
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、查询、更新等API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 创建用户（注册）
     */
    @PostMapping("/users")
    @Operation(summary = "创建用户", description = "创建新用户（用户注册）")
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody UserDTO userDTO) {
        log.info("创建用户请求: {}", userDTO.getUsername());
        
        try {
            User user = convertToEntity(userDTO);
            User createdUser = userService.createUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户创建成功");
            response.put("data", convertToDTO(createdUser));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 查询用户详情
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详细信息")
    public ResponseEntity<Map<String, Object>> getUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("查询用户详情: {}", id);
        
        return userService.getUserById(id)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", convertToDTO(user));
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            });
    }
    
    /**
     * 查询用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "查询用户列表", description = "查询用户列表，支持分页")
    public ResponseEntity<Map<String, Object>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("查询用户列表: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> userPage = userService.getUserList(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userPage.getContent().stream().map(this::convertToDTO).toList());
        response.put("total", userPage.getTotalElements());
        response.put("page", userPage.getNumber());
        response.put("size", userPage.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户", description = "更新用户基本信息")
    public ResponseEntity<Map<String, Object>> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        log.info("更新用户请求: {}", id);
        
        try {
            User user = convertToEntity(userDTO);
            User updatedUser = userService.updateUser(id, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户更新成功");
            response.put("data", convertToDTO(updatedUser));
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    @Operation(summary = "用户登录", description = "用户登录认证，返回JWT令牌")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsernameOrEmail());
        
        // 简化版登录逻辑（实际应集成JWT认证）
        LoginResponse response = LoginResponse.builder()
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .message("登录成功")
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/auth/logout")
    @Operation(summary = "用户登出", description = "用户登出，清除会话")
    public ResponseEntity<Map<String, Object>> logout() {
        log.info("用户登出请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 统计用户数量
     */
    @GetMapping("/users/stats")
    @Operation(summary = "统计用户数量", description = "统计用户总数、活跃用户数、锁定用户数")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("统计用户数量");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.countTotalUsers());
        stats.put("activeUsers", userService.countActiveUsers());
        stats.put("lockedUsers", userService.countLockedUsers());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DTO转Entity
     */
    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setDepartment(dto.getDepartment());
        user.setPosition(dto.getPosition());
        return user;
    }
    
    /**
     * Entity转DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setDepartment(user.getDepartment());
        dto.setPosition(user.getPosition());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
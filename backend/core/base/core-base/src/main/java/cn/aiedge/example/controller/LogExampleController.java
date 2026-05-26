package cn.aiedge.example.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 日志审计模块使用示例
 * 展示如何在业务代码中使用日志审计功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
public class LogExampleController {

    /**
     * 创建用户的示例方法
     * 使用@OperationLog注解记录操作日志
     */
    @PostMapping("/user")
    @OperationLog(module = "用户管理", type = "CREATE", desc = "创建用户", businessKey = "#dto.id", businessType = "'USER'")
    public Result<String> createUser(@RequestBody CreateUserDto dto) {
        // 模拟业务逻辑
        System.out.println("创建用户: " + dto.getName());
        return Result.ok("用户创建成功");
    }

    /**
     * 更新用户的示例方法
     * 使用@OperationLog注解记录操作日志
     */
    @PutMapping("/user/{id}")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "更新用户信息", businessKey = "#id", businessType = "'USER'")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody UpdateUserDto dto) {
        // 模拟业务逻辑
        System.out.println("更新用户: " + id);
        return Result.ok("用户更新成功");
    }

    /**
     * 删除用户的示例方法
     * 使用@OperationLog注解记录操作日志
     */
    @DeleteMapping("/user/{id}")
    @OperationLog(module = "用户管理", type = "DELETE", desc = "删除用户", businessKey = "#id", businessType = "'USER'")
    public Result<String> deleteUser(@PathVariable Long id) {
        // 模拟业务逻辑
        System.out.println("删除用户: " + id);
        return Result.ok("用户删除成功");
    }

    /**
     * 查询用户的示例方法
     * 使用@OperationLog注解记录操作日志
     */
    @GetMapping("/user/{id}")
    @OperationLog(module = "用户管理", type = "QUERY", desc = "查询用户详情", businessKey = "#id", businessType = "'USER'")
    public Result<String> getUser(@PathVariable Long id) {
        // 模拟业务逻辑
        System.out.println("查询用户: " + id);
        return Result.ok("用户信息");
    }

    /**
     * 导出用户数据的示例方法
     * 使用@OperationLog注解记录操作日志
     */
    @GetMapping("/user/export")
    @OperationLog(module = "用户管理", type = "EXPORT", desc = "导出用户数据")
    public Result<String> exportUsers() {
        // 模拟业务逻辑
        System.out.println("导出用户数据");
        return Result.ok("用户数据导出完成");
    }
}

/**
 * 创建用户DTO
 */
class CreateUserDto {
    private Long id;
    private String name;
    private String email;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

/**
 * 更新用户DTO
 */
class UpdateUserDto {
    private String name;
    private String email;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
package cn.aiedge.runner;

import cn.aiedge.base.service.UserService;
import cn.aiedge.base.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时重置admin密码的Runner
 * 用于解决首次登录问题
 */
@Component
public class PasswordResetRunner implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 获取admin用户
            User adminUser = userService.getByUsername("admin");
            if (adminUser != null) {
                // 重置密码为admin123
                String newPassword = "admin123";
                String encodedPassword = passwordEncoder.encode(newPassword);
                
                // 更新密码
                userService.resetPassword(adminUser.getId(), encodedPassword);
                
                System.out.println("===================================");
                System.out.println("Admin password reset successfully!");
                System.out.println("Username: admin");
                System.out.println("Password: admin123");
                System.out.println("===================================");
            } else {
                System.out.println("Admin user not found in database.");
            }
        } catch (Exception e) {
            System.err.println("Failed to reset admin password: " + e.getMessage());
        }
    }
}

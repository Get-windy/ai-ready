package cn.aiedge.runner;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时重置admin密码的Runner
 * 使用Hutool BCrypt确保与登录模块兼容
 */
@Component
public class PasswordResetRunner implements CommandLineRunner {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 查找admin用户（忽略租户过滤，用户名全局唯一）
            SysUser adminUser = sysUserMapper.selectByUsername("admin", null);
            if (adminUser != null) {
                // 密码: admin123
                String newPassword = "admin123";
                String encodedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

                // 更新密码
                adminUser.setPassword(encodedPassword);
                sysUserMapper.updateById(adminUser);

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

package cn.aiedge.runner;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时重置admin密码的Runner
 * 使用Hutool BCrypt确保与登录模块兼容
 */
@Slf4j
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

                log.info("===================================");
                log.info("Admin password reset successfully!");
                log.info("Username: admin");
                log.info("Password: admin123");
                log.info("===================================");
            } else {
                log.warn("Admin user not found in database.");
            }
        } catch (Exception e) {
            log.error("Failed to reset admin password: " + e.getMessage());
        }
    }
}

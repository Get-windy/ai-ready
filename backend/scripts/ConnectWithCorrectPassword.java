import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ConnectWithCorrectPassword {
    public static void main(String[] args) {
        try {
            // 使用从Docker配置中找到的正确密码
            String password = "Dev@2026#Local"; // From docker-compose.yml
            String url = "jdbc:postgresql://localhost:5432/devdb";
            String username = "devuser";
            
            System.out.println("Trying to connect with correct password: " + password);
            Connection conn = DriverManager.getConnection(url, username, password);
            
            System.out.println("SUCCESS! Connected to database with correct credentials");
            
            // Generate new password hash using Hutool's BCrypt
            String newPassword = "admin123";
            String hashedPassword = cn.hutool.crypto.digest.BCrypt.hashpw(newPassword, cn.hutool.crypto.digest.BCrypt.gensalt());
            
            // Update admin user password
            String updateSql = "UPDATE sys_user SET password = ? WHERE username = 'admin'";
            PreparedStatement stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, hashedPassword);
            int rowsUpdated = stmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                System.out.println("Password reset successful!");
                System.out.println("Username: admin");
                System.out.println("New password: " + newPassword);
                System.out.println("You can now login with /api/auth/login");
                System.out.println("Credentials: admin / admin123");
            } else {
                System.out.println("No admin user found");
            }
            
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
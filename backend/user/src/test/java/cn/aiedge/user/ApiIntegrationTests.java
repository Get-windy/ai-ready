package cn.aiedge.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Integration Tests for User Management Module
 * Tests all 7 REST API endpoints:
 * 1. POST /api/v1/users - Create user
 * 2. GET /api/v1/users/{id} - Get user by ID
 * 3. GET /api/v1/users - Get user list with pagination
 * 4. PUT /api/v1/users/{id} - Update user
 * 5. POST /api/v1/auth/login - User login
 * 6. POST /api/v1/auth/logout - User logout  
 * 7. GET /api/v1/users/stats - Get user statistics
 */
@SpringBootTest
@AutoConfigureWebMvc
public class ApiIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Test
    public void contextLoads() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Test 1: Create User (POST /api/v1/users)
     */
    @Test
    public void testCreateUser() throws Exception {
        String userJson = """
            {
                "username": "test_api_user",
                "password": "Test@123",
                "email": "test_api@example.com",
                "fullName": "Test API User",
                "phone": "13800138000"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户创建成功"))
                .andExpect(jsonPath("$.data.username").value("test_api_user"))
                .andExpect(jsonPath("$.data.email").value("test_api@example.com"));
    }

    /**
     * Test 2: Get User by ID (GET /api/v1/users/{id})
     */
    @Test
    public void testGetUserById() throws Exception {
        // First create a user
        String userJson = """
            {
                "username": "test_get_user",
                "password": "Test@123", 
                "email": "test_get@example.com"
            }
            """;
        
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson));
        
        // Then get the user (assuming ID=1 for first user)
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("test_get_user"))
                .andExpect(jsonPath("$.data.email").value("test_get@example.com"));
    }

    /**
     * Test 3: Get User List (GET /api/v1/users)
     */
    @Test
    public void testGetUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Test 4: Update User (PUT /api/v1/users/{id})
     */
    @Test
    public void testUpdateUser() throws Exception {
        // Create user first
        String createUserJson = """
            {
                "username": "test_update_user",
                "password": "Test@123",
                "email": "test_update@example.com"
            }
            """;
            
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson));
        
        // Update user
        String updateUserJson = """
            {
                "fullName": "Updated Test User",
                "phone": "13900139000",
                "department": "Product",
                "position": "Product Manager"
            }
            """;
            
        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateUserJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户更新成功"));
    }

    /**
     * Test 5: User Login (POST /api/v1/auth/login)
     */
    @Test
    public void testUserLogin() throws Exception {
        // Create user first
        String createUserJson = """
            {
                "username": "test_login_user",
                "password": "Test@123",
                "email": "test_login@example.com"
            }
            """;
            
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson));
        
        // Login user
        String loginJson = """
            {
                "usernameOrEmail": "test_login_user",
                "password": "Test@123"
            }
            """;
            
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    /**
     * Test 6: User Logout (POST /api/v1/auth/logout)
     */
    @Test
    public void testUserLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }

    /**
     * Test 7: Get User Statistics (GET /api/v1/users/stats)
     */
    @Test
    public void testGetUserStats() throws Exception {
        mockMvc.perform(get("/api/v1/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").exists())
                .andExpect(jsonPath("$.data.activeUsers").exists())
                .andExpect(jsonPath("$.data.lockedUsers").exists());
    }

    /**
     * Test Error Cases
     */
    
    @Test
    public void testCreateUserDuplicateUsername() throws Exception {
        // Create first user
        String user1Json = """
            {
                "username": "duplicate_user",
                "password": "Test@123",
                "email": "user1@example.com"
            }
            """;
            
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user1Json));
        
        // Try to create user with same username
        String user2Json = """
            {
                "username": "duplicate_user",
                "password": "Test@456",
                "email": "user2@example.com"
            }
            """;
            
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user2Json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testGetNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testInvalidEmailFormat() throws Exception {
        String invalidUserJson = """
            {
                "username": "invalid_email_user",
                "password": "Test@123",
                "email": "invalid-email-format"
            }
            """;
            
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
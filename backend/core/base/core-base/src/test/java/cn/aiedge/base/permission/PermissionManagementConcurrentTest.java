package cn.aiedge.base.permission;

import cn.aiedge.base.config.TestConfig;
import cn.aiedge.base.entity.*;
import cn.aiedge.base.service.*;
import cn.aiedge.base.util.PermissionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限管理并发测试类
 * 测试权限管理模块的并发场景
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class PermissionManagementConcurrentTest {

    @Autowired
    private SysRoleService roleService;
    
    @Autowired
    private SysPermissionService permissionService;
    
    @Autowired
    private PermissionTemplateService templateService;
    
    @Autowired
    private RoleInheritanceService inheritanceService;
    
    @Autowired
    private DataPermissionService dataPermissionService;
    
    @Autowired
    private PermissionUtils permissionUtils;

    private static final int THREAD_COUNT = 10;
    private static final int OPERATION_COUNT = 100;

    @Test
    void testConcurrentRoleCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    SysRole role = new SysRole();
                    role.setRoleName("并发测试角色_" + index + "_" + System.currentTimeMillis());
                    role.setRoleCode("CONCURRENT_ROLE_" + index + "_" + System.currentTimeMillis());
                    role.setRoleType(1);
                    role.setDataScope(0);
                    role.setStatus(0);
                    role.setTenantId(1L);
                    
                    Long roleId = roleService.createRole(role);
                    if (roleId != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分角色创建成功");
        System.out.println("成功创建角色数: " + successCount.get());
        System.out.println("失败数: " + failCount.get());
    }

    @Test
    void testConcurrentRoleUpdate() throws InterruptedException {
        SysRole role = new SysRole();
        role.setRoleName("并发更新测试角色");
        role.setRoleCode("CONCURRENT_UPDATE_ROLE");
        role.setRoleType(1);
        role.setDataScope(0);
        role.setStatus(0);
        role.setTenantId(1L);
        Long roleId = roleService.createRole(role);
        assertNotNull(roleId);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    SysRole updateRole = roleService.getById(roleId);
                    updateRole.setRoleName("更新后的角色_" + index);
                    roleService.updateRole(updateRole);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分更新成功");
        System.out.println("成功更新次数: " + successCount.get());
    }

    @Test
    void testConcurrentPermissionCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    SysPermission permission = new SysPermission();
                    permission.setPermissionName("并发测试权限_" + index);
                    permission.setPermissionCode("concurrent:permission:" + index + ":" + System.currentTimeMillis());
                    permission.setPermissionType(3);
                    permission.setApiPath("/api/concurrent/" + index);
                    permission.setMethod("GET");
                    permission.setStatus(0);
                    permission.setTenantId(1L);
                    
                    Long permissionId = permissionService.createPermission(permission);
                    if (permissionId != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分权限创建成功");
        System.out.println("成功创建权限数: " + successCount.get());
    }

    @Test
    void testConcurrentRoleInheritance() throws InterruptedException {
        SysRole parentRole = new SysRole();
        parentRole.setRoleName("并发父角色");
        parentRole.setRoleCode("CONCURRENT_PARENT");
        parentRole.setRoleType(1);
        parentRole.setStatus(0);
        parentRole.setTenantId(1L);
        Long parentRoleId = roleService.createRole(parentRole);
        assertNotNull(parentRoleId);

        List<Long> childRoleIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SysRole childRole = new SysRole();
            childRole.setRoleName("并发子角色_" + i);
            childRole.setRoleCode("CONCURRENT_CHILD_" + i);
            childRole.setRoleType(1);
            childRole.setStatus(0);
            childRole.setTenantId(1L);
            Long childId = roleService.createRole(childRole);
            childRoleIds.add(childId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(childRoleIds.size());
        CountDownLatch latch = new CountDownLatch(childRoleIds.size());
        AtomicInteger successCount = new AtomicInteger(0);

        for (Long childId : childRoleIds) {
            executor.submit(() -> {
                try {
                    inheritanceService.setRoleInheritance(parentRoleId, childId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分继承关系设置成功");
        System.out.println("成功设置继承关系数: " + successCount.get());
    }

    @Test
    void testConcurrentPermissionRead() throws InterruptedException {
        SysPermission permission = new SysPermission();
        permission.setPermissionName("并发读取测试权限");
        permission.setPermissionCode("concurrent:read:test");
        permission.setPermissionType(3);
        permission.setApiPath("/api/concurrent/read");
        permission.setMethod("GET");
        permission.setStatus(0);
        permission.setTenantId(1L);
        Long permissionId = permissionService.createPermission(permission);
        assertNotNull(permissionId);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    SysPermission result = permissionService.getById(permissionId);
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(THREAD_COUNT, successCount.get(), "所有并发读取都应成功");
    }

    @Test
    void testConcurrentTemplateCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    PermissionTemplate template = new PermissionTemplate();
                    template.setTemplateName("并发测试模板_" + index);
                    template.setTemplateCode("CONCURRENT_TEMPLATE_" + index + "_" + System.currentTimeMillis());
                    template.setDescription("并发测试权限模板");
                    template.setTemplateType(2);
                    template.setStatus(0);
                    template.setTenantId(1L);
                    template.setPermissionConfig(Arrays.asList(1L, 2L, 3L));
                    
                    Long templateId = templateService.createTemplate(template);
                    if (templateId != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分模板创建成功");
        System.out.println("成功创建模板数: " + successCount.get());
    }

    @Test
    void testConcurrentDataPermissionCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    DataPermission dataPermission = new DataPermission();
                    dataPermission.setPermissionName("并发测试数据权限_" + index);
                    dataPermission.setPermissionCode("concurrent:data:" + index + ":" + System.currentTimeMillis());
                    dataPermission.setDataScope(0);
                    dataPermission.setScopeType(3);
                    dataPermission.setStatus(0);
                    dataPermission.setTenantId(1L);
                    
                    Long permissionId = dataPermissionService.createDataPermission(dataPermission);
                    if (permissionId != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "至少应有部分数据权限创建成功");
        System.out.println("成功创建数据权限数: " + successCount.get());
    }

    @Test
    void testConcurrentPermissionUtilsValidation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    boolean valid1 = permissionUtils.isValidPermissionCode("user:create:" + index);
                    boolean valid2 = permissionUtils.isValidRoleCode("ADMIN_" + index);
                    
                    if (valid1 && valid2) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(THREAD_COUNT, successCount.get(), "所有并发验证都应成功");
    }
}
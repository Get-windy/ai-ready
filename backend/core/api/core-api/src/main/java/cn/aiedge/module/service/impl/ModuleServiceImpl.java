package cn.aiedge.module.service.impl;

import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import cn.aiedge.module.service.ModuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModuleServiceImpl implements ModuleService {

    private static final AtomicLong ID_GEN = new AtomicLong(100);
    private static final AtomicLong VERSION_ID_GEN = new AtomicLong(200);

    private static final List<SysModule> MODULE_LIST = new CopyOnWriteArrayList<>();
    private static final List<SysModuleVersion> VERSION_LIST = new CopyOnWriteArrayList<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // ========== 模块数据 ==========
        addModule(1L, "用户管理", "user", "2.1.0", "用户注册、登录、权限管理", 1, "UserOutlined", 1, "admin", now);
        addModule(2L, "订单管理", "order", "1.8.3", "订单创建、审批、跟踪", 1, "ShoppingCartOutlined", 2, "admin", now);
        addModule(3L, "商品管理", "product", "3.0.1", "商品上架、分类、库存", 1, "AppstoreOutlined", 3, "admin", now);
        addModule(4L, "支付模块", "payment", "2.2.0", "支付渠道对接、账单管理", 1, "DollarOutlined", 4, "admin", now);
        addModule(5L, "消息通知", "notification", "1.5.0", "站内信、邮件、短信通知", 1, "BellOutlined", 5, "admin", now);
        addModule(6L, "数据分析", "analytics", "1.0.0", "业务数据统计与分析报表", 0, "BarChartOutlined", 6, "admin", now);
        addModule(7L, "文件管理", "file", "2.0.2", "文件上传、下载、预览", 1, "FolderOutlined", 7, "admin", now);
        addModule(8L, "系统配置", "system", "1.3.0", "系统参数配置与维护", 1, "SettingOutlined", 8, "admin", now);

        // ========== 版本数据 ==========
        addVersion(1L, 1L, "用户管理", "2.1.0", "新增LDAP认证支持\\n修复角色权限继承问题\\n优化用户搜索性能", "released", "admin", now.minusDays(5));
        addVersion(2L, 1L, "用户管理", "2.0.0", "全面重构权限体系\\n新增组织架构管理\\n引入RBAC模型", "released", "admin", now.minusMonths(1));
        addVersion(3L, 1L, "用户管理", "1.3.0", "新增第三方登录支持\\n修复会话超时缺陷", "released", "admin", now.minusMonths(2));
        addVersion(4L, 2L, "订单管理", "1.8.3", "修复并发下单库存超卖问题\\n优化订单查询SQL性能", "released", "admin", now.minusDays(3));
        addVersion(5L, 2L, "订单管理", "1.8.2", "新增订单批量导出功能\\n修复退款流程图异常", "released", "admin", now.minusDays(15));
        addVersion(6L, 2L, "订单管理", "1.8.0", "新增订单拆分与合并功能\\n优化审批流程配置", "released", "admin", now.minusMonths(1));
        addVersion(7L, 3L, "商品管理", "3.0.1", "修复商品详情页缓存穿透问题", "released", "admin", now.minusDays(1));
        addVersion(8L, 3L, "商品管理", "3.0.0", "全新商品管理界面\\n支持多规格SKU\\n新增批量导入导出", "released", "admin", now.minusDays(7));
        addVersion(9L, 4L, "支付模块", "2.2.0", "新增银联支付渠道\\n优化支付超时处理逻辑", "released", "admin", now.minusDays(10));
        addVersion(10L, 4L, "支付模块", "2.1.0", "新增微信支付V3接口\\n修复退款回调幂等性问题", "released", "admin", now.minusMonths(2));
        addVersion(11L, 5L, "消息通知", "1.5.0", "新增钉钉/企业微信机器人通知\\n优化消息队列消费性能", "released", "admin", now.minusDays(12));
        addVersion(12L, 6L, "数据分析", "1.0.0", "首个版本发布\\n支持基础报表与数据看板", "draft", "admin", now.minusDays(20));
        addVersion(13L, 7L, "文件管理", "2.0.2", "修复大文件分片上传内存溢出问题\\n新增图片压缩选项", "released", "admin", now.minusDays(8));
        addVersion(14L, 8L, "系统配置", "1.3.0", "新增配置导入导出功能\\n优化配置缓存策略", "released", "admin", now.minusDays(6));
    }

    private static void addModule(Long id, String name, String code, String version,
                                  String desc, Integer status, String icon,
                                  Integer sortOrder, String createBy, LocalDateTime now) {
        SysModule m = new SysModule();
        m.setId(id);
        m.setModuleName(name);
        m.setModuleCode(code);
        m.setVersion(version);
        m.setDescription(desc);
        m.setStatus(status);
        m.setIcon(icon);
        m.setSortOrder(sortOrder);
        m.setTenantId(1L);
        m.setCreateTime(now.minusMonths(6));
        m.setUpdateTime(now);
        m.setCreateBy(createBy);
        m.setUpdateBy(createBy);
        m.setDeleted(0);
        MODULE_LIST.add(m);
    }

    private static void addVersion(Long id, Long moduleId, String moduleName, String version,
                                   String changelog, String releaseStatus, String publisher,
                                   LocalDateTime releaseTime) {
        SysModuleVersion v = new SysModuleVersion();
        v.setId(id);
        v.setModuleId(moduleId);
        v.setModuleName(moduleName);
        v.setVersion(version);
        v.setChangelog(changelog);
        v.setReleaseStatus(releaseStatus);
        v.setPublisher(publisher);
        v.setReleaseTime(releaseTime);
        v.setTenantId(1L);
        v.setCreateTime(releaseTime);
        v.setUpdateTime(releaseTime);
        VERSION_LIST.add(v);
    }

    @Override
    public List<SysModule> getModuleList() {
        return MODULE_LIST.stream()
                .filter(m -> m.getDeleted() == 0)
                .sorted(Comparator.comparingInt(SysModule::getSortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public SysModule getModuleById(Long id) {
        return MODULE_LIST.stream()
                .filter(m -> m.getId().equals(id) && m.getDeleted() == 0)
                .findFirst()
                .orElse(null);
    }

    @Override
    public SysModule createModule(SysModule module) {
        long id = ID_GEN.incrementAndGet();
        module.setId(id);
        module.setTenantId(1L);
        module.setDeleted(0);
        module.setCreateTime(LocalDateTime.now());
        module.setUpdateTime(LocalDateTime.now());
        if (module.getStatus() == null) {
            module.setStatus(1);
        }
        if (module.getSortOrder() == null) {
            module.setSortOrder(MODULE_LIST.size() + 1);
        }
        MODULE_LIST.add(module);
        log.info("创建模块: id={}, name={}", id, module.getModuleName());
        return module;
    }

    @Override
    public SysModule updateModule(SysModule module) {
        for (int i = 0; i < MODULE_LIST.size(); i++) {
            SysModule existing = MODULE_LIST.get(i);
            if (existing.getId().equals(module.getId()) && existing.getDeleted() == 0) {
                module.setTenantId(existing.getTenantId());
                module.setCreateTime(existing.getCreateTime());
                module.setUpdateTime(LocalDateTime.now());
                module.setDeleted(existing.getDeleted());
                MODULE_LIST.set(i, module);
                log.info("更新模块: id={}, name={}", module.getId(), module.getModuleName());
                return module;
            }
        }
        return null;
    }

    @Override
    public boolean deleteModule(Long id) {
        for (int i = 0; i < MODULE_LIST.size(); i++) {
            SysModule m = MODULE_LIST.get(i);
            if (m.getId().equals(id) && m.getDeleted() == 0) {
                m.setDeleted(1);
                m.setUpdateTime(LocalDateTime.now());
                MODULE_LIST.set(i, m);
                log.info("删除模块: id={}", id);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean toggleStatus(Long id) {
        for (int i = 0; i < MODULE_LIST.size(); i++) {
            SysModule m = MODULE_LIST.get(i);
            if (m.getId().equals(id) && m.getDeleted() == 0) {
                m.setStatus(m.getStatus() == 1 ? 0 : 1);
                m.setUpdateTime(LocalDateTime.now());
                MODULE_LIST.set(i, m);
                log.info("切换模块状态: id={}, newStatus={}", id, m.getStatus());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<SysModuleVersion> getVersionList() {
        return new ArrayList<>(VERSION_LIST);
    }

    @Override
    public SysModuleVersion publishVersion(Long moduleId, String version, String changelog) {
        SysModule module = getModuleById(moduleId);
        if (module == null) {
            log.warn("发布版本失败，模块不存在: moduleId={}", moduleId);
            return null;
        }

        long id = VERSION_ID_GEN.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();

        SysModuleVersion v = new SysModuleVersion();
        v.setId(id);
        v.setModuleId(moduleId);
        v.setModuleName(module.getModuleName());
        v.setVersion(version);
        v.setChangelog(changelog);
        v.setReleaseStatus("released");
        v.setPublisher("admin");
        v.setReleaseTime(now);
        v.setTenantId(1L);
        v.setCreateTime(now);
        v.setUpdateTime(now);

        VERSION_LIST.add(v);

        // 同步更新模块当前版本号
        module.setVersion(version);
        module.setUpdateTime(now);
        for (int i = 0; i < MODULE_LIST.size(); i++) {
            if (MODULE_LIST.get(i).getId().equals(moduleId)) {
                MODULE_LIST.set(i, module);
                break;
            }
        }

        log.info("发布版本: moduleId={}, version={}", moduleId, version);
        return v;
    }

    @Override
    public List<Map<String, Object>> getUsageStats() {
        List<Map<String, Object>> stats = new ArrayList<>();

        Map<String, Object> userStat = new LinkedHashMap<>();
        userStat.put("moduleName", "用户管理");
        userStat.put("moduleCode", "user");
        userStat.put("callCount", 12580);
        userStat.put("activeUsers", 342);
        userStat.put("avgResponseTime", 45);
        stats.add(userStat);

        Map<String, Object> orderStat = new LinkedHashMap<>();
        orderStat.put("moduleName", "订单管理");
        orderStat.put("moduleCode", "order");
        orderStat.put("callCount", 8920);
        orderStat.put("activeUsers", 128);
        orderStat.put("avgResponseTime", 120);
        stats.add(orderStat);

        Map<String, Object> productStat = new LinkedHashMap<>();
        productStat.put("moduleName", "商品管理");
        productStat.put("moduleCode", "product");
        productStat.put("callCount", 6540);
        productStat.put("activeUsers", 95);
        productStat.put("avgResponseTime", 68);
        stats.add(productStat);

        Map<String, Object> paymentStat = new LinkedHashMap<>();
        paymentStat.put("moduleName", "支付模块");
        paymentStat.put("moduleCode", "payment");
        paymentStat.put("callCount", 13200);
        paymentStat.put("activeUsers", 56);
        paymentStat.put("avgResponseTime", 200);
        stats.add(paymentStat);

        Map<String, Object> notificationStat = new LinkedHashMap<>();
        notificationStat.put("moduleName", "消息通知");
        notificationStat.put("moduleCode", "notification");
        notificationStat.put("callCount", 45000);
        notificationStat.put("activeUsers", 500);
        notificationStat.put("avgResponseTime", 30);
        stats.add(notificationStat);

        Map<String, Object> analyticsStat = new LinkedHashMap<>();
        analyticsStat.put("moduleName", "数据分析");
        analyticsStat.put("moduleCode", "analytics");
        analyticsStat.put("callCount", 2100);
        analyticsStat.put("activeUsers", 30);
        analyticsStat.put("avgResponseTime", 3500);
        stats.add(analyticsStat);

        Map<String, Object> fileStat = new LinkedHashMap<>();
        fileStat.put("moduleName", "文件管理");
        fileStat.put("moduleCode", "file");
        fileStat.put("callCount", 7800);
        fileStat.put("activeUsers", 210);
        fileStat.put("avgResponseTime", 150);
        stats.add(fileStat);

        Map<String, Object> systemStat = new LinkedHashMap<>();
        systemStat.put("moduleName", "系统配置");
        systemStat.put("moduleCode", "system");
        systemStat.put("callCount", 3200);
        systemStat.put("activeUsers", 15);
        systemStat.put("avgResponseTime", 25);
        stats.add(systemStat);

        return stats;
    }
}

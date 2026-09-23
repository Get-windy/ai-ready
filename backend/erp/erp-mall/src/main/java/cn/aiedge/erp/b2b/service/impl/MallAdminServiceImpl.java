package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpProductMallMapper;
import cn.aiedge.erp.b2b.dao.ErpProductWrite;
import cn.aiedge.erp.b2b.dao.ErpProductWriteMapper;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderItemMallMapper;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMallMapper;
import cn.aiedge.erp.b2b.dto.MallOrderItemPageDTO;
import cn.aiedge.erp.b2b.dto.MallOrderRefundRequest;
import cn.aiedge.erp.b2b.dto.MallOrderShipRequest;
import cn.aiedge.erp.b2b.dto.ShopUserUpdateRequest;
import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;
import cn.aiedge.erp.b2b.mapper.MallProductMapper;
import cn.aiedge.erp.b2b.mapper.MallTradeAnalysisMapper;
import cn.aiedge.erp.b2b.mapper.ShopBannerMapper;
import cn.aiedge.erp.b2b.mapper.ShopConfigMapper;
import cn.aiedge.erp.b2b.mapper.ShopDecorationMapper;
import cn.aiedge.erp.b2b.mapper.ShopDecorationProductMapper;
import cn.aiedge.erp.b2b.mapper.ShopTemplateMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserTenantMapper;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopDecoration;
import cn.aiedge.erp.b2b.model.ShopDecorationProduct;
import cn.aiedge.erp.b2b.model.ShopTemplate;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.model.ShopUserTenant;
import cn.aiedge.erp.b2b.service.MallAdminService;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.service.CustomerGradeService;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.trade.monitor.TimeParsers;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class MallAdminServiceImpl implements MallAdminService {

    private final ShopConfigMapper shopConfigMapper;
    /** 「系统顾客 × 租户」关联：本店顾客是谁、审核到哪一步，**唯一事实来源**（§11.3 阶段 1）。 */
    private final ShopUserTenantMapper shopUserTenantMapper;
    private final ShopUserMapper shopUserMapper;
    private final ShopBannerMapper shopBannerMapper;
    private final ShopTemplateMapper shopTemplateMapper;
    /** 装修配置（shop_decoration，Flyway V11.366.0） */
    private final ShopDecorationMapper shopDecorationMapper;
    /** 装修配置-应用商品关联（shop_decoration_product，Flyway V11.366.0） */
    private final ShopDecorationProductMapper shopDecorationProductMapper;
    /**
     * 【已废弃表】mall_product Mapper。
     *
     * <p>mall_product 已在 V9.0.0 Part 9 标注为「[已废弃] 由 v_mall_product 视图替代，数据源为 erp_product」。
     * 商品上架页的读（{@link #pageProducts}）与编辑保存（{@link #saveProduct} 的 id != null 分支）
     * 均已改走权威表 erp_product。本字段剩余使用点仅两处，均在 {@code erp-mall} 内：</p>
     * <ul>
     *   <li>{@link #saveProduct} 的<b>新增分支</b>（POST /product）：缺口未闭环，见方法注释；</li>
     *   <li>{@link #deleteProduct}（DELETE /product/{id}）：仍按旧表定位与软删，未一并改（越出本次修复范围）；</li>
     * </ul>
     * <p>模块外另见 {@code MallOrderServiceImpl}（订单履约时读/回写 mall_product 的库存与销量口径），
     * 属同一数据源遗留问题，不在本次范围内。</p>
     */
    private final MallProductMapper mallProductMapper;
    /** v_mall_product 视图 Mapper（商品上架页列表数据源，含 V11.361.6 追加的对标列） */
    private final ErpProductMallMapper erpProductMallMapper;
    /** 【权威写入】erp_product 表写入 Mapper（商品上架页编辑弹窗保存目标） */
    private final ErpProductWriteMapper erpProductWriteMapper;
    private final ErpSaleOrderMallMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMallMapper erpSaleOrderItemMapper;
    private final MallTradeAnalysisMapper mallTradeAnalysisMapper;
    private final SecurityContext securityContext;
    /** 客户级别主数据（biz_customer_grade）：/user/page?gradeId= 过滤时把级别ID解析为级别名称 */
    private final CustomerGradeService customerGradeService;
    /**
     * 批量操作逐条独立事务（REQUIRES_NEW）：单条状态校验失败只回滚自身，
     * 不会把同一批次中已成功条目一并回滚，使「返回成功条数」真实可信。
     */
    private final TransactionTemplate batchTxTemplate;

    public MallAdminServiceImpl(ShopConfigMapper shopConfigMapper,
                                ShopUserMapper shopUserMapper,
                                ShopUserTenantMapper shopUserTenantMapper,
                                ShopBannerMapper shopBannerMapper,
                                ShopTemplateMapper shopTemplateMapper,
                                ShopDecorationMapper shopDecorationMapper,
                                ShopDecorationProductMapper shopDecorationProductMapper,
                                MallProductMapper mallProductMapper,
                                ErpProductMallMapper erpProductMallMapper,
                                ErpProductWriteMapper erpProductWriteMapper,
                                ErpSaleOrderMallMapper erpSaleOrderMapper,
                                ErpSaleOrderItemMallMapper erpSaleOrderItemMapper,
                                MallTradeAnalysisMapper mallTradeAnalysisMapper,
                                SecurityContext securityContext,
                                CustomerGradeService customerGradeService,
                                PlatformTransactionManager transactionManager) {
        this.shopConfigMapper = shopConfigMapper;
        this.shopUserMapper = shopUserMapper;
        this.shopUserTenantMapper = shopUserTenantMapper;
        this.shopBannerMapper = shopBannerMapper;
        this.shopTemplateMapper = shopTemplateMapper;
        this.shopDecorationMapper = shopDecorationMapper;
        this.shopDecorationProductMapper = shopDecorationProductMapper;
        this.mallProductMapper = mallProductMapper;
        this.erpProductMallMapper = erpProductMallMapper;
        this.erpProductWriteMapper = erpProductWriteMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.mallTradeAnalysisMapper = mallTradeAnalysisMapper;
        this.securityContext = securityContext;
        this.customerGradeService = customerGradeService;
        this.batchTxTemplate = new TransactionTemplate(transactionManager);
        this.batchTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 每批最多处理条数（批量审核/批量发货，避免单次请求过大） */
    private static final int MAX_BATCH_SIZE = 200;

    /** 已支付（payment_status=2） */
    private static final int PAY_STATUS_PAID = 2;
    /** 已退款（payment_status=4） */
    private static final int PAY_STATUS_REFUNDED = 4;
    /** 已发货（delivery_status=2） */
    private static final int DELIVERY_STATUS_SHIPPED = 2;

    private Long getCurrentTenantId() {
        return securityContext.getCurrentTenantId();
    }

    /** 当前登录人（无会话时返回 null，避免写操作 500） */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /** 读取订单并做租户归属校验 */
    private ErpSaleOrderMall requireOrder(Long orderId) {
        Long tenantId = getCurrentTenantId();
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(orderId);
        if (order == null || !tenantId.equals(order.getTenantId())) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    // ==================== 商城配置 ====================

    @Override
    public ShopConfig getConfig() {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ShopConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopConfig::getTenantId, tenantId);
        return shopConfigMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ShopConfig config) {
        Long tenantId = getCurrentTenantId();
        ShopConfig existing = shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>().eq(ShopConfig::getTenantId, tenantId));
        if (existing == null) {
            config.setId(null);
            config.setTenantId(tenantId);
            config.setStatus(1);
            shopConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setTenantId(tenantId);
            config.setUpdateBy(StpUtil.getLoginIdAsLong());
            config.setUpdateTime(LocalDateTime.now());
            shopConfigMapper.updateById(config);
        }
    }

    // ==================== 商城用户审核 ====================

    @Override
    public Page<ShopUser> pageUsers(Integer pageNum, Integer pageSize, String keyword,
                                    Integer auditStatus, Integer status,
                                    String createTimeStart, String createTimeEnd,
                                    Long categoryId, Long gradeId, Boolean showDisabled) {
        Long tenantId = getCurrentTenantId();

        // ★ 本店顾客 = shop_user_tenant 里属于本租户的关联（§11.3 阶段 1：审核改挂关联表）。
        // ⚠️ 不能再按 shop_user.tenant_id 过滤：顾客已升为**系统级身份**、该列恒 0，
        //    那样过滤的结果是**永远空列表**（本次修掉的正是这个）。
        //    审核状态口径：前端 0待审/1通过/2驳回，与关联表 status 的取值一一对应，直接透传。
        //
        // ★ 启用/停用也**逐租户**（`shop_user_tenant.enabled`，本批新增）：
        //   前端"停用"传的 status=0/1 与它一一对应 ⇒ 列表过滤与回填都走关联表，
        //   否则 A 店停用的顾客在 B 店的列表里也会显示成停用（而他在 B 店其实能买）。
        //   显示停用（买家账号页勾选框）：false = 只看启用账号（强制 enabled=1，忽略 status 参数）；
        //   true / null = 不过滤停用，此时仍可用 status 参数显式指定
        //
        // ⚠️ 2026-09-23 修拆箱 NPE：原写法
        //   `Boolean.FALSE.equals(showDisabled) ? ShopUserTenant.ENABLED_YES : status`
        //   因 ENABLED_YES 是 **int 基本类型**，条件表达式的类型经二元数值提升后为 int，
        //   于是**两个分支都会把 Integer status 拆箱** —— status 为 null（前端默认不传）时
        //   直接 NPE，/erp/mall/admin/user/page 恒 500（买家账号、买家申请管理两页打不开）。
        //   现改为三目只在分支处装箱，null 时保持"不过滤"的原语义。
        Integer enabledFilter = Boolean.FALSE.equals(showDisabled)
                ? Integer.valueOf(ShopUserTenant.ENABLED_YES)
                : status;
        List<ShopUserTenant> links = shopUserTenantMapper.selectByTenant(tenantId, auditStatus, enabledFilter);
        if (links.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        Map<Long, Integer> shopStatusOf = new LinkedHashMap<>();
        Map<Long, Integer> shopEnabledOf = new LinkedHashMap<>();
        for (ShopUserTenant l : links) {
            shopStatusOf.putIfAbsent(l.getShopUserId(), l.getStatus());
            shopEnabledOf.putIfAbsent(l.getShopUserId(), l.getEnabled());
        }

        LambdaQueryWrapper<ShopUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ShopUser::getId, shopStatusOf.keySet());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ShopUser::getUsername, keyword)
                    .or().like(ShopUser::getNickname, keyword)
                    .or().like(ShopUser::getPhone, keyword)
                    .or().like(ShopUser::getCompanyName, keyword));
        }
        // 注册时间范围（shop_user.create_time；起始含当日 00:00:00，截止含当日 23:59:59）
        LocalDateTime start = parseRangeStart(createTimeStart);
        if (start != null) {
            wrapper.ge(ShopUser::getCreateTime, start);
        }
        LocalDateTime end = parseRangeEnd(createTimeEnd);
        if (end != null) {
            wrapper.le(ShopUser::getCreateTime, end);
        }
        // 归属分类（shop_user.category_id，分类树选中后回传）
        if (categoryId != null) {
            wrapper.eq(ShopUser::getCategoryId, categoryId);
        }
        // 客户级别：shop_user 无 grade_id 列，级别以**名称**存于 customer_level，
        // 故把 biz_customer_grade.id 解析为 grade_name 后按名称匹配（与页面「客户级别」列展示口径一致）
        String gradeName = resolveGradeName(gradeId);
        if (gradeName != null) {
            wrapper.eq(ShopUser::getCustomerLevel, gradeName);
        }

        wrapper.orderByDesc(ShopUser::getCreateTime);
        Page<ShopUser> page = shopUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 回填「**本店**视角"的两个状态：
        //   · auditStatus ← 关联表 status（准入审核）；shop_user.audit_status 已降级为历史列；
        //   · status      ← 关联表 enabled（本店启用/停用）；shop_user.status 是**平台级**开关，
        //     直接回显它会把"A 店停用"显示成"B 店也停用"（前端就是读这个字段渲染开关的）。
        for (ShopUser u : page.getRecords()) {
            Integer shopStatus = shopStatusOf.get(u.getId());
            if (shopStatus != null) {
                u.setAuditStatus(shopStatus);
            }
            Integer shopEnabled = shopEnabledOf.get(u.getId());
            if (shopEnabled != null) {
                u.setStatus(shopEnabled);
            }
        }
        return page;
    }

    /**
     * 客户级别ID → 级别名称（biz_customer_grade.grade_name）。
     * 级别不存在/无名称时返回 null（= 该条件不过滤），不抛异常中断列表查询。
     */
    private String resolveGradeName(Long gradeId) {
        if (gradeId == null) {
            return null;
        }
        try {
            CustomerGrade grade = customerGradeService.getById(gradeId);
            if (grade != null && isNotBlank(grade.getGradeName())) {
                return grade.getGradeName().trim();
            }
        } catch (Exception e) {
            log.warn("解析客户级别失败: gradeId={}", gradeId, e);
        }
        log.warn("客户级别不存在或无名称，忽略该过滤条件: gradeId={}", gradeId);
        return null;
    }

    /** 查询时间(起)：yyyy-MM-dd → 当日 00:00:00；亦兼容 ISO 日期时间串；无法解析则不过滤 */
    private static LocalDateTime parseRangeStart(String value) {
        if (!isNotBlank(value)) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.length() <= 10) {
                return LocalDate.parse(v).atStartOfDay();
            }
            return LocalDateTime.parse(v.replace(' ', 'T'));
        } catch (Exception e) {
            log.warn("无法解析查询时间(起)，忽略该条件: {}", value);
            return null;
        }
    }

    /** 查询时间(止)：yyyy-MM-dd → 当日 23:59:59（截止日含当天整天）；亦兼容 ISO 日期时间串 */
    private static LocalDateTime parseRangeEnd(String value) {
        if (!isNotBlank(value)) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.length() <= 10) {
                return LocalDate.parse(v).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(v.replace(' ', 'T'));
        } catch (Exception e) {
            log.warn("无法解析查询时间(止)，忽略该条件: {}", value);
            return null;
        }
    }

    /**
     * 单据日期(起)：纯日期 → 当日 00:00:00，亦兼容 {@code yyyy-MM-dd HH:mm:ss} / ISO；
     * 未传或无法解析返回 {@code null}（不过滤）。
     */
    private LocalDateTime orderDateFrom(String value) {
        LocalDateTime parsed = TimeParsers.parse(value);
        if (parsed == null && isNotBlank(value)) {
            log.warn("无法解析单据日期(起)，忽略该条件: {}", value);
        }
        return parsed;
    }

    /**
     * 单据日期(止)：只传日期（00:00:00）时按「含当日」补足为次日 00:00:00
     * （与 core-payment {@code endOfRange} 同口径），调用方用 {@code lt} 比较，
     * 避免 23:59:59 口径漏掉当日最后 1 秒内的数据；未传或无法解析返回 {@code null}（不过滤）。
     */
    private LocalDateTime orderDateTo(String value) {
        LocalDateTime parsed = TimeParsers.parse(value);
        if (parsed == null) {
            if (isNotBlank(value)) {
                log.warn("无法解析单据日期(止)，忽略该条件: {}", value);
            }
            return null;
        }
        return parsed.toLocalTime().equals(LocalTime.MIDNIGHT) ? parsed.plusDays(1) : parsed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        // ⚠️ 归属判断改走**关联表**（shop_user.tenant_id 现在恒 0，旧判断已失效 ⇒ 会变成谁都能删）
        requireShopLink(userId);
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("买家账号不存在");
        }
        // 逻辑删除：ShopUser.deleted 带 @TableLogic，deleteById 实际生成
        // UPDATE shop_user SET deleted=1 WHERE id=? AND deleted=0，物理行保留（可追溯、不破坏订单外键语义）
        int affected = shopUserMapper.deleteById(userId);
        if (affected <= 0) {
            throw BusinessException.badRequest("买家账号删除失败（记录不存在或已被删除）");
        }
        log.info("买家账号逻辑删除: id={}, username={}", userId, user.getUsername());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopUser updateUser(Long userId, ShopUserUpdateRequest request) {
        // ⚠️ 同上：归属改走关联表
        requireShopLink(userId);
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("买家账号不存在");
        }
        if (request == null) {
            throw BusinessException.badRequest("编辑内容不能为空");
        }

        // ── 部分更新：字段为 null 表示不修改；String 传空串表示清空（统一归一为 NULL）──
        // 注意：id / tenantId / deleted / password / auditStatus / partyId 不开放编辑，
        // 由 DTO 隔离，避免前端越权改写。
        if (request.getNickname() != null) {
            user.setNickname(trimToNull(request.getNickname()));
        }
        if (request.getContactName() != null) {
            user.setContactName(trimToNull(request.getContactName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getCompanyName() != null) {
            user.setCompanyName(trimToNull(request.getCompanyName()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trimToNull(request.getEmail()));
        }
        if (request.getQq() != null) {
            user.setQq(trimToNull(request.getQq()));
        }
        if (request.getWechat() != null) {
            user.setWechat(trimToNull(request.getWechat()));
        }
        if (request.getAddress() != null) {
            user.setAddress(trimToNull(request.getAddress()));
        }
        if (request.getRemark() != null) {
            user.setRemark(trimToNull(request.getRemark()));
        }
        if (request.getCustomerLevel() != null) {
            user.setCustomerLevel(trimToNull(request.getCustomerLevel()));
        }
        // 归属分类（数值型不支持清空，仅支持改选）
        if (request.getCategoryId() != null) {
            user.setCategoryId(request.getCategoryId());
        }
        // 归属三件套（默认经手人 / 所属仓库 / 所属部门）成对提交：
        // id 或 name 任一非 null 即视为提交（name 传空串 → 同时清空 id，实现取消归属）
        applyHandler(user, request);
        applyWarehouse(user, request);
        applyDept(user, request);

        user.setUpdateBy(getCurrentUserId());
        shopUserMapper.updateById(user);
        log.info("买家账号编辑: id={}, username={}", userId, user.getUsername());
        // 回读库中最新行，保证前端拿到真实落库结果（而非仅内存态）
        return shopUserMapper.selectById(userId);
    }

    /** 默认经手人（id + name 成对提交） */
    private void applyHandler(ShopUser user, ShopUserUpdateRequest request) {
        if (request.getDefaultHandlerId() != null || request.getDefaultHandlerName() != null) {
            user.setDefaultHandlerId(request.getDefaultHandlerId());
            user.setDefaultHandlerName(trimToNull(request.getDefaultHandlerName()));
        }
    }

    /** 所属仓库（id + name 成对提交） */
    private void applyWarehouse(ShopUser user, ShopUserUpdateRequest request) {
        if (request.getWarehouseId() != null || request.getWarehouseName() != null) {
            user.setWarehouseId(request.getWarehouseId());
            user.setWarehouseName(trimToNull(request.getWarehouseName()));
        }
    }

    /** 所属部门（id + name 成对提交） */
    private void applyDept(ShopUser user, ShopUserUpdateRequest request) {
        if (request.getDeptId() != null || request.getDeptName() != null) {
            user.setDeptId(request.getDeptId());
            user.setDeptName(trimToNull(request.getDeptName()));
        }
    }

    // ══════════════════════ 顾客审核（**逐租户**，§11.3 阶段 1） ══════════════════════
    //
    // ⚠️ 审核结果落在 shop_user_tenant（关联表），**不是** shop_user.audit_status：
    //    顾客是系统级身份，同一个顾客在 A 店被批准、在 B 店被拒绝是合法状态。
    //    旧实现在 shop_user 上写审核状态 + 按 shop_user.tenant_id 判归属 ⇒
    //    顾客升系统级（该列恒 0）之后，这里的判归属**恒不成立**（永远 404），
    //    商城顾客页也永远空列表 —— 本批一并修正。

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveUser(Long userId) {
        ShopUserTenant link = requireShopLink(userId);
        link.setStatus(ShopUserTenant.STATUS_ACTIVE);
        link.setAuditBy(StpUtil.getLoginIdAsLong());
        link.setAuditTime(LocalDateTime.now());
        link.setRejectReason(null);
        shopUserTenantMapper.updateById(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectUser(Long userId, String reason) {
        ShopUserTenant link = requireShopLink(userId);
        link.setStatus(ShopUserTenant.STATUS_REJECTED);
        link.setAuditBy(StpUtil.getLoginIdAsLong());
        link.setAuditTime(LocalDateTime.now());
        link.setRejectReason(reason);
        shopUserTenantMapper.updateById(link);
    }

    /**
     * 取「这个系统顾客在**本店**的关联」；没有就 404。
     *
     * <p>它是本类所有"按顾客 id 操作"的入口守卫：顾客是系统级的，
     * 不先确认"他属于本店"，任何一个租户管理员都能改到别家的顾客
     * （旧代码靠 {@code shop_user.tenant_id} 判，那一列现在恒 0，守卫已失效）。</p>
     */
    private ShopUserTenant requireShopLink(Long shopUserId) {
        ShopUserTenant link = shopUserTenantMapper.selectLink(shopUserId, getCurrentTenantId());
        if (link == null) {
            throw BusinessException.notFound("该顾客不属于本店");
        }
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId, Integer status) {
        // 先确认"他是本店顾客"再看账号：否则任一租户管理员都能停用别家的顾客
        // （旧代码按 shop_user.tenant_id 判，那一列现在恒 0，守卫已失效）
        //
        // ⚠️ 写的是**关联表**的 enabled，不是 shop_user.status：
        //    后者是平台级账号开关，租户管理员点一次"停用"会让该顾客**在所有店**都进不去
        //    （A 店的运营动作影响 B 店）。本批把它下沉为逐租户。
        ShopUserTenant link = requireShopLink(userId);
        link.setEnabled(ShopUserTenant.ENABLED_YES == status ? ShopUserTenant.ENABLED_YES
                : ShopUserTenant.ENABLED_NO);
        shopUserTenantMapper.updateById(link);
        log.info("本店顾客启用状态变更: shopUserId={}, 本店租户={}, enabled={}",
                userId, getCurrentTenantId(), link.getEnabled());
    }

    // ==================== 轮播图管理 ====================

    @Override
    public List<ShopBanner> listBanners() {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ShopBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopBanner::getTenantId, tenantId);
        wrapper.orderByAsc(ShopBanner::getSortOrder);
        return shopBannerMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBanner(ShopBanner banner) {
        banner.setId(null);
        banner.setTenantId(getCurrentTenantId());
        banner.setCreateBy(StpUtil.getLoginIdAsLong());
        shopBannerMapper.insert(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(ShopBanner banner) {
        Long tenantId = getCurrentTenantId();
        ShopBanner existing = shopBannerMapper.selectById(banner.getId());
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("轮播图不存在");
        }
        banner.setTenantId(tenantId);
        banner.setUpdateBy(StpUtil.getLoginIdAsLong());
        banner.setUpdateTime(LocalDateTime.now());
        shopBannerMapper.updateById(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        Long tenantId = getCurrentTenantId();
        ShopBanner banner = shopBannerMapper.selectById(id);
        if (banner == null || !banner.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("轮播图不存在");
        }
        shopBannerMapper.deleteById(id);
    }

    // ==================== 商品管理 ====================

    @Override
    public Page<ErpProductMall> pageProducts(Integer pageNum, Integer pageSize, String keyword, String categoryId,
                                             String status, String brand, String productName, String productTag,
                                             String couponUsed, String productType, String visibleStatus) {
        Long tenantId = getCurrentTenantId();
        // 商品上架页数据源为 v_mall_product 视图（ErpProductMall），而非已废弃的 mall_product 表：
        // 视图已由 V11.361.6 扩列，28 个对标列（条码/规格/型号/产地/品牌/单位/批发价/预设进价/
        // 排序/排序值/起订量/商品积分/备注/关键字/商品类型/显示状态/商品标签/优惠券 + 等级价聚合）
        // 均可直接读取；视图未提供的列（20 个食品分类独立标记）由前端按 mall_tags 槽位映射。
        LambdaQueryWrapper<ErpProductMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpProductMall::getTenantId, tenantId);
        wrapper.eq(ErpProductMall::getDeleted, 0);

        // 筛选条件：商品名称 / 商品货号 / 条码 / 规格 / 型号（对标查询区单框多字段口径）
        if (isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ErpProductMall::getProductName, kw)
                    .or().like(ErpProductMall::getProductCode, kw)
                    .or().like(ErpProductMall::getProductId, kw)
                    .or().like(ErpProductMall::getBarcode, kw)
                    .or().like(ErpProductMall::getSpecification, kw)
                    .or().like(ErpProductMall::getModelNo, kw));
        }
        // 品牌（erp_product.brand）
        if (isNotBlank(brand)) {
            wrapper.like(ErpProductMall::getBrand, brand.trim());
        }
        // 商品（商品名称精确到模糊匹配）
        if (isNotBlank(productName)) {
            wrapper.like(ErpProductMall::getProductName, productName.trim());
        }
        // 商品标签（erp_product.mall_tags，逗号分隔槽位编码，按包含匹配）
        if (isNotBlank(productTag)) {
            wrapper.like(ErpProductMall::getProductTag, productTag.trim());
        }
        // 分类（前端传 erp 分类 id 字符串；视图 category_id 为 CAST(category_id AS TEXT)）
        if (isNotBlank(categoryId)) {
            wrapper.eq(ErpProductMall::getCategoryId, categoryId.trim());
        }
        // 上架状态（视图 status：ON_SHELF / INACTIVE；兼容前端 OFF_SHELF 口径）
        if (isNotBlank(status)) {
            wrapper.eq(ErpProductMall::getStatus, normalizeShelfStatus(status));
        }
        // 使用优惠券（erp_product.use_coupon → 视图 coupon_used 布尔）
        Integer couponFlag = parseYesNo(couponUsed);
        if (couponFlag != null) {
            wrapper.eq(ErpProductMall::getCouponUsed, couponFlag == 1);
        }
        // 商品类型（erp_product.product_type：SINGLE/KIT/SERVICE）
        if (isNotBlank(productType)) {
            wrapper.eq(ErpProductMall::getProductType, productType.trim());
        }
        // 显示状态（erp_product.status：ENABLED/DISABLED）
        if (isNotBlank(visibleStatus)) {
            wrapper.eq(ErpProductMall::getVisibleStatus, visibleStatus.trim());
        }
        wrapper.orderByDesc(ErpProductMall::getCreateTime);
        return erpProductMallMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 空串归一判定（查询条件为空时不过滤） */
    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 上架状态归一：前端可能传 ON_SHELF / OFF_SHELF / INACTIVE，
     * 视图 v_mall_product.status 只有 ON_SHELF（上架）与 INACTIVE（下架）两值。
     */
    private static String normalizeShelfStatus(String status) {
        String v = status.trim().toUpperCase();
        return switch (v) {
            case "ON_SHELF", "ON_SALE", "SHELF_ON", "上架" -> "ON_SHELF";
            case "OFF_SHELF", "OFF_SALE", "SHELF_OFF", "下架", "INACTIVE" -> "INACTIVE";
            default -> v;
        };
    }

    /** 是/否查询值解析：1=是 0=否，无法识别返回 null（不过滤） */
    private static Integer parseYesNo(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "yes", "y", "1", "true", "是" -> 1;
            case "no", "n", "0", "false", "否" -> 0;
            default -> null;
        };
    }

    /**
     * 保存商城商品（POST /erp/mall/admin/product 新增；PUT /erp/mall/admin/product/{id} 编辑）。
     *
     * <p><b>数据源口径（修复记录）</b>：商品上架页列表读 {@code v_mall_product} 视图（源表 {@code erp_product}），
     * 编辑保存此前写已废弃的 {@code mall_product} 表（V9.0.0 Part 9 已标注废弃），
     * 造成「保存成功但列表不反映」。现编辑分支改为 UPDATE 权威表 {@code erp_product}
     * （{@link ErpProductWriteMapper}，只写弹窗实际提交的列）。</p>
     *
     * <p><b>字段映射（弹窗 ⇒ erp_product 物理列）</b>：
     * productName→product_name、imageUrl→image_url、salePrice→retail_price（视图 sale_price 的源列，
     * 非 standard_price）、marketPrice→wholesale_price（视图 market_price 与 wholesale_price 同源）、
     * categoryName→mall_category_name、status→mall_shelf_status（1=上架/0=下架，
     * <b>不是</b> erp_product.status 显示状态）、description→mall_description。
     * 不写：productId（编辑态禁用，erp_product.product_code 是商品主数据唯一编码，商城弹窗无权改写）、
     * stockQuantity（视图由 erp_stock 实时聚合，erp_product 无此列，硬写会造出假库存）。</p>
     *
     * <p><b>已知缺口（未闭环）</b>：新增分支仍写已废弃的 {@code mall_product} 表，保存后列表同样不反映。
     * 原因是 {@code erp_product} 是 ERP 商品主数据（product_code 唯一、含单位/分类/价格体系/
     * 审批与库存联动等必填业务约束），由商城弹窗仅凭「商品编码 + 名称 + 价格」INSERT 会造出半可用的
     * 主数据（单位/分类/等级价缺失，且可能撞 product_code 唯一约束）。正确做法是新增时从 ERP 商品档案
     * 选取既有商品再上架（详见 docs/Yh-Spec/手动整理对标开发文档/交易模块/商品上架开发文档.md）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(MallProduct product) {
        Long tenantId = getCurrentTenantId();
        if (product.getId() != null) {
            saveProductUpdate(product, tenantId);
        } else {
            // 未闭环缺口：新增仍落旧表 mall_product（列表读 v_mall_product 视图，故此分支保存后列表不反映）
            product.setId(null);
            product.setTenantId(tenantId);
            product.setCreateBy(StpUtil.getLoginIdAsLong());
            mallProductMapper.insert(product);
        }
    }

    /**
     * 编辑保存（PUT /product/{id}）：把弹窗字段 UPDATE 到权威表 {@code erp_product}。
     *
     * <p>写入前先用视图（{@link ErpProductMallMapper}，与列表同源）校验该商品存在且属于当前租户，
     * 再以 {@code id + tenant_id + deleted = 0} 为 WHERE 条件更新，避免跨租户改写。
     * 实体非 null 字段才参与 SET（MyBatis-Plus 默认 NOT_NULL 策略），故弹窗未提交的字段保持原值。</p>
     */
    private void saveProductUpdate(MallProduct req, Long tenantId) {
        Long id = req.getId();
        ErpProductMall existing = erpProductMallMapper.selectById(id);
        if (existing == null || !tenantId.equals(existing.getTenantId())) {
            throw BusinessException.notFound("商品不存在");
        }

        ErpProductWrite patch = new ErpProductWrite();
        patch.setProductName(req.getProductName());
        patch.setImageUrl(req.getImageUrl());
        patch.setRetailPrice(req.getSalePrice());
        // 前端「市场价」在视图里就是 erp_product.wholesale_price（不新增列、不改写为其它价格列）
        patch.setWholesalePrice(req.getMarketPrice());
        patch.setMallCategoryName(req.getCategoryName());
        patch.setMallDescription(req.getDescription());
        if (req.getStatus() != null) {
            patch.setMallShelfStatus(toMallShelfFlag(req.getStatus()));
        }
        // update_by 为 VARCHAR(50)：无会话时写 null（不写死字符串 "null"，也不因取会话失败而 500）
        Long operatorId = getCurrentUserId();
        patch.setUpdateBy(operatorId == null ? null : String.valueOf(operatorId));
        patch.setUpdateTime(LocalDateTime.now());

        LambdaUpdateWrapper<ErpProductWrite> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ErpProductWrite::getId, id);
        wrapper.eq(ErpProductWrite::getTenantId, tenantId);
        wrapper.eq(ErpProductWrite::getDeleted, 0);
        if (erpProductWriteMapper.update(patch, wrapper) == 0) {
            throw BusinessException.notFound("商品不存在");
        }
    }

    /**
     * 上架状态归一 → {@code erp_product.mall_shelf_status}（1=上架 / 0=下架）。
     * 取值口径与视图一致：{@code CASE WHEN p.mall_shelf_status = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END}
     * （见 V11.361.6 视图定义）；兼容前端 OFF_SHELF 口径。
     */
    private static int toMallShelfFlag(String status) {
        return "ON_SHELF".equals(normalizeShelfStatus(status)) ? 1 : 0;
    }

    /**
     * 删除商城商品（DELETE /product/{id}）。
     *
     * <p><b>遗留未改（越出本次修复范围）</b>：仍按已废弃的 {@code mall_product} 表定位与软删，
     * 既不反映到 {@code v_mall_product} 视图列表，也未同步权威表。若要闭环，应改为
     * 软删 {@code erp_product}（下架 + deleted）或仅下架 mall_shelf_status，需业务确认，故不在本次改动内。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Long tenantId = getCurrentTenantId();
        MallProduct product = mallProductMapper.selectById(id);
        if (product == null || !product.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("商品不存在");
        }
        mallProductMapper.deleteById(id);
    }

    // ==================== 订单管理（erp_sale_order, order_source=2） ====================

    /** 企业客户商城订单来源值 */
    private static final int ORDER_SOURCE_B2B_MALL = 2;

    /** 个人会员商城订单来源值 */
    private static final int ORDER_SOURCE_MEMBER_MALL = 3;

    /**
     * 商城状态 → erp_sale_order.status（权威口径：0草稿 / 1待审批 / 2已审批 / 3部分出库 /
     * 4完成 / 5交易完成 / 6已取消，以 {@code SaleOrder.status}、{@code ErpSaleOrderMall.status} 注释为准）。
     *
     * @return 映射结果；返回 {@code null} 表示该状态串不在白名单内（调用方需显式忽略，不得静默退化为 0）
     */
    private Integer toErpStatusOrNull(String mallStatus) {
        if (mallStatus == null) {
            return null;
        }
        switch (mallStatus.trim().toUpperCase()) {
            case "PENDING_PAYMENT": return 0;  // 待付款 → 草稿（未付款）
            case "PENDING_AUDIT":   return 1;  // 审核中 → 待审批
            case "PAID":            return 1;  // 已付款待审批 → 待审批
            case "APPROVED":        return 2;  // 已审批（待发货）
            case "SHIPPED":         return 3;  // 已发货 → 部分出库
            case "COMPLETED":       return 5;  // 交易完成
            case "CANCELLED":
            case "REJECTED":        return 6;  // 已取消（erp_sale_order 规范：6=已取消；CANCELLED/REJECTED 共用）
            default:                return null;
        }
    }

    /**
     * 商城状态 → erp 状态（兼容既有调用方；未识别状态退化为 0）
     *
     * @deprecated 查询场景请使用 {@link #toErpStatusOrNull}，避免未识别状态静默退化为 0
     */
    @Deprecated
    private int toErpStatus(String mallStatus) {
        Integer mapped = toErpStatusOrNull(mallStatus);
        return mapped != null ? mapped : 0;
    }

    /**
     * erp_sale_order.status → 商城状态串（与 {@link #toErpStatusOrNull} 同一张字典，用于 extInfo 缺失时兜底）
     *
     * <p>注意：erp 规范里 4=完成、5=交易完成 是两个不同阶段，但商城口径都归为「交易完成」；
     * 6=已取消 同时覆盖商城侧的「审核驳回」与「强制终止」，商城侧明细靠 extInfo.originalMallStatus 区分。</p>
     */
    private String erpStatusToMallStatus(Integer status) {
        switch (status != null ? status : 0) {
            case 0:  return "PENDING_PAYMENT";
            case 1:  return "PAID";
            case 2:  return "APPROVED";
            case 3:  return "SHIPPED";
            case 4:
            case 5:  return "COMPLETED";
            case 6:  return "CANCELLED";
            default: return "PENDING_PAYMENT";
        }
    }

    /** 从 extInfo 恢复原始商城状态 */
    private String getOriginalMallStatus(String extInfo) {
        if (extInfo != null && extInfo.contains("\"originalMallStatus\"")) {
            try {
                int idx = extInfo.indexOf("\"originalMallStatus\"");
                int valStart = extInfo.indexOf(':', idx) + 2;
                int valEnd = extInfo.indexOf('"', valStart);
                if (valStart > 1 && valEnd > valStart) {
                    return extInfo.substring(valStart, valEnd);
                }
            } catch (Exception e) {
                log.warn("解析 extInfo.originalMallStatus 失败", e);
            }
        }
        return null;
    }

    @Override
    public Page<ErpSaleOrderMall> pageOrders(Integer pageNum, Integer pageSize, String keyword, String orderStatus,
                                             String status, String orderNo, String startDate, String endDate,
                                             String consignee, String paymentMethod, Integer orderSource,
                                             String productName) {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<ErpSaleOrderMall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErpSaleOrderMall::getTenantId, tenantId);
        wrapper.eq(ErpSaleOrderMall::getOrderSource, ORDER_SOURCE_B2B_MALL);
        wrapper.eq(ErpSaleOrderMall::getDeleted, 0);

        // 状态：商城状态串（订单处理页）与数字 status（商城订单页 Tab1）取并集；为空即不过滤
        Set<Integer> statusSet = collectStatusSet(orderStatus, status);
        if (statusSet != null) {
            // 有筛选但一个都没映射上 → 显式置空结果集，不静默退化为全表
            wrapper.in(ErpSaleOrderMall::getStatus, statusSet.isEmpty() ? List.of(-1) : statusSet);
        }

        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(ErpSaleOrderMall::getOrderNo, orderNo.trim());
        }
        LocalDateTime start = orderDateFrom(startDate);
        LocalDateTime end = orderDateTo(endDate);
        wrapper.ge(start != null, ErpSaleOrderMall::getOrderDate, start);
        wrapper.lt(end != null, ErpSaleOrderMall::getOrderDate, end);
        if (consignee != null && !consignee.isEmpty()) {
            wrapper.like(ErpSaleOrderMall::getConsignee, consignee.trim());
        }
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            wrapper.eq(ErpSaleOrderMall::getPaymentMethod, paymentMethod.trim());
        }
        if (orderSource != null) {
            wrapper.eq(ErpSaleOrderMall::getOrderSource, orderSource);
        }
        // 商品名称/货号：erp_sale_order 无该列，按 erp_sale_order_item 反查单据 ID 集合
        if (productName != null && !productName.trim().isEmpty()) {
            List<Long> orderIds = findOrderIdsByProduct(productName.trim());
            wrapper.in(ErpSaleOrderMall::getId, orderIds.isEmpty() ? List.of(-1L) : orderIds);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ErpSaleOrderMall::getOrderNo, keyword)
                    .or().like(ErpSaleOrderMall::getCustomerName, keyword)
                    .or().like(ErpSaleOrderMall::getConsignee, keyword));
        }
        wrapper.orderByDesc(ErpSaleOrderMall::getCreateTime);
        return erpSaleOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 按商品名称/货号反查单据 ID 集合（erp_sale_order_item.product_name / product_code 模糊）
     *
     * <p>tenant_id 由 MyBatis-Plus 多租户拦截器自动注入。</p>
     */
    private List<Long> findOrderIdsByProduct(String productName) {
        List<ErpSaleOrderItemMall> items = erpSaleOrderItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOrderItemMall>()
                        .select(ErpSaleOrderItemMall::getOrderId)
                        .and(w -> w.like(ErpSaleOrderItemMall::getProductName, productName)
                                .or().like(ErpSaleOrderItemMall::getProductCode, productName)));
        return items.stream()
                .map(ErpSaleOrderItemMall::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public ErpSaleOrderMall getOrderDetail(Long id) {
        Long tenantId = getCurrentTenantId();
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(id);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        Long tenantId = getCurrentTenantId();
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(orderId);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }

        String mallStatus = getOriginalMallStatus(order.getExtInfo());
        if (mallStatus == null) mallStatus = "PAID";
        if (!"PAID".equals(mallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许审核通过");
        }
        order.setStatus(toErpStatus("APPROVED"));
        order.setExtInfo("{\"originalMallStatus\":\"APPROVED\",\"source\":\"b2b_mall\"}");
        erpSaleOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId, String reason) {
        Long tenantId = getCurrentTenantId();
        ErpSaleOrderMall order = erpSaleOrderMapper.selectById(orderId);
        if (order == null || !order.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("订单不存在");
        }

        String mallStatus = getOriginalMallStatus(order.getExtInfo());
        if (mallStatus == null) mallStatus = "PAID";
        if (!"PAID".equals(mallStatus) && !"PENDING_PAYMENT".equals(mallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许驳回");
        }
        order.setStatus(toErpStatus("REJECTED"));
        order.setRemark(reason);
        order.setExtInfo("{\"originalMallStatus\":\"REJECTED\",\"source\":\"b2b_mall\"}");
        erpSaleOrderMapper.updateById(order);
    }

    // ==================== 订单管理 · 扩展端点（统计 / 按明细 / 收款 / 发货 / 退款 / 终止 / 批量） ====================

    @Override
    public TradeAnalysisDTO.Summary getOrderStats() {
        // 复用既有交易分析聚合（MallTradeAnalysisMapper.selectSummary）：
        //   totalOrderCount 总订单数、totalGmv 订单总额合计、avgOrderAmount 客单价、
        //   refundOrderCount 退款单数、refundRate 退款率(%)
        // 口径：erp_sale_order、order_source IN (2,3)、deleted=0、排除已取消（status IN (5,6)）；
        // 退款口径：无独立退款金额列，按 payment_status=4（已退款）计数占比派生。
        TradeAnalysisDTO.Summary summary = mallTradeAnalysisMapper.selectSummary(null, null);
        if (summary == null) {
            summary = new TradeAnalysisDTO.Summary();
            summary.setTotalOrderCount(0L);
            summary.setTotalGmv(BigDecimal.ZERO);
            summary.setAvgOrderAmount(BigDecimal.ZERO);
            summary.setRefundOrderCount(0L);
            summary.setRefundRate(BigDecimal.ZERO);
        }
        return summary;
    }

    @Override
    public Page<MallOrderItemPageDTO> pageOrderItems(Integer pageNum, Integer pageSize, String keyword, String orderStatus,
                                                     String status, String orderNo, String startDate, String endDate,
                                                     String consignee, String paymentMethod, Integer orderSource,
                                                     String productName) {
        Page<MallOrderItemPageDTO> page = new Page<>(pageNum, pageSize);
        Set<Integer> statusSet = collectStatusSet(orderStatus, status);
        // tenant_id 由 MyBatis-Plus 多租户拦截器自动注入到 JOIN 的每张表
        IPage<MallOrderItemPageDTO> result = erpSaleOrderItemMapper.selectDetailPage(
                page,
                trimToNull(keyword),
                statusSet == null ? null : new ArrayList<>(statusSet.isEmpty() ? List.of(-1) : statusSet),
                trimToNull(orderNo),
                orderDateFrom(startDate),
                orderDateTo(endDate),
                trimToNull(consignee),
                trimToNull(paymentMethod),
                orderSource,
                trimToNull(productName));
        return (Page<MallOrderItemPageDTO>) result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId) {
        ErpSaleOrderMall order = requireOrder(orderId);
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        order.setReceivedAmount(total);
        order.setPaymentStatus(PAY_STATUS_PAID);
        // 收款后推进商城状态为 PAID（待审批），与买家端 MallOrderServiceImpl#payOrder 一致；
        // 已推进到 APPROVED/SHIPPED 的订单不回退状态
        String mallStatus = getOriginalMallStatus(order.getExtInfo());
        if ("CANCELLED".equals(mallStatus) || "REJECTED".equals(mallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许收款");
        }
        if (mallStatus == null || "PENDING_PAYMENT".equals(mallStatus)) {
            mallStatus = "PAID";
            if (order.getStatus() == null || order.getStatus() < toErpStatus("PAID")) {
                order.setStatus(toErpStatus("PAID"));
            }
        }
        order.setExtInfo(mergeExtInfo(order.getExtInfo(), mallStatus));
        order.setUpdateBy(getCurrentUserId());
        order.setUpdateTime(LocalDateTime.now());
        erpSaleOrderMapper.updateById(order);
        log.info("商城订单收款成功: orderNo={}, amount={}", order.getOrderNo(), total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, MallOrderShipRequest request) {
        ErpSaleOrderMall order = requireOrder(orderId);
        String mallStatus = resolveMallStatus(order);
        if ("CANCELLED".equals(mallStatus) || "REJECTED".equals(mallStatus) || "COMPLETED".equals(mallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许发货");
        }

        order.setDeliveryStatus(DELIVERY_STATUS_SHIPPED);
        // 订单状态：未推进到「已审批」的商城单同步推进为 3（部分出库/已发货）；已是 3/4 的不回退
        Integer status = order.getStatus();
        if (status == null || status < toErpStatus("SHIPPED")) {
            order.setStatus(toErpStatus("SHIPPED"));
        }
        if (request != null) {
            // 物流公司/运单号落实体已有列（logistics_company / waybill_no，ErpSaleOrderMall 已映射）
            order.setLogisticsCompany(trimToNull(request.getLogisticsCompany()));
            order.setTrackingNo(trimToNull(request.getTrackingNo()));
            if (request.getRemark() != null && !request.getRemark().isEmpty()) {
                order.setRemark(request.getRemark());
            }
        }
        order.setExtInfo(mergeExtInfo(order.getExtInfo(), "SHIPPED"));
        order.setUpdateBy(getCurrentUserId());
        order.setUpdateTime(LocalDateTime.now());
        erpSaleOrderMapper.updateById(order);
        log.info("商城订单发货成功: orderNo={}, logisticsCompany={}, trackingNo={}",
                order.getOrderNo(),
                request != null ? request.getLogisticsCompany() : null,
                request != null ? request.getTrackingNo() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, MallOrderRefundRequest request) {
        ErpSaleOrderMall order = requireOrder(orderId);
        if (order.getPaymentStatus() == null || order.getPaymentStatus() != PAY_STATUS_PAID) {
            throw BusinessException.badRequest("当前订单未支付，不允许退款");
        }

        order.setPaymentStatus(PAY_STATUS_REFUNDED);
        // 无独立退款金额列：退款金额/原因/备注统一落单头备注，退款率按 payment_status=4 计数派生
        if (request != null) {
            StringBuilder remark = new StringBuilder();
            if (request.getAmount() != null) {
                remark.append("退款金额：").append(request.getAmount());
            }
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                if (remark.length() > 0) remark.append("；");
                remark.append("退款原因：").append(request.getReason());
            }
            if (request.getRemark() != null && !request.getRemark().isEmpty()) {
                if (remark.length() > 0) remark.append("；");
                remark.append(request.getRemark());
            }
            if (remark.length() > 0) {
                order.setRemark(remark.toString());
            }
        }
        order.setUpdateBy(getCurrentUserId());
        order.setUpdateTime(LocalDateTime.now());
        erpSaleOrderMapper.updateById(order);
        log.info("商城订单退款成功: orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateOrder(Long orderId, String reason) {
        ErpSaleOrderMall order = requireOrder(orderId);
        String mallStatus = resolveMallStatus(order);
        if ("COMPLETED".equals(mallStatus) || "CANCELLED".equals(mallStatus)) {
            throw BusinessException.badRequest("当前订单状态不允许强制终止");
        }

        order.setStatus(toErpStatus("CANCELLED"));
        order.setExtInfo(mergeExtInfo(order.getExtInfo(), "CANCELLED"));
        if (reason != null && !reason.isEmpty()) {
            order.setRemark(reason);
        }
        order.setUpdateBy(getCurrentUserId());
        order.setUpdateTime(LocalDateTime.now());
        erpSaleOrderMapper.updateById(order);
        log.info("商城订单强制终止: orderNo={}, reason={}", order.getOrderNo(), reason);
    }

    @Override
    public int batchApproveOrders(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (Long id : ids.subList(0, Math.min(ids.size(), MAX_BATCH_SIZE))) {
            if (id == null) continue;
            try {
                // 逐条独立事务：单条状态校验失败不回滚已成功条目
                Boolean ok = batchTxTemplate.execute(status -> {
                    approveOrder(id);
                    return Boolean.TRUE;
                });
                if (Boolean.TRUE.equals(ok)) {
                    success++;
                }
            } catch (Exception e) {
                log.warn("批量审核跳过订单: id={}, 原因={}", id, e.getMessage());
            }
        }
        log.info("批量审核完成: 请求={}, 成功={}", ids.size(), success);
        return success;
    }

    @Override
    public int batchShipOrders(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (Long id : ids.subList(0, Math.min(ids.size(), MAX_BATCH_SIZE))) {
            if (id == null) continue;
            try {
                // 逐条独立事务：单条状态校验失败不回滚已成功条目
                Boolean ok = batchTxTemplate.execute(status -> {
                    shipOrder(id, null);
                    return Boolean.TRUE;
                });
                if (Boolean.TRUE.equals(ok)) {
                    success++;
                }
            } catch (Exception e) {
                log.warn("批量发货跳过订单: id={}, 原因={}", id, e.getMessage());
            }
        }
        log.info("批量发货完成: 请求={}, 成功={}", ids.size(), success);
        return success;
    }

    /**
     * 归一化两页「单据状态」查询参数 → erp_sale_order.status 整数集合
     *
     * <ul>
     *   <li>{@code orderStatus}：商城状态串，逗号分隔多值（订单处理页提交 PENDING_AUDIT,PENDING_PAYMENT）；
     *       逐值走 {@link #toErpStatusOrNull} 映射，**无法识别的值显式忽略并记日志**（不再静默退化为 0）</li>
     *   <li>{@code status}：erp_sale_order.status 数字，单值或逗号分隔多值（商城订单页 Tab1 提交数字）；
     *       非数字取值同样忽略并记日志</li>
     * </ul>
     *
     * @return {@code null} 表示两个参数都未传（不过滤）；非 null 集合可能为空（表示传了但全无法识别，调用方需返回空结果）
     */
    private Set<Integer> collectStatusSet(String orderStatus, String status) {
        boolean hasOrderStatus = orderStatus != null && !orderStatus.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        if (!hasOrderStatus && !hasStatus) {
            return null;
        }
        Set<Integer> result = new LinkedHashSet<>();
        if (hasOrderStatus) {
            for (String part : orderStatus.split(",")) {
                String v = part.trim();
                if (v.isEmpty()) continue;
                Integer mapped = toErpStatusOrNull(v);
                if (mapped != null) {
                    result.add(mapped);
                } else {
                    log.warn("商城订单查询：忽略无法识别的商城状态值 orderStatus={}", v);
                }
            }
        }
        if (hasStatus) {
            for (String part : status.split(",")) {
                String v = part.trim();
                if (v.isEmpty()) continue;
                try {
                    result.add(Integer.valueOf(v));
                } catch (NumberFormatException e) {
                    log.warn("商城订单查询：忽略无法识别的数字状态值 status={}", v);
                }
            }
        }
        return result;
    }

    /**
     * 恢复订单的商城状态：优先 extInfo.originalMallStatus，缺失时按 erp_sale_order.status 推断
     * （字典与 {@link #toErpStatusOrNull} 互为逆映射）
     */
    private String resolveMallStatus(ErpSaleOrderMall order) {
        String mallStatus = getOriginalMallStatus(order.getExtInfo());
        return mallStatus != null ? mallStatus : erpStatusToMallStatus(order.getStatus());
    }

    /**
     * 合并扩展信息 JSON：保留原有字段，仅覆盖 originalMallStatus。
     * 不做全量 JSON 解析，避免引入额外依赖（与既有 getOriginalMallStatus 的轻量解析风格一致）。
     */
    private String mergeExtInfo(String extInfo, String mallStatus) {
        String status = (mallStatus != null && !mallStatus.isEmpty()) ? mallStatus : "PENDING_PAYMENT";
        if (extInfo == null || extInfo.isEmpty() || "null".equals(extInfo)) {
            return "{\"originalMallStatus\":\"" + status + "\",\"source\":\"b2b_mall\"}";
        }
        if (extInfo.contains("\"originalMallStatus\"")) {
            return extInfo.replaceFirst("\"originalMallStatus\"\\s*:\\s*\"[^\"]*\"",
                    "\"originalMallStatus\":\"" + status + "\"");
        }
        int end = extInfo.lastIndexOf('}');
        if (end < 0) {
            return "{\"originalMallStatus\":\"" + status + "\",\"source\":\"b2b_mall\"}";
        }
        return extInfo.substring(0, end) + ",\"originalMallStatus\":\"" + status + "\"}";
    }

    /** 去掉首尾空白，空白串归一为 null */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ==================== 页面模板 ====================

    @Override
    public List<ShopTemplate> listTemplates() {
        LambdaQueryWrapper<ShopTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopTemplate::getStatus, 1);
        // 「我的模板」= 非库条目。用 ne(1) 而非 eq(0)：V11.366.0 之前的历史模板行
        // is_library 为 NULL，eq(0) 会把它们一并排除（回归），ne(1) 则保持可见。
        wrapper.ne(ShopTemplate::getIsLibrary, 1);
        wrapper.orderByAsc(ShopTemplate::getId);
        return shopTemplateMapper.selectList(wrapper);
    }

    @Override
    public List<ShopTemplate> listTemplateLibrary() {
        LambdaQueryWrapper<ShopTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopTemplate::getIsLibrary, 1);
        wrapper.eq(ShopTemplate::getStatus, 1);
        wrapper.orderByAsc(ShopTemplate::getId);
        return shopTemplateMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopTemplate createTemplate(ShopTemplate template) {
        if (template == null) {
            throw new BusinessException("模板参数不能为空");
        }
        String name = trimToNull(template.getTemplateName());
        if (name == null) {
            throw new BusinessException("模板名称不能为空");
        }
        template.setId(null);
        template.setTemplateName(name);
        if (trimToNull(template.getTemplateCode()) == null) {
            // 表上 template_code 有唯一约束（shop_template_template_code_key），
            // 自动生成时带毫秒时间戳降低并发碰撞概率；调用方也可显式传入。
            template.setTemplateCode("DECO_" + System.currentTimeMillis());
        }
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        // 新增的必然是「我的模板」，不允许调用方借此写库条目
        template.setIsLibrary(0);
        template.setCreateBy(getCurrentUserId());
        shopTemplateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopTemplate referenceLibraryTemplate(Long libraryTemplateId) {
        ShopTemplate source = libraryTemplateId == null ? null : shopTemplateMapper.selectById(libraryTemplateId);
        if (source == null || !Integer.valueOf(1).equals(source.getIsLibrary())) {
            throw BusinessException.notFound("行业库模板不存在");
        }
        ShopTemplate copy = new ShopTemplate();
        copy.setTemplateName(source.getTemplateName());
        // 编码必须唯一：库编码 + 引用时间戳，避免与库条目/既有引用冲突
        copy.setTemplateCode(source.getTemplateCode() + "_REF_" + System.currentTimeMillis());
        copy.setThumbnail(source.getThumbnail());
        copy.setDescription(source.getDescription());
        // ⚠️ 库条目的 config_json 为空（行业模板内容未实测）→ 复制出的模板内容同样为空。
        // 这是如实留缺口：不臆造模板结构，等待排版布局编辑器保存时再写入。
        copy.setConfigJson(source.getConfigJson());
        copy.setIsDefault(0);
        copy.setStatus(1);
        copy.setIsLibrary(0);
        copy.setIndustryCode(source.getIndustryCode());
        copy.setIndustryName(source.getIndustryName());
        copy.setCreateBy(getCurrentUserId());
        shopTemplateMapper.insert(copy);
        return copy;
    }

    // ==================== 装修配置 ====================

    @Override
    public List<ShopDecoration> listDecorations(String scope) {
        LambdaQueryWrapper<ShopDecoration> wrapper = new LambdaQueryWrapper<>();
        // tenant_id 由租户拦截器自动注入；此处显式带上以兼容无会话的调用方（与 listBanners 同风格）
        wrapper.eq(ShopDecoration::getTenantId, getCurrentTenantId());
        String s = trimToNull(scope);
        if (s != null) {
            wrapper.eq(ShopDecoration::getScope, s);
        }
        wrapper.orderByDesc(ShopDecoration::getId);
        return shopDecorationMapper.selectList(wrapper);
    }

    @Override
    public ShopDecoration getDecoration(Long id) {
        return requireDecoration(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopDecoration createDecoration(ShopDecoration decoration) {
        if (decoration == null) {
            throw new BusinessException("装修配置参数不能为空");
        }
        String name = trimToNull(decoration.getName());
        if (name == null) {
            throw new BusinessException("装修名称不能为空");
        }
        decoration.setId(null);
        decoration.setName(name);
        if (trimToNull(decoration.getScope()) == null) {
            decoration.setScope(ShopDecoration.SCOPE_HOME);
        }
        if (decoration.getStatus() == null) {
            decoration.setStatus(1);
        }
        decoration.setTenantId(getCurrentTenantId());
        decoration.setCreateBy(getCurrentUserId());
        shopDecorationMapper.insert(decoration);
        return decoration;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopDecoration updateDecoration(Long id, ShopDecoration decoration) {
        ShopDecoration existing = requireDecoration(id);
        if (decoration == null) {
            throw new BusinessException("装修配置参数不能为空");
        }
        // 部分更新：仅覆盖请求体中非 null 字段（与 updateConfig 的 NOT_NULL 策略同口径）
        ShopDecoration patch = new ShopDecoration();
        patch.setId(existing.getId());
        patch.setName(trimToNull(decoration.getName()));
        patch.setScope(trimToNull(decoration.getScope()));
        patch.setTemplateId(decoration.getTemplateId());
        patch.setConfigJson(decoration.getConfigJson());
        patch.setStatus(decoration.getStatus());
        patch.setTenantId(existing.getTenantId());
        patch.setUpdateBy(getCurrentUserId());
        patch.setUpdateTime(LocalDateTime.now());
        shopDecorationMapper.updateById(patch);
        return requireDecoration(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDecoration(Long id) {
        ShopDecoration existing = requireDecoration(id);
        shopDecorationMapper.deleteById(existing.getId());
        // 一并逻辑删除商品关联，避免留下孤儿关联行
        shopDecorationProductMapper.delete(
                new LambdaQueryWrapper<ShopDecorationProduct>()
                        .eq(ShopDecorationProduct::getTenantId, existing.getTenantId())
                        .eq(ShopDecorationProduct::getDecorationId, existing.getId()));
    }

    @Override
    public List<Long> listDecorationProducts(Long decorationId) {
        ShopDecoration existing = requireDecoration(decorationId);
        List<ShopDecorationProduct> rows = shopDecorationProductMapper.selectList(
                new LambdaQueryWrapper<ShopDecorationProduct>()
                        .eq(ShopDecorationProduct::getTenantId, existing.getTenantId())
                        .eq(ShopDecorationProduct::getDecorationId, existing.getId())
                        .orderByAsc(ShopDecorationProduct::getId));
        List<Long> ids = new ArrayList<>(rows.size());
        for (ShopDecorationProduct row : rows) {
            if (row.getProductId() != null) {
                ids.add(row.getProductId());
            }
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> replaceDecorationProducts(Long decorationId, List<Long> productIds) {
        ShopDecoration existing = requireDecoration(decorationId);
        // 全量替换：先逻辑删旧关联，再插新关联（同一事务，失败则整体回滚）
        shopDecorationProductMapper.delete(
                new LambdaQueryWrapper<ShopDecorationProduct>()
                        .eq(ShopDecorationProduct::getTenantId, existing.getTenantId())
                        .eq(ShopDecorationProduct::getDecorationId, existing.getId()));

        // 去重 + 保序（LinkedHashSet 与仓库既有风格一致），并丢弃 null
        Set<Long> distinct = new LinkedHashSet<>();
        if (productIds != null) {
            for (Long pid : productIds) {
                if (pid != null) {
                    distinct.add(pid);
                }
            }
        }
        Long userId = getCurrentUserId();
        for (Long pid : distinct) {
            ShopDecorationProduct row = new ShopDecorationProduct();
            row.setTenantId(existing.getTenantId());
            row.setDecorationId(existing.getId());
            row.setProductId(pid);
            row.setCreateBy(userId);
            shopDecorationProductMapper.insert(row);
        }
        return new ArrayList<>(distinct);
    }

    /** 读取装修配置并做租户归属校验 */
    private ShopDecoration requireDecoration(Long id) {
        Long tenantId = getCurrentTenantId();
        ShopDecoration decoration = id == null ? null : shopDecorationMapper.selectById(id);
        if (decoration == null || (tenantId != null && !tenantId.equals(decoration.getTenantId()))) {
            throw BusinessException.notFound("装修配置不存在");
        }
        return decoration;
    }
}

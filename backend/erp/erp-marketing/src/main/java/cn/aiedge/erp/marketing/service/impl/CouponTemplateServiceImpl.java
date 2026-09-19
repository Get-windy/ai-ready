package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.marketing.dto.CouponRecordRow;
import cn.aiedge.erp.marketing.entity.CouponCustomer;
import cn.aiedge.erp.marketing.entity.CouponTemplate;
import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import cn.aiedge.erp.marketing.mapper.CouponCustomerMapper;
import cn.aiedge.erp.marketing.mapper.CouponTemplateMapper;
import cn.aiedge.erp.marketing.mapper.LoyaltyCouponMapper;
import cn.aiedge.erp.marketing.mapper.MarketingQueryMapper;
import cn.aiedge.erp.marketing.service.CouponTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponTemplateService {

    private final CouponCustomerMapper couponCustomerMapper;
    private final LoyaltyCouponMapper loyaltyCouponMapper;
    private final MarketingQueryMapper marketingQueryMapper;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    private int intOf(Integer v) {
        return v == null ? 0 : v;
    }

    @Override
    public IPage<CouponTemplate> pageWithCounts(String couponName, String status, String customerScope,
                                                Integer openReceive, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CouponTemplate> w = new LambdaQueryWrapper<>();
        if (couponName != null && !couponName.isEmpty()) w.like(CouponTemplate::getCouponName, couponName);
        if (status != null && !status.isEmpty()) w.eq(CouponTemplate::getStatus, status);
        if (customerScope != null && !customerScope.isEmpty()) w.eq(CouponTemplate::getCustomerScope, customerScope);
        if (openReceive != null) w.eq(CouponTemplate::getOpenReceive, openReceive);
        w.orderByDesc(CouponTemplate::getCreateTime).orderByDesc(CouponTemplate::getId);
        IPage<CouponTemplate> page = page(new Page<>(pageNum, pageSize), w);
        page.getRecords().forEach(t -> t.setRemainingCount(
                intOf(t.getTotalCount()) - intOf(t.getReceivedCount()) - intOf(t.getUsedCount())));
        return page;
    }

    @Override
    public IPage<CouponRecordRow> pageRecords(Long templateId, String status, String billNo, String couponName,
                                              Long partnerId, Integer pageNum, Integer pageSize) {
        IPage<CouponRecordRow> page = marketingQueryMapper.selectCouponRecordPage(
                new Page<>(pageNum, pageSize), tenantId(), templateId, status, billNo, couponName, partnerId);
        page.getRecords().forEach(r -> r.setReceiveStatus(mapReceiveStatus(r.getRawStatus())));
        return page;
    }

    /** 领用状态：由 erp_loyalty_coupon.status 映射（对标「领用状态」列） */
    private String mapReceiveStatus(String status) {
        if (status == null) return "已领取";
        return switch (status) {
            case "USED" -> "已使用";
            case "EXPIRED" -> "已过期";
            case "CANCELLED" -> "已作废";
            default -> "已领取";
        };
    }

    @Override
    public List<CouponCustomer> listCustomers(Long templateId) {
        return couponCustomerMapper.selectList(new LambdaQueryWrapper<CouponCustomer>()
                .eq(CouponCustomer::getTemplateId, templateId)
                .orderByAsc(CouponCustomer::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCustomers(Long templateId, List<CouponCustomer> customers) {
        couponCustomerMapper.delete(new LambdaQueryWrapper<CouponCustomer>()
                .eq(CouponCustomer::getTemplateId, templateId));
        if (customers == null || customers.isEmpty()) return;
        Long tid = tenantId();
        for (CouponCustomer c : customers) {
            c.setId(null);
            c.setTenantId(tid);
            c.setTemplateId(templateId);
            c.setCreateTime(LocalDateTime.now());
            couponCustomerMapper.insert(c);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int issue(Long templateId, List<Long> partnerIds, Integer quantityPerPartner, String sourceBillNo) {
        CouponTemplate tpl = getById(templateId);
        if (tpl == null) throw new IllegalArgumentException("优惠券不存在");
        if ("VOID".equals(tpl.getStatus())) throw new IllegalArgumentException("该优惠券已作废，不能发放");
        if (partnerIds == null || partnerIds.isEmpty()) throw new IllegalArgumentException("请先选择要发放的客户");

        int per = quantityPerPartner == null || quantityPerPartner < 1 ? 1 : quantityPerPartner;
        int need = per * partnerIds.size();
        int remain = intOf(tpl.getTotalCount()) - intOf(tpl.getReceivedCount()) - intOf(tpl.getUsedCount());
        if (need > remain) {
            throw new IllegalArgumentException("可发放数量不足：未领取 " + remain + " 张，本次需发放 " + need + " 张");
        }

        Long tid = tenantId();
        LocalDateTime now = LocalDateTime.now();
        String batch = "FV" + System.currentTimeMillis();
        int seq = 0;
        for (Long partnerId : partnerIds) {
            for (int i = 0; i < per; i++) {
                seq++;
                LoyaltyCoupon coupon = new LoyaltyCoupon()
                        .setTenantId(tid)
                        .setTemplateId(templateId)
                        .setPartnerId(partnerId)
                        .setCode(batch + String.format("%03d", seq))
                        .setStatus("UNUSED")
                        .setFaceValue(tpl.getFaceValue() != null ? tpl.getFaceValue() : BigDecimal.ZERO)
                        .setBalance(tpl.getFaceValue() != null ? tpl.getFaceValue() : BigDecimal.ZERO)
                        .setExpirationDate(tpl.getEndTime())
                        .setReceiveTime(now)
                        .setSourceBillNo(sourceBillNo)
                        .setCreateTime(now)
                        .setUpdateTime(now)
                        .setDeleted(0);
                loyaltyCouponMapper.insert(coupon);
            }
        }

        update(new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getReceivedCount, intOf(tpl.getReceivedCount()) + need)
                .set(CouponTemplate::getUpdateTime, now));
        log.info("[优惠券] 模板 {} 发放 {} 张给 {} 个客户（批次 {}）", templateId, need, partnerIds.size(), batch);
        return need;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidTemplate(Long templateId) {
        CouponTemplate tpl = getById(templateId);
        if (tpl == null) throw new IllegalArgumentException("优惠券不存在");
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getStatus, "VOID")
                .set(CouponTemplate::getUpdateTime, now));
        List<LoyaltyCoupon> unused = loyaltyCouponMapper.selectList(new LambdaQueryWrapper<LoyaltyCoupon>()
                .eq(LoyaltyCoupon::getTemplateId, templateId)
                .eq(LoyaltyCoupon::getStatus, "UNUSED"));
        List<Long> ids = new ArrayList<>();
        unused.forEach(c -> ids.add(c.getId()));
        if (!ids.isEmpty()) {
            loyaltyCouponMapper.update(null, new LambdaUpdateWrapper<LoyaltyCoupon>()
                    .in(LoyaltyCoupon::getId, ids)
                    .set(LoyaltyCoupon::getStatus, "CANCELLED")
                    .set(LoyaltyCoupon::getUpdateTime, now));
        }
        log.info("[优惠券] 模板 {} 已作废，同步作废未使用券 {} 张", templateId, ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidCoupon(Long couponId) {
        LoyaltyCoupon coupon = loyaltyCouponMapper.selectById(couponId);
        if (coupon == null) throw new IllegalArgumentException("优惠券不存在");
        if (!"UNUSED".equals(coupon.getStatus())) {
            throw new IllegalArgumentException("只有「已领取未使用」的券可以作废，当前状态：" + coupon.getStatus());
        }
        loyaltyCouponMapper.update(null, new LambdaUpdateWrapper<LoyaltyCoupon>()
                .eq(LoyaltyCoupon::getId, couponId)
                .eq(LoyaltyCoupon::getStatus, "UNUSED")
                .set(LoyaltyCoupon::getStatus, "CANCELLED")
                .set(LoyaltyCoupon::getUpdateTime, LocalDateTime.now()));
        log.info("[优惠券] 券 {} 已作废", coupon.getCode());
    }

    @Override
    public Map<String, Object> stat(Long templateId) {
        CouponTemplate tpl = getById(templateId);
        Map<String, Object> map = new LinkedHashMap<>();
        if (tpl == null) return map;
        map.put("totalCount", intOf(tpl.getTotalCount()));
        map.put("receivedCount", intOf(tpl.getReceivedCount()));
        map.put("usedCount", intOf(tpl.getUsedCount()));
        map.put("remainingCount", intOf(tpl.getTotalCount()) - intOf(tpl.getReceivedCount()) - intOf(tpl.getUsedCount()));
        return map;
    }
}

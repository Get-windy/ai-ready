package cn.aiedge.erp.sale.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.entity.FreightRule;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.entity.SaleOrderLogistics;
import cn.aiedge.erp.sale.entity.ShipmentNotify;
import cn.aiedge.erp.sale.mapper.FreightRuleMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderLogisticsMapper;
import cn.aiedge.erp.sale.mapper.SaleOrderMapper;
import cn.aiedge.erp.sale.mapper.ShipmentNotifyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 销售物流域服务（包裹 / 运费 / 取号 / 发货通知）
 *
 * <p>落地《物流发货-业界做法调研.md》的 P0/P1/P2：</p>
 * <ul>
 *   <li><b>P0</b> 物流信息统一落 1:N 子表 {@code erp_sale_order_logistics}，主表扁平列只作列表展示快照；承运商引用档案；</li>
 *   <li><b>P1</b> 子表按「包裹」语义使用：一单多包（一条记录 = 一个包裹）；</li>
 *   <li><b>P2-4</b> 承运商运费规则（首重/续重）→ 自动试算落 {@code shipping_fee} → 与承运商账单金额对账出差异；</li>
 *   <li><b>P2-5</b> 电子面单取号：配置化承运商接口（无配置时明确报错，前端手输兜底，不做假号）；</li>
 *   <li><b>P2-6</b> 发货通知 ASN：发货后幂等生成台账，按配置回调地址推送，可重试。</li>
 * </ul>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleLogisticsService {

    /** 配置：电子面单取号（外部服务密钥一律落 core-api application.yml / 环境变量，见 README §7） */
    @Value("${erp.logistics.waybill.enabled:false}")
    private boolean waybillEnabled;
    @Value("${erp.logistics.waybill.url:}")
    private String waybillUrl;
    @Value("${erp.logistics.waybill.token:}")
    private String waybillToken;
    @Value("${erp.logistics.waybill.response-path:data.waybillNo}")
    private String waybillResponsePath;
    /** 配置：发货通知（ASN）回调地址；为空则只落台账不发 */
    @Value("${erp.shipment.asn.callback-url:}")
    private String asnCallbackUrl;
    @Value("${erp.shipment.asn.token:}")
    private String asnToken;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 包裹状态 */
    private static final int PKG_PENDING = 0, PKG_SHIPPED = 1;

    private final SaleOrderLogisticsMapper logisticsMapper;
    private final SaleOrderMapper orderMapper;
    private final FreightRuleMapper freightRuleMapper;
    private final ShipmentNotifyMapper notifyMapper;

    // ═══════════════════════════════════════════
    // P0/P1 包裹（一单多包）
    // ═══════════════════════════════════════════

    /** 订单包裹列表（按 id 升序；空 = 尚未维护物流） */
    public List<SaleOrderLogistics> listPackages(Long orderId) {
        return logisticsMapper.selectList(new LambdaQueryWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getOrderId, orderId)
                .orderByAsc(SaleOrderLogistics::getId));
    }

    /** 新增/修改包裹；自动补包裹号、校验运单号唯一、按规则试算运费，并同步主表快照 */
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderLogistics savePackage(Long orderId, SaleOrderLogistics pkg) {
        SaleOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在: " + orderId);
        }
        pkg.setOrderId(orderId);
        if (pkg.getPackageStatus() == null) {
            pkg.setPackageStatus(PKG_PENDING);
        }
        // 运单号唯一（防重复录号/重复取号）
        String waybillNo = trimToNull(pkg.getWaybillNo());
        if (waybillNo != null) {
            Long dup = logisticsMapper.selectCount(new LambdaQueryWrapper<SaleOrderLogistics>()
                    .eq(SaleOrderLogistics::getWaybillNo, waybillNo)
                    .ne(pkg.getId() != null, SaleOrderLogistics::getId, pkg.getId()));
            if (dup != null && dup > 0) {
                throw BusinessException.badRequest("运单号已存在，请勿重复录入: " + waybillNo);
            }
        }
        // 运费：未填且有重量+承运商 → 按规则试算（算不出就留空，不阻断）
        if (pkg.getShippingFee() == null && pkg.getPackageWeight() != null) {
            try {
                Map<String, Object> calc = calcFreight(pkg.getLogisticsCompanyId(), null, pkg.getPackageWeight());
                if (Boolean.TRUE.equals(calc.get("matched"))) {
                    pkg.setShippingFee((BigDecimal) calc.get("freight"));
                }
            } catch (Exception e) {
                log.debug("包裹运费试算跳过: {}", e.getMessage());
            }
        }
        if (pkg.getId() == null) {
            if (!StringUtils.hasText(pkg.getPackageNo())) {
                pkg.setPackageNo("P" + (listPackages(orderId).size() + 1));
            }
            logisticsMapper.insert(pkg);
        } else {
            SaleOrderLogistics exist = logisticsMapper.selectById(pkg.getId());
            if (exist == null || !Objects.equals(exist.getOrderId(), orderId)) {
                throw BusinessException.notFound("包裹不存在: " + pkg.getId());
            }
            logisticsMapper.updateById(pkg);
        }
        syncOrderSnapshot(order);
        return logisticsMapper.selectById(pkg.getId());
    }

    /** 删除包裹；最后一个包裹不允许删（物流信息至少保留一条，避免主表快照指向空） */
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long orderId, Long packageId) {
        SaleOrderLogistics pkg = logisticsMapper.selectById(packageId);
        if (pkg == null || !Objects.equals(pkg.getOrderId(), orderId)) {
            throw BusinessException.notFound("包裹不存在: " + packageId);
        }
        if (listPackages(orderId).size() <= 1) {
            throw BusinessException.badRequest("至少保留一个包裹（可清空其内容，但不能删除）");
        }
        logisticsMapper.deleteById(packageId);
        syncOrderSnapshot(orderMapper.selectById(orderId));
    }

    /**
     * 主表扁平列 = 子表现状快照（拣货/发货 37 列读的就是主表这几列）。
     * 规则：默认包裹（id 最小）的 承运商/运单号/配送方式/司机 回写主表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncOrderSnapshot(SaleOrder order) {
        if (order == null) {
            return;
        }
        SaleOrderLogistics first = listPackages(order.getId()).stream().findFirst().orElse(null);
        if (first == null) {
            return;
        }
        orderMapper.update(null, new LambdaUpdateWrapper<SaleOrder>()
                .eq(SaleOrder::getId, order.getId())
                .set(SaleOrder::getLogisticsCompany, first.getLogisticsCompany())
                .set(SaleOrder::getWaybillNo, first.getWaybillNo())
                .set(first.getDeliveryMethod() != null, SaleOrder::getDeliveryMethod, first.getDeliveryMethod())
                .set(first.getDriverName() != null, SaleOrder::getDriverName, first.getDriverName()));
    }

    /** 发货：把该订单所有包裹置「已发货」（P1 部分发货的最小口径：整单发货时包裹全发） */
    @Transactional(rollbackFor = Exception.class)
    public int markPackagesShipped(Long orderId) {
        List<SaleOrderLogistics> pkgs = listPackages(orderId);
        for (SaleOrderLogistics pkg : pkgs) {
            if (pkg.getPackageStatus() == null || pkg.getPackageStatus() == PKG_PENDING) {
                logisticsMapper.update(null, new LambdaUpdateWrapper<SaleOrderLogistics>()
                        .eq(SaleOrderLogistics::getId, pkg.getId())
                        .set(SaleOrderLogistics::getPackageStatus, PKG_SHIPPED));
            }
        }
        return pkgs.size();
    }

    // ═══════════════════════════════════════════
    // P2-4 运费规则 / 试算 / 对账
    // ═══════════════════════════════════════════

    public List<FreightRule> listRules(Long carrierId) {
        return freightRuleMapper.selectList(new LambdaQueryWrapper<FreightRule>()
                .eq(carrierId != null, FreightRule::getCarrierId, carrierId)
                .orderByDesc(FreightRule::getPriority)
                .orderByAsc(FreightRule::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public FreightRule saveRule(FreightRule rule) {
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getId() == null) {
            freightRuleMapper.insert(rule);
        } else {
            freightRuleMapper.updateById(rule);
        }
        return freightRuleMapper.selectById(rule.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        freightRuleMapper.deleteById(id);
    }

    /**
     * 运费试算：首重价 + 基础费 + 续重（按步长向上取整）
     *
     * <p>规则匹配优先级：承运商+区域 &gt; 承运商 &gt; 区域 &gt; 通用；同组按 priority 降序，
     * 重量落在 [minWeight, maxWeight) 区间内即命中（maxWeight 为空视为无穷）。</p>
     */
    public Map<String, Object> calcFreight(Long carrierId, String area, BigDecimal weight) {
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal w = weight == null ? BigDecimal.ZERO : weight;
        List<FreightRule> rules = listRules(null).stream()
                .filter(r -> r.getEnabled() == null || r.getEnabled() == 1)
                .filter(r -> r.getCarrierId() == null || Objects.equals(r.getCarrierId(), carrierId))
                .filter(r -> !StringUtils.hasText(r.getArea()) || Objects.equals(r.getArea(), area))
                .filter(r -> inRange(r, w))
                .sorted(Comparator
                        .comparingInt((FreightRule r) -> specificity(r, carrierId, area)).reversed()
                        .thenComparing(r -> r.getPriority() == null ? 0 : -r.getPriority()))
                .toList();
        if (rules.isEmpty()) {
            result.put("matched", false);
            result.put("freight", BigDecimal.ZERO);
            result.put("message", "未命中任何运费规则（可按承运商/区域新增规则，或手工填写运费）");
            return result;
        }
        FreightRule rule = rules.get(0);
        BigDecimal freight = nz(rule.getFirstPrice()).add(nz(rule.getBaseFee()));
        BigDecimal firstWeight = nz(rule.getFirstWeight());
        BigDecimal addStep = nz(rule.getAddStep());
        BigDecimal addPrice = nz(rule.getAddPrice());
        if (w.compareTo(firstWeight) > 0 && addStep.signum() > 0) {
            BigDecimal extra = w.subtract(firstWeight);
            BigDecimal steps = extra.divide(addStep, 0, RoundingMode.CEILING);
            freight = freight.add(steps.multiply(addPrice));
        }
        result.put("matched", true);
        result.put("ruleId", rule.getId());
        result.put("ruleName", String.format("%s/%s", rule.getCarrierName() == null ? "通用" : rule.getCarrierName(),
                StringUtils.hasText(rule.getArea()) ? rule.getArea() : "通用"));
        result.put("freight", freight.setScale(2, RoundingMode.HALF_UP));
        result.put("weight", w);
        return result;
    }

    /** 规则特异性得分：承运商+区域=2 / 单条件=1 / 通用=0 */
    private int specificity(FreightRule rule, Long carrierId, String area) {
        int score = 0;
        if (rule.getCarrierId() != null && Objects.equals(rule.getCarrierId(), carrierId)) {
            score++;
        }
        if (StringUtils.hasText(rule.getArea()) && Objects.equals(rule.getArea(), area)) {
            score++;
        }
        return score;
    }

    private boolean inRange(FreightRule rule, BigDecimal weight) {
        if (rule.getMinWeight() != null && weight.compareTo(rule.getMinWeight()) < 0) {
            return false;
        }
        return rule.getMaxWeight() == null || weight.compareTo(rule.getMaxWeight()) < 0;
    }

    /** 运费对账：按承运商 + 签收/发货期间列运单级差异（账单金额 − 我方计费） */
    public Map<String, Object> reconcile(Long carrierId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<SaleOrderLogistics> wrapper = new LambdaQueryWrapper<SaleOrderLogistics>()
                .isNotNull(SaleOrderLogistics::getWaybillNo)
                .eq(carrierId != null, SaleOrderLogistics::getLogisticsCompanyId, carrierId);
        if (startDate != null) {
            wrapper.ge(SaleOrderLogistics::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(SaleOrderLogistics::getCreateTime, endDate.atTime(LocalTime.MAX));
        }
        List<SaleOrderLogistics> rows = logisticsMapper.selectList(wrapper);
        List<Map<String, Object>> details = new ArrayList<>();
        BigDecimal charged = BigDecimal.ZERO, billed = BigDecimal.ZERO;
        for (SaleOrderLogistics pkg : rows) {
            BigDecimal fee = nz(pkg.getShippingFee());
            BigDecimal bill = nz(pkg.getFreightBillAmount());
            BigDecimal diff = bill.subtract(fee);
            charged = charged.add(fee);
            billed = billed.add(bill);
            Map<String, Object> row = new LinkedHashMap<>();
            SaleOrder order = orderMapper.selectById(pkg.getOrderId());
            row.put("packageId", pkg.getId());
            row.put("orderId", pkg.getOrderId());
            row.put("orderNo", order == null ? null : order.getOrderNo());
            row.put("carrierId", pkg.getLogisticsCompanyId());
            row.put("carrierName", pkg.getLogisticsCompany());
            row.put("waybillNo", pkg.getWaybillNo());
            row.put("weight", pkg.getPackageWeight());
            row.put("shippingFee", fee);
            row.put("billAmount", pkg.getFreightBillAmount());
            row.put("diff", bill.subtract(fee));
            row.put("reconciled", pkg.getFreightReconciled());
            row.put("diffFlag", diff.abs().compareTo(new BigDecimal("0.01")) > 0);
            details.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("chargedAmount", charged.setScale(2, RoundingMode.HALF_UP));
        result.put("billedAmount", billed.setScale(2, RoundingMode.HALF_UP));
        result.put("diffAmount", billed.subtract(charged).setScale(2, RoundingMode.HALF_UP));
        result.put("diffCount", details.stream().filter(d -> Boolean.TRUE.equals(d.get("diffFlag"))).count());
        result.put("unReconciledCount", details.stream().filter(d -> !Integer.valueOf(1).equals(d.get("reconciled"))).count());
        result.put("details", details);
        return result;
    }

    /** 录入承运商账单金额 → 计算差异 */
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderLogistics saveBillAmount(Long packageId, BigDecimal billAmount) {
        SaleOrderLogistics pkg = logisticsMapper.selectById(packageId);
        if (pkg == null) {
            throw BusinessException.notFound("包裹不存在: " + packageId);
        }
        BigDecimal fee = nz(pkg.getShippingFee());
        logisticsMapper.update(null, new LambdaUpdateWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getId, packageId)
                .set(SaleOrderLogistics::getFreightBillAmount, billAmount)
                .set(SaleOrderLogistics::getFreightDiff, nz(billAmount).subtract(fee))
                .set(SaleOrderLogistics::getFreightReconciled, 0));
        return logisticsMapper.selectById(packageId);
    }

    /** 标记已对账 */
    @Transactional(rollbackFor = Exception.class)
    public int markReconciled(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            throw BusinessException.badRequest("请先勾选要标记的运单");
        }
        return logisticsMapper.update(null, new LambdaUpdateWrapper<SaleOrderLogistics>()
                .in(SaleOrderLogistics::getId, packageIds)
                .set(SaleOrderLogistics::getFreightReconciled, 1));
    }

    // ═══════════════════════════════════════════
    // P2-5 电子面单取号（配置化；未开通明确报错，不造假号）
    // ═══════════════════════════════════════════

    /** 取号是否开通（前端据此决定是否显示「获取电子面单」） */
    public boolean waybillEnabled() {
        return waybillEnabled && StringUtils.hasText(waybillUrl);
    }

    /** 发货通知回调是否已配置（未配置时通知只落台账） */
    public boolean asnConfigured() {
        return StringUtils.hasText(asnCallbackUrl);
    }

    /**
     * 获取电子面单运单号（调用配置的承运商接口）。
     *
     * <p>未配置 {@code erp.logistics.waybill.enabled/url} 时**明确报错**并提示手工录入——
     * 与地图 Key 同款策略：宁可降级也不返回假号（假号会污染对账与轨迹）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderLogistics acquireWaybill(Long packageId) {
        if (!waybillEnabled()) {
            throw BusinessException.badRequest("电子面单未开通：请配置 erp.logistics.waybill.enabled/url，或直接手工录入运单号");
        }
        SaleOrderLogistics pkg = logisticsMapper.selectById(packageId);
        if (pkg == null) {
            throw BusinessException.notFound("包裹不存在: " + packageId);
        }
        SaleOrder order = orderMapper.selectById(pkg.getOrderId());
        ObjectNode req = MAPPER.createObjectNode();
        req.put("carrierId", pkg.getLogisticsCompanyId());
        req.put("carrierName", pkg.getLogisticsCompany());
        req.put("orderNo", order == null ? null : order.getOrderNo());
        req.put("receiverName", order == null ? null : order.getReceiverName());
        req.put("receiverPhone", order == null ? null : order.getReceiverPhone());
        req.put("address", order == null ? null : order.getShippingAddress());
        req.put("weight", pkg.getPackageWeight());
        req.put("count", pkg.getPackageCount());
        String waybillNo = callWaybillApi(req);
        if (!StringUtils.hasText(waybillNo)) {
            throw BusinessException.badRequest("承运商未返回运单号（请检查 erp.logistics.waybill.response-path 配置）");
        }
        Long dup = logisticsMapper.selectCount(new LambdaQueryWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getWaybillNo, waybillNo)
                .ne(SaleOrderLogistics::getId, packageId));
        if (dup != null && dup > 0) {
            throw BusinessException.badRequest("取号重复（该运单号已被使用）: " + waybillNo);
        }
        logisticsMapper.update(null, new LambdaUpdateWrapper<SaleOrderLogistics>()
                .eq(SaleOrderLogistics::getId, packageId)
                .set(SaleOrderLogistics::getWaybillNo, waybillNo));
        syncOrderSnapshot(order);
        log.info("电子面单取号: packageId={}, waybillNo={}", packageId, waybillNo);
        return logisticsMapper.selectById(packageId);
    }

    private String callWaybillApi(ObjectNode req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(waybillToken)) {
                headers.setBearerAuth(waybillToken);
            }
            String body = restTemplate().postForObject(waybillUrl,
                    new HttpEntity<>(MAPPER.writeValueAsString(req), headers), String.class);
            JsonNode root = MAPPER.readTree(body == null ? "{}" : body);
            JsonNode node = root;
            for (String seg : waybillResponsePath.split("\\.")) {
                node = node == null ? null : node.get(seg);
            }
            return node == null || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            log.error("电子面单取号失败: url={}", waybillUrl, e);
            throw BusinessException.badRequest("电子面单取号失败: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // P2-6 发货通知 ASN
    // ═══════════════════════════════════════════

    /** 发货后生成 ASN 台账（按 订单+运单号 幂等）；无运单号则不生成 */
    @Transactional(rollbackFor = Exception.class)
    public int createNotifyForOrder(Long orderId) {
        SaleOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return 0;
        }
        int created = 0;
        for (SaleOrderLogistics pkg : listPackages(orderId)) {
            if (!StringUtils.hasText(pkg.getWaybillNo())) {
                continue;
            }
            String idem = orderId + ":" + pkg.getWaybillNo();
            Long exist = notifyMapper.selectCount(new LambdaQueryWrapper<ShipmentNotify>()
                    .eq(ShipmentNotify::getIdempotentKey, idem));
            if (exist != null && exist > 0) {
                continue;
            }
            ShipmentNotify notify = new ShipmentNotify()
                    .setOrderId(orderId).setOrderNo(order.getOrderNo())
                    .setCarrierId(pkg.getLogisticsCompanyId()).setCarrierName(pkg.getLogisticsCompany())
                    .setWaybillNo(pkg.getWaybillNo())
                    .setNotifyType("ASN").setStatus(0).setRetryCount(0)
                    .setIdempotentKey(idem)
                    .setTargetUrl(StringUtils.hasText(asnCallbackUrl) ? asnCallbackUrl : null)
                    .setPayload(buildAsnPayload(order, pkg));
            notifyMapper.insert(notify);
            created++;
        }
        return created;
    }

    private String buildAsnPayload(SaleOrder order, SaleOrderLogistics pkg) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("orderNo", order.getOrderNo());
            node.put("waybillNo", pkg.getWaybillNo());
            node.put("carrierName", pkg.getLogisticsCompany());
            node.put("packageNo", pkg.getPackageNo());
            node.put("packageCount", pkg.getPackageCount());
            node.put("packageWeight", pkg.getPackageWeight());
            node.put("receiverName", order.getReceiverName());
            node.put("receiverPhone", order.getReceiverPhone());
            node.put("shippingAddress", order.getShippingAddress());
            node.put("shippedAt", LocalDateTime.now().toString());
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Page<ShipmentNotify> notifyPage(Integer status, String orderNo, long current, long size) {
        return notifyMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ShipmentNotify>()
                .eq(status != null, ShipmentNotify::getStatus, status)
                .like(StringUtils.hasText(orderNo), ShipmentNotify::getOrderNo, orderNo)
                .orderByDesc(ShipmentNotify::getCreateTime));
    }

    /** 推送一条发货通知；未配置回调地址 → 保持待发送并记录原因（不算失败） */
    @Transactional(rollbackFor = Exception.class)
    public ShipmentNotify sendNotify(Long id) {
        ShipmentNotify notify = notifyMapper.selectById(id);
        if (notify == null) {
            throw BusinessException.notFound("发货通知不存在: " + id);
        }
        if (!StringUtils.hasText(asnCallbackUrl)) {
            notifyMapper.update(null, new LambdaUpdateWrapper<ShipmentNotify>()
                    .eq(ShipmentNotify::getId, id)
                    .set(ShipmentNotify::getLastError, "未配置 erp.shipment.asn.callback-url，暂存台账待发送")
                    .set(ShipmentNotify::getTargetUrl, null));
            return notifyMapper.selectById(id);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(asnToken)) {
                headers.setBearerAuth(asnToken);
            }
            restTemplate().postForObject(asnCallbackUrl,
                    new HttpEntity<>(notify.getPayload(), headers), String.class);
            notifyMapper.update(null, new LambdaUpdateWrapper<ShipmentNotify>()
                    .eq(ShipmentNotify::getId, id)
                    .set(ShipmentNotify::getStatus, 1)
                    .set(ShipmentNotify::getSentTime, LocalDateTime.now())
                    .set(ShipmentNotify::getTargetUrl, asnCallbackUrl)
                    .set(ShipmentNotify::getLastError, null)
                    .set(notify.getRetryCount() == null, ShipmentNotify::getRetryCount, 0));
            log.info("发货通知已发送: id={}, orderNo={}, waybillNo={}", id, notify.getOrderNo(), notify.getWaybillNo());
        } catch (Exception e) {
            int retry = (notify.getRetryCount() == null ? 0 : notify.getRetryCount()) + 1;
            notifyMapper.update(null, new LambdaUpdateWrapper<ShipmentNotify>()
                    .eq(ShipmentNotify::getId, id)
                    .set(ShipmentNotify::getStatus, 2)
                    .set(ShipmentNotify::getRetryCount, retry)
                    .set(ShipmentNotify::getLastError, String.valueOf(e.getMessage()).substring(0, Math.min(480, String.valueOf(e.getMessage()).length())))
                    .set(ShipmentNotify::getTargetUrl, asnCallbackUrl));
            log.warn("发货通知发送失败: id={}, retry={}, err={}", id, retry, e.getMessage());
        }
        return notifyMapper.selectById(id);
    }

    /** 推送全部待发送/失败的通知（供手工重试 / 定时任务） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendPending() {
        List<ShipmentNotify> list = notifyMapper.selectList(new LambdaQueryWrapper<ShipmentNotify>()
                .in(ShipmentNotify::getStatus, 0, 2));
        int ok = 0, fail = 0;
        for (ShipmentNotify n : list) {
            ShipmentNotify after = sendNotify(n.getId());
            if (after != null && Integer.valueOf(1).equals(after.getStatus())) {
                ok++;
            } else {
                fail++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("sent", ok);
        result.put("failed", fail);
        result.put("configured", StringUtils.hasText(asnCallbackUrl));
        return result;
    }

    // ═══════════════════════════════════════════
    // 私有辅助
    // ═══════════════════════════════════════════

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}

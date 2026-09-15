package cn.aiedge.dms.channel.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.channel.adapter.DeliveryAdapter;
import cn.aiedge.dms.channel.dto.ChannelRiderStatVO;
import cn.aiedge.dms.channel.dto.ChannelVO;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送渠道（运力来源）服务
 *
 * 金标准要点（见《渠道管理开发文档》§3）：
 * · 出参脱敏：对接凭据（AppSecret/token/password/...）一律以 ****** 返回，且回传掩码时不覆盖原值；
 * · 真删除：删除前做引用保护（被配送员引用则拒绝），不再用「改状态」冒充删除；
 * · 在线运力不落冗余列，按 dms_rider.channel_id 实时统计（列表一次查全量，避免 N+1）；
 * · 连通性测试与运力同步走适配器；适配器未对接时如实返回「未对接」，不伪造成功。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    /** 脱敏掩码 */
    public static final String MASK = "******";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DmsChannelMapper channelMapper;
    private final DmsRiderMapper riderMapper;
    private final ApplicationContext applicationContext;
    /** 对接凭据字段级加密（AES-GCM，见 §7.4） */
    private final ChannelCryptoService cryptoService;

    // ══════════════════════════════════════════════════════════
    // 查询
    // ══════════════════════════════════════════════════════════

    /** 多条件分页：编码/名称模糊 + 类型/对接状态/启用状态 + 创建时间区间 */
    public Page<ChannelVO> page(int pageNum, int pageSize, String channelCode, String channelName,
                                Integer channelType, Integer linkStatus, Integer status,
                                LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DmsChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(hasText(channelCode), DmsChannel::getChannelCode, channelCode)
                .like(hasText(channelName), DmsChannel::getChannelName, channelName)
                .eq(channelType != null, DmsChannel::getChannelType, channelType)
                .eq(linkStatus != null, DmsChannel::getLinkStatus, linkStatus)
                .eq(status != null, DmsChannel::getStatus, status)
                .ge(startDate != null, DmsChannel::getCreateTime,
                        startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, DmsChannel::getCreateTime,
                        endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByAsc(DmsChannel::getPriority)
                .orderByDesc(DmsChannel::getId);

        Page<DmsChannel> raw = channelMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<ChannelVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(toVOList(raw.getRecords()));
        return result;
    }

    /** 渠道详情（脱敏 + 实时运力） */
    public ChannelVO getDetail(Long id) {
        DmsChannel channel = getById(id);
        return toVOList(List.of(channel)).get(0);
    }

    /** 原实体（内部使用，不脱敏） */
    public DmsChannel getById(Long id) {
        DmsChannel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.notFound("渠道不存在");
        }
        return channel;
    }

    public DmsChannel getByCode(String channelCode) {
        return channelMapper.selectOne(
                new LambdaQueryWrapper<DmsChannel>().eq(DmsChannel::getChannelCode, channelCode));
    }

    /** 按渠道编码跨租户查询（回调/无登录会话场景使用：多租户插件在无会话时不注入条件） */
    public List<DmsChannel> findByCodeAcrossTenants(String channelCode) {
        return channelMapper.selectByCodeIgnoreTenant(channelCode);
    }

    /**
     * 库内对接配置 → 明文（供适配器调用与回调验签使用，**结果不出服务端**）
     * 无 ENCv1 前缀的历史明文原样返回；解密失败抛业务异常（fail closed）。
     */
    public String decryptConfig(DmsChannel channel) {
        return cryptoService.decryptConfigJson(channel.getConfigJson());
    }

    /** 所有已启用渠道（按优先级排序），供派单/下拉使用 */
    public List<DmsChannel> getAvailableChannels() {
        return channelMapper.selectList(
                new LambdaQueryWrapper<DmsChannel>()
                        .eq(DmsChannel::getStatus, 1)
                        .orderByAsc(DmsChannel::getPriority));
    }

    /** 按ID批量取渠道（派单预览一次查全量，避免逐单 N+1） */
    public Map<Long, DmsChannel> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, DmsChannel> map = new HashMap<>();
        for (DmsChannel channel : channelMapper.selectBatchIds(ids)) {
            map.put(channel.getId(), channel);
        }
        return map;
    }

    /** 渠道适配器实例（派单流程按渠道编码取用） */
    public DeliveryAdapter getAdapter(String channelCode) {
        DmsChannel channel = getByCode(channelCode);
        if (channel == null) {
            throw BusinessException.notFound("渠道不存在: " + channelCode);
        }
        if (!hasText(channel.getAdapterBean())) {
            throw BusinessException.badRequest("渠道未配置适配器: " + channelCode);
        }
        DeliveryAdapter adapter = resolveAdapter(channel.getAdapterBean());
        if (adapter == null) {
            throw BusinessException.badRequest("适配器未启用或不存在: " + channel.getAdapterBean());
        }
        return adapter;
    }

    /**
     * 解析渠道适配器（软解析：未配置/未启用返回 null，不抛异常）。
     * 供派单链路给出可读原因、并在错误信息里附上当前已装配适配器清单。
     */
    public DeliveryAdapter resolveAdapter(DmsChannel channel) {
        return channel == null ? null : resolveAdapter(channel.getAdapterBean());
    }

    /**
     * 解析渠道适配器：入参既可为 Spring Bean 名，也可为适配器自身声明的 {@code channelCode}。
     * （配置里写 channelCode 更贴合 {@link DeliveryAdapter#getChannelCode()} 的设计约定。）
     */
    private DeliveryAdapter resolveAdapter(String beanNameOrCode) {
        if (!hasText(beanNameOrCode)) {
            return null;
        }
        if (applicationContext.containsBean(beanNameOrCode)) {
            return applicationContext.getBean(beanNameOrCode, DeliveryAdapter.class);
        }
        for (DeliveryAdapter adapter : applicationContext.getBeansOfType(DeliveryAdapter.class).values()) {
            if (beanNameOrCode.equalsIgnoreCase(adapter.getChannelCode())) {
                return adapter;
            }
        }
        return null;
    }

    /** 当前装配的适配器（Bean 名 / channelCode），用于排查「渠道连不上」的原因 */
    public String availableAdapterNames() {
        try {
            Map<String, DeliveryAdapter> beans = applicationContext.getBeansOfType(DeliveryAdapter.class);
            if (beans.isEmpty()) {
                return "无";
            }
            List<String> names = new ArrayList<>();
            beans.forEach((name, adapter) -> names.add(name + "(" + adapter.getChannelCode() + ")"));
            return String.join(", ", names);
        } catch (Exception e) {
            return "未知";
        }
    }

    // ══════════════════════════════════════════════════════════
    // 写操作
    // ══════════════════════════════════════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public DmsChannel create(DmsChannel dto) {
        if (!hasText(dto.getChannelCode())) {
            throw new DmsBusinessException("渠道编码不能为空");
        }
        String code = dto.getChannelCode().trim();
        assertCodeUnique(code, null);

        DmsChannel channel = new DmsChannel();
        // 注意：不设置 tenantId —— 交给多租户插件按登录会话注入。
        // 旧实现硬编码 tenantId=0，新建数据会落在租户 0、当前租户查询不可见。
        channel.setChannelCode(code);
        channel.setChannelName(dto.getChannelName());
        channel.setChannelType(dto.getChannelType());
        channel.setAdapterBean(dto.getAdapterBean());
        // 敏感凭据（AppSecret/token/...）加密后落库；非敏感键保持明文可读
        channel.setConfigJson(cryptoService.encryptConfigJson(dto.getConfigJson()));
        channel.setPriority(dto.getPriority() == null ? 100 : dto.getPriority());
        channel.setSortOrder(dto.getSortOrder());
        channel.setLinkStatus(0);
        channel.setCoverageArea(dto.getCoverageArea());
        channel.setBillingType(dto.getBillingType() == null ? 1 : dto.getBillingType());
        channel.setBillingConfig(dto.getBillingConfig());
        channel.setRemark(dto.getRemark());
        channel.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        channelMapper.insert(channel);
        log.info("新增配送渠道: id={}, code={}", channel.getId(), code);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public DmsChannel update(Long id, DmsChannel dto) {
        DmsChannel channel = getById(id);
        // 渠道编码为业务主键（外部平台以此对接），创建后不可改
        channel.setChannelName(dto.getChannelName());
        channel.setChannelType(dto.getChannelType());
        channel.setAdapterBean(dto.getAdapterBean());
        // 回传的是脱敏值时按「同名键保留库中原值」逐键合并，避免把 ****** 写进库；
        // 普通字段（appKey/回调地址/网关）仍可正常修改。合并结果重新加密落库。
        if (dto.getConfigJson() != null) {
            String storedPlain = decryptConfig(channel);
            String merged = cryptoService.mergeMasked(storedPlain, dto.getConfigJson(), MASK);
            channel.setConfigJson(cryptoService.encryptConfigJson(merged));
        }
        channel.setPriority(dto.getPriority());
        channel.setSortOrder(dto.getSortOrder());
        channel.setCoverageArea(dto.getCoverageArea());
        if (dto.getBillingType() != null) {
            channel.setBillingType(dto.getBillingType());
        }
        channel.setBillingConfig(dto.getBillingConfig());
        channel.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            channel.setStatus(dto.getStatus());
        }
        if (dto.getLinkStatus() != null) {
            channel.setLinkStatus(dto.getLinkStatus());
        }
        channelMapper.updateById(channel);
        log.info("更新配送渠道: id={}", id);
        return channel;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsChannel channel = getById(id);
        channel.setStatus(status);
        channelMapper.updateById(channel);
        log.info("渠道状态更新: id={}, status={}", id, status);
    }

    /** 批量启停 */
    @Transactional(rollbackFor = Exception.class)
    public int batchStatus(Collection<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (Long id : ids) {
            DmsChannel channel = channelMapper.selectById(id);
            if (channel == null) {
                continue;
            }
            channel.setStatus(status);
            affected += channelMapper.updateById(channel);
        }
        log.info("批量启停渠道: status={}, affected={}", status, affected);
        return affected;
    }

    /** 真删除（引用保护：被配送员引用时拒绝，提示先解绑或停用） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsChannel channel = getById(id);
        assertNotReferenced(channel);
        channelMapper.deleteById(id);
        log.info("删除渠道: id={}, code={}", id, channel.getChannelCode());
    }

    /** 批量删除（逐条引用保护，全部通过才删） */
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<DmsChannel> channels = new ArrayList<>();
        for (Long id : ids) {
            DmsChannel channel = channelMapper.selectById(id);
            if (channel == null) {
                continue;
            }
            assertNotReferenced(channel);
            channels.add(channel);
        }
        channels.forEach(c -> channelMapper.deleteById(c.getId()));
        log.info("批量删除渠道: count={}", channels.size());
        return channels.size();
    }

    // ══════════════════════════════════════════════════════════
    // 适配器能力：连通性测试 / 运力同步
    // ══════════════════════════════════════════════════════════

    /** 连通性测试结果 */
    public record TestResult(boolean success, Integer linkStatus, String message, LocalDateTime testTime) {
    }

    /**
     * 连通性测试：调用渠道适配器的探活能力，并回写对接状态与最近测试结果。
     * 适配器未对接（Stub）时如实返回失败与原因，不伪造连通。
     */
    @Transactional(rollbackFor = Exception.class)
    public TestResult testConnection(Long id) {
        DmsChannel channel = getById(id);
        String bean = channel.getAdapterBean();
        boolean success;
        int linkStatus;
        String message;

        DeliveryAdapter adapter = resolveAdapter(bean);
        if (!hasText(bean)) {
            success = false;
            linkStatus = 0;
            message = "渠道未配置适配器，无法接通";
        } else if (adapter == null) {
            success = false;
            linkStatus = 2;
            message = "适配器未启用或不存在：" + bean + "（请确认 dms.channel.*.enabled 开关；"
                    + "当前可用适配器：" + availableAdapterNames() + "）";
            log.warn("渠道未找到适配器: id={}, adapterBean={}, 可用={}", id, bean, availableAdapterNames());
        } else {
            try {
                DeliveryAdapter.CheckResult r = adapter.check();
                success = r.success();
                linkStatus = success ? 1 : 2;
                message = r.message();
            } catch (Exception e) {
                success = false;
                linkStatus = 2;
                message = "适配器探活异常：" + e.getMessage();
                log.warn("渠道连通性测试异常: id={}, bean={}", id, bean, e);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        channel.setLinkStatus(linkStatus);
        channel.setLastTestTime(now);
        channel.setLastTestResult(message);
        channelMapper.updateById(channel);
        return new TestResult(success, linkStatus, message, now);
    }

    /** 运力同步结果 */
    public record SyncResult(int inserted, int updated, String message) {
    }

    /**
     * 同步外部平台运力：调用适配器拉取配送员并落 {@code dms_rider}（渠道关联）。
     * 适配器未对接时返回 0 条并说明原因（不伪造数据）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncRiders(Long id) {
        DmsChannel channel = getById(id);
        String bean = channel.getAdapterBean();
        if (!hasText(bean)) {
            return new SyncResult(0, 0, "渠道未对接（无可用适配器），无可同步运力");
        }

        DeliveryAdapter adapter = resolveAdapter(bean);
        if (adapter == null) {
            return new SyncResult(0, 0, "适配器未启用或不存在：" + bean);
        }

        List<DeliveryAdapter.RemoteRider> remote;
        try {
            remote = adapter.fetchRiders();
        } catch (Exception e) {
            log.warn("拉取渠道运力失败: id={}, bean={}", id, bean, e);
            return new SyncResult(0, 0, "拉取外部运力失败：" + e.getMessage());
        }
        if (remote == null || remote.isEmpty()) {
            return new SyncResult(0, 0, "该渠道当前无可同步运力（适配器未对接或平台无在线运力）");
        }

        Long tenantId = currentTenantId();
        int inserted = 0;
        int updated = 0;
        for (DeliveryAdapter.RemoteRider r : remote) {
            if (!hasText(r.phone())) {
                continue;
            }
            DmsRider exist = riderMapper.selectOne(new LambdaQueryWrapper<DmsRider>()
                    .eq(DmsRider::getChannelId, id)
                    .eq(DmsRider::getPhone, r.phone())
                    .last("LIMIT 1"));
            if (exist == null) {
                DmsRider rider = new DmsRider();
                rider.setRiderType(3); // 3-外部平台骑手
                rider.setChannelId(id);
                rider.setRealName(r.realName());
                rider.setPhone(r.phone());
                rider.setVehicleType(r.vehicleType());
                rider.setVehicleNo(r.vehicleNo());
                rider.setStatus(0);
                rider.setVerifyStatus(0);
                rider.setTenantId(tenantId);
                riderMapper.insert(rider);
                inserted++;
            } else {
                exist.setRealName(r.realName());
                exist.setVehicleType(r.vehicleType());
                exist.setVehicleNo(r.vehicleNo());
                riderMapper.updateById(exist);
                updated++;
            }
        }
        channel.setLinkStatus(1);
        channel.setLastTestResult("运力同步完成：新增 " + inserted + "，更新 " + updated);
        channelMapper.updateById(channel);
        log.info("渠道运力同步: id={}, inserted={}, updated={}", id, inserted, updated);
        return new SyncResult(inserted, updated, "同步完成：新增 " + inserted + " 名，更新 " + updated + " 名");
    }

    // ══════════════════════════════════════════════════════════
    // 内部工具
    // ══════════════════════════════════════════════════════════

    private void assertCodeUnique(String code, Long excludeId) {
        Long count = channelMapper.selectCount(new LambdaQueryWrapper<DmsChannel>()
                .eq(DmsChannel::getChannelCode, code)
                .ne(excludeId != null, DmsChannel::getId, excludeId));
        if (count != null && count > 0) {
            throw new DmsBusinessException("渠道编码已存在：" + code);
        }
    }

    private void assertNotReferenced(DmsChannel channel) {
        Long used = channelMapper.countRidersByChannel(currentTenantId(), channel.getId());
        if (used != null && used > 0) {
            throw new DmsBusinessException(
                    "该渠道已被 " + used + " 名配送员引用，不能删除；请先解绑或改为停用");
        }
    }

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new DmsBusinessException("无法获取当前租户上下文");
        }
        return tenantId;
    }

    /** 实体 → VO（脱敏 + 批量补运营力统计） */
    private List<ChannelVO> toVOList(List<DmsChannel> channels) {
        Map<Long, ChannelRiderStatVO> statMap = new HashMap<>();
        if (channels != null && !channels.isEmpty()) {
            for (ChannelRiderStatVO stat : channelMapper.selectRiderStat(currentTenantId())) {
                statMap.put(stat.getChannelId(), stat);
            }
        }
        List<ChannelVO> list = new ArrayList<>();
        if (channels == null) {
            return list;
        }
        for (DmsChannel channel : channels) {
            ChannelVO vo = new ChannelVO();
            BeanUtils.copyProperties(channel, vo);
            vo.setConfigJson(maskConfigJson(channel.getConfigJson()));
            ChannelRiderStatVO stat = statMap.get(channel.getId());
            vo.setRiderTotal(stat == null || stat.getRiderTotal() == null ? 0 : stat.getRiderTotal());
            vo.setRiderOnline(stat == null || stat.getRiderOnline() == null ? 0 : stat.getRiderOnline());
            list.add(vo);
        }
        return list;
    }

    /** 对接配置脱敏：命中敏感键名的值替换为掩码；非 JSON 一律隐藏（宁可不可读也不泄露） */
    private String maskConfigJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode root = JSON.readTree(json);
            maskNode(root);
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"_masked\":\"配置格式非法，已隐藏\"}";
        }
    }

    private void maskNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode child = obj.get(name);
                if (child.isValueNode()) {
                    if (isSecretKey(name) && hasText(child.asText())) {
                        obj.put(name, MASK);
                    }
                } else if (child.isContainerNode()) {
                    maskNode(child);
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::maskNode);
        }
    }

    private boolean isSecretKey(String name) {
        return ChannelCryptoService.isSecretKey(name);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

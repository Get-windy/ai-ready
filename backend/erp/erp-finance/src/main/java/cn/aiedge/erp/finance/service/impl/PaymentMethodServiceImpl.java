package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.mapper.FinanceAccountMapper;
import cn.aiedge.erp.finance.mapper.PaymentChannelMapper;
import cn.aiedge.erp.finance.mapper.PaymentMethodMapper;
import cn.aiedge.erp.finance.model.dto.PaymentMethodQuery;
import cn.aiedge.erp.finance.model.dto.PaymentMethodVO;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import cn.aiedge.erp.finance.service.PaymentMethodService;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.mapper.PaymentMapper;
import cn.aiedge.erp.payment.mapper.ReceiptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 支付方式Service实现类
 *
 * 红线（《支付方式开发文档》最终裁决）：
 *   支付方式为全局唯一基础字典（md_payment_method），收款/付款/预收/预付统一引用，严禁另建重复字典。
 *
 * 业务规则：
 *   1. method_code 租户内唯一（统一大写存储，编辑时校验排除自身）
 *   2. is_default 全租户唯一（置默认时自动清除其它默认）
 *   3. 删除做引用保护：被支付渠道 / 收款单 / 付款单引用时拒绝删除
 *   4. 停用时同时清除默认标记（停用项不应是系统默认）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodServiceImpl extends ServiceImpl<PaymentMethodMapper, PaymentMethod>
        implements PaymentMethodService {

    /** 支付方式类型枚举（与前端 METHOD_TYPE_MAP 一一对应） */
    private static final Set<String> METHOD_TYPES =
            Set.of("CASH", "BANK", "WECHAT", "ALIPAY", "CHECK", "OTHER");

    private final FinanceAccountMapper financeAccountMapper;
    private final PaymentChannelMapper paymentChannelMapper;
    private final ReceiptMapper receiptMapper;
    private final PaymentMapper paymentMapper;

    // ==================== 查询 ====================

    @Override
    public Page<PaymentMethodVO> pageQuery(PaymentMethodQuery query) {
        PaymentMethodQuery q = query != null ? query : new PaymentMethodQuery();
        Page<PaymentMethod> page = page(
                new Page<>(normalizePageNum(q.getPageNum()), normalizePageSize(q.getPageSize())),
                buildWrapper(q));

        List<PaymentMethodVO> records = toVOList(page.getRecords());
        Page<PaymentMethodVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public List<PaymentMethodVO> listByQuery(PaymentMethodQuery query) {
        return toVOList(list(buildWrapper(query != null ? query : new PaymentMethodQuery())));
    }

    @Override
    public PaymentMethodVO getDetail(Long id) {
        PaymentMethod entity = getById(id);
        if (entity == null) {
            throw BusinessException.notFound("支付方式不存在: " + id);
        }
        return toVOList(List.of(entity)).get(0);
    }

    @Override
    public List<PaymentMethodVO> listEnabled() {
        PaymentMethodQuery q = new PaymentMethodQuery();
        q.setStatus(1);
        return toVOList(list(buildWrapper(q)));
    }

    private LambdaQueryWrapper<PaymentMethod> buildWrapper(PaymentMethodQuery q) {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<>();
        String keyword = q.getKeyword() == null ? null : q.getKeyword().trim();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(PaymentMethod::getMethodCode, keyword)
                .or().like(PaymentMethod::getMethodName, keyword));
        wrapper.eq(StringUtils.hasText(q.getMethodType()), PaymentMethod::getMethodType, q.getMethodType());
        // status 优先；未指定时按「显示停用」口径：未勾选只看启用
        if (q.getStatus() != null) {
            wrapper.eq(PaymentMethod::getStatus, q.getStatus());
        } else if (q.getShowDisabled() == null || q.getShowDisabled() != 1) {
            wrapper.eq(PaymentMethod::getStatus, 1);
        }
        wrapper.orderByAsc(PaymentMethod::getSort).orderByDesc(PaymentMethod::getCreateTime);
        return wrapper;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 500);
    }

    /** 实体 → 出参；批量回填默认入账账户名称（一次查询，避免 N+1） */
    private List<PaymentMethodVO> toVOList(List<PaymentMethod> rows) {
        Map<Long, String> accountNames = loadAccountNames(rows);
        List<PaymentMethodVO> list = new ArrayList<>(rows.size());
        for (PaymentMethod row : rows) {
            list.add(toVO(row, accountNames));
        }
        return list;
    }

    private Map<Long, String> loadAccountNames(List<PaymentMethod> rows) {
        Set<Long> ids = rows.stream()
                .map(PaymentMethod::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return financeAccountMapper.selectBatchIds(ids).stream()
                .filter(a -> a.getAccountName() != null)
                .collect(Collectors.toMap(FinanceAccount::getId, FinanceAccount::getAccountName, (a, b) -> a));
    }

    private PaymentMethodVO toVO(PaymentMethod entity, Map<Long, String> accountNames) {
        PaymentMethodVO vo = new PaymentMethodVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setMethodCode(entity.getMethodCode());
        vo.setMethodName(entity.getMethodName());
        vo.setMethodType(entity.getMethodType());
        vo.setAccountId(entity.getAccountId());
        vo.setAccountName(entity.getAccountId() == null ? null : accountNames.get(entity.getAccountId()));
        vo.setFeeRate(entity.getFeeRate());
        vo.setIsDefault(entity.getIsDefault());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentMethodVO create(PaymentMethod entity) {
        entity.setId(null);
        normalizeAndValidate(entity);
        ensureCodeUnique(entity.getMethodCode(), null);

        entity.setStatus(entity.getStatus() == null ? 1 : entity.getStatus());
        entity.setIsDefault(entity.getIsDefault() == null ? 0 : entity.getIsDefault());
        save(entity);

        if (entity.getIsDefault() == 1) {
            clearOtherDefaults(entity.getId());
        }
        return getDetail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentMethodVO updateMethod(Long id, PaymentMethod entity) {
        PaymentMethod exist = getById(id);
        if (exist == null) {
            throw BusinessException.notFound("支付方式不存在: " + id);
        }
        entity.setId(id);
        normalizeAndValidate(entity);
        ensureCodeUnique(entity.getMethodCode(), id);

        // 停用项不允许保留默认标记
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            entity.setIsDefault(0);
        }

        // 用 UpdateWrapper 显式 set 全部字段：PUT 为整体覆盖语义，
        // 且 account_id / remark 清空（null）时必须真正落库（updateById 默认忽略 null 字段）
        LambdaUpdateWrapper<PaymentMethod> updateWrapper = new LambdaUpdateWrapper<PaymentMethod>()
                .set(PaymentMethod::getMethodCode, entity.getMethodCode())
                .set(PaymentMethod::getMethodName, entity.getMethodName())
                .set(PaymentMethod::getMethodType, entity.getMethodType())
                .set(PaymentMethod::getAccountId, entity.getAccountId())
                .set(PaymentMethod::getFeeRate, entity.getFeeRate())
                .set(PaymentMethod::getIsDefault, entity.getIsDefault())
                .set(PaymentMethod::getSort, entity.getSort())
                .set(PaymentMethod::getStatus, entity.getStatus())
                .set(PaymentMethod::getRemark, entity.getRemark())
                .eq(PaymentMethod::getId, id);
        update(updateWrapper);

        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearOtherDefaults(id);
        }
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PaymentMethod exist = getById(id);
        if (exist == null) {
            throw BusinessException.notFound("支付方式不存在: " + id);
        }
        String blocked = describeReferences(exist);
        if (blocked != null) {
            throw BusinessException.badRequest("「" + exist.getMethodName() + "」已被引用，无法删除：" + blocked
                    + "。如不再使用请改为「停用」。");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentMethodVO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态值非法，仅支持 0-停用 / 1-启用");
        }
        PaymentMethod exist = getById(id);
        if (exist == null) {
            throw BusinessException.notFound("支付方式不存在: " + id);
        }
        // 只 set 状态字段：PaymentMethod 实体的 feeRate/isDefault/sort/status 带默认初始值，
        // 直接 updateById(new PaymentMethod()) 会把未赋值字段一并写回 0（数据污染）
        LambdaUpdateWrapper<PaymentMethod> wrapper = new LambdaUpdateWrapper<PaymentMethod>()
                .set(PaymentMethod::getStatus, status)
                .set(status == 0, PaymentMethod::getIsDefault, 0)
                .eq(PaymentMethod::getId, id);
        update(wrapper);
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先勾选要操作的支付方式");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态值非法，仅支持 0-停用 / 1-启用");
        }
        LambdaUpdateWrapper<PaymentMethod> wrapper = new LambdaUpdateWrapper<PaymentMethod>()
                .set(PaymentMethod::getStatus, status)
                .in(PaymentMethod::getId, ids);
        if (status == 0) {
            wrapper.set(PaymentMethod::getIsDefault, 0);
        }
        update(wrapper);
        return ids.size();
    }

    // ==================== 内部校验 ====================

    /** 归一化 + 必填/取值校验 */
    private void normalizeAndValidate(PaymentMethod entity) {
        if (!StringUtils.hasText(entity.getMethodCode())) {
            throw BusinessException.badRequest("请输入支付方式编码");
        }
        entity.setMethodCode(entity.getMethodCode().trim().toUpperCase());
        if (entity.getMethodCode().length() > 50) {
            throw BusinessException.badRequest("支付方式编码不能超过 50 个字符");
        }

        if (!StringUtils.hasText(entity.getMethodName())) {
            throw BusinessException.badRequest("请输入支付方式名称");
        }
        entity.setMethodName(entity.getMethodName().trim());
        if (entity.getMethodName().length() > 100) {
            throw BusinessException.badRequest("支付方式名称不能超过 100 个字符");
        }

        if (!StringUtils.hasText(entity.getMethodType())
                || !METHOD_TYPES.contains(entity.getMethodType())) {
            throw BusinessException.badRequest("请选择合法的支付方式类型：" + String.join("/", METHOD_TYPES));
        }

        BigDecimal feeRate = entity.getFeeRate() == null ? BigDecimal.ZERO : entity.getFeeRate();
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(BigDecimal.ONE) > 0) {
            throw BusinessException.badRequest("手续费率需在 0% ~ 100% 之间");
        }
        entity.setFeeRate(feeRate);

        if (entity.getSort() == null || entity.getSort() < 0) {
            entity.setSort(0);
        }
        if (entity.getStatus() != null && entity.getStatus() != 0 && entity.getStatus() != 1) {
            throw BusinessException.badRequest("状态值非法，仅支持 0-停用 / 1-启用");
        }
        if (entity.getIsDefault() != null && entity.getIsDefault() != 0 && entity.getIsDefault() != 1) {
            throw BusinessException.badRequest("默认标记非法，仅支持 0-否 / 1-是");
        }

        if (entity.getAccountId() != null && financeAccountMapper.selectById(entity.getAccountId()) == null) {
            throw BusinessException.badRequest("默认入账账户不存在，请重新选择");
        }
    }

    private void ensureCodeUnique(String methodCode, Long excludeId) {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getMethodCode, methodCode)
                .ne(excludeId != null, PaymentMethod::getId, excludeId);
        if (count(wrapper) > 0) {
            throw BusinessException.badRequest("支付方式编码已存在：" + methodCode);
        }
    }

    /** 清除其它记录的默认标记，保证系统只有一个默认支付方式 */
    private void clearOtherDefaults(Long keepId) {
        update(new LambdaUpdateWrapper<PaymentMethod>()
                .set(PaymentMethod::getIsDefault, 0)
                .eq(PaymentMethod::getIsDefault, 1)
                .ne(PaymentMethod::getId, keepId));
    }

    /** 引用保护：返回被引用描述，无引用返回 null */
    private String describeReferences(PaymentMethod method) {
        List<String> parts = new ArrayList<>();

        Long channelCount = paymentChannelMapper.selectCount(new LambdaQueryWrapper<PaymentChannel>()
                .eq(PaymentChannel::getMethodId, method.getId()));
        if (channelCount != null && channelCount > 0) {
            parts.add("支付渠道 " + channelCount + " 条");
        }

        Long receiptCount = receiptMapper.selectCount(new LambdaQueryWrapper<Receipt>()
                .eq(Receipt::getPaymentMethod, method.getMethodCode()));
        if (receiptCount != null && receiptCount > 0) {
            parts.add("收款单 " + receiptCount + " 条");
        }

        Long paymentCount = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentMethod, method.getMethodCode()));
        if (paymentCount != null && paymentCount > 0) {
            parts.add("付款单 " + paymentCount + " 条");
        }

        return parts.isEmpty() ? null : String.join("；", parts);
    }
}

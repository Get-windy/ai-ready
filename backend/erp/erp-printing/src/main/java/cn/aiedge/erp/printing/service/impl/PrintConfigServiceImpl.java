package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.erp.printing.dto.PrintConfigRequest;
import cn.aiedge.erp.printing.entity.SetPrintConfig;
import cn.aiedge.erp.printing.mapper.SetPrintConfigMapper;
import cn.aiedge.erp.printing.service.PrintConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 打印设置服务实现（真实读写 {@code set_print_config}）
 *
 * <p><b>三处关键实现口径</b>（对应开发文档 §10.1 的验收项）：</p>
 * <ol>
 *   <li><b>租户由入参决定、不由客户端传</b>：控制层从登录会话解析 tenantId 后传进来；
 *       所有查询 / 更新都**显式带 {@code tenant_id} 条件** —— 不依赖多租户插件，
 *       因为平台超管的会话会「整体豁免租户注入」（见 {@code MyBatisPlusConfig.isTenantScopeExempt}），
 *       仅靠插件会导致超管读到**其它租户**的行。</li>
 *   <li><b>无行则自动建行</b>（一租户一行，唯一索引兜底）：并发首访撞唯一索引时
 *       捕获 {@link DuplicateKeyException} 后重查，避免把并发变成 500。</li>
 *   <li><b>保存后回读</b>：返回值一律来自库（而不是内存里的入参），保证「存得下」可被前端验证，
 *       避免本模块出现过的「假保存」（README §7.1 第 8/10 条）。</li>
 * </ol>
 *
 * <p><b>配置的消费点</b>（2026-09-18 接线）：本类仍只负责「配置存得下、读得回」，
 * 「配置改变行为」在前端共享组件侧落地 —— {@code components/PrintDialog/printBehavior.ts}
 * 读取 {@code GET /api/set/print-config} 后作用于打印数据与打印入口
 * （草稿门控 / 数量与单价小数位格式化 / 打印内容拼接 / 助手跳过预览 / 远程打印门控与链路提交）。
 * 仍**不接线**的两项（属性商品汇总打印、批次效期商品汇总打印）原因见该文件顶部说明：
 * 本系统打印引擎 {@code FormatEngineImpl.renderTable} 逐行直出、**没有汇总行能力**，不硬造。</p>
 *
 * <p>⚠️ <b>刻意不加 {@code @Transactional}</b>（这是本库的一个真实陷阱，不要"顺手补上"）：
 * 建行靠唯一索引兜底，撞索引时 PostgreSQL 会把**整个事务标记为 aborted** ——
 * 若在同一个事务里 catch {@link DuplicateKeyException} 后再回查，后续语句一律报
 * 「当前事务被终止」而失败。因此这里让每条语句各自成事务，catch 之后的重查才有意义。
 * 本页只有「一行一租户」的单表读写，跨语句原子性无实际收益。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintConfigServiceImpl implements PrintConfigService {

    private final SetPrintConfigMapper printConfigMapper;

    @Override
    public SetPrintConfig getOrCreate(Long tenantId, Long userId) {
        SetPrintConfig existing = selectByTenant(tenantId);
        if (existing != null) {
            return existing;
        }
        SetPrintConfig created = buildDefault(tenantId);
        // 审计列：MetaObjectHandler 只填 create_time/update_time/tenant_id，操作人须自己写
        created.setCreateBy(userId);
        created.setUpdateBy(userId);
        try {
            printConfigMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            // 并发首访：另一请求已建行 → 以库内行为准（唯一索引 uk_set_print_config_tenant 兜底）
            log.info("打印设置并发建行冲突，改为读取既有行: tenantId={}", tenantId);
            SetPrintConfig row = selectByTenant(tenantId);
            if (row == null) {
                throw e;
            }
            return row;
        }
    }

    @Override
    public SetPrintConfig save(Long tenantId, PrintConfigRequest request, Long userId) {
        // 校验（非法值 → IllegalArgumentException → 全局异常处理器转 HTTP 400）
        Integer qtyDecimal = request.getQtyDecimal();
        if (qtyDecimal != null && (qtyDecimal < DECIMAL_MIN || qtyDecimal > DECIMAL_MAX)) {
            throw new IllegalArgumentException("数量小数位必须在 " + DECIMAL_MIN + "~" + DECIMAL_MAX + " 之间");
        }
        Integer priceDecimal = request.getPriceDecimal();
        if (priceDecimal != null && (priceDecimal < DECIMAL_MIN || priceDecimal > DECIMAL_MAX)) {
            throw new IllegalArgumentException("单价小数位必须在 " + DECIMAL_MIN + "~" + DECIMAL_MAX + " 之间");
        }
        String printContent = request.getPrintContent();
        if (StringUtils.hasText(printContent)) {
            String trimmed = printContent.trim();
            if (trimmed.length() > PRINT_CONTENT_MAX_LENGTH) {
                throw new IllegalArgumentException("打印内容长度不能超过 " + PRINT_CONTENT_MAX_LENGTH + " 个字符");
            }
            request.setPrintContent(trimmed);
        }
        // ⚠️ 刻意**不做枚举校验**（选项集已实测，见下，但仍保持宽松）：
        //    ql361「打印内容」下拉的完整选项集已于 2026-09-18 实测为 3 项
        //    （controller.PRINT_CONTENT_OPTIONS，证据 _summary.md §三 3.1），前端下拉已收敛到这 3 项；
        //    但服务端**仍不做白名单硬校验** —— 一是库内已存量的历史值（含 ql361 rawValue 口径）不能被判非法，
        //    二是对标侧后续新增选项时不该先改后端才能保存。这里只做 trim + 长度上限（列宽 VARCHAR(64)）。

        SetPrintConfig existing = getOrCreate(tenantId, userId);

        int affected = updateRow(existing, request, userId);
        if (affected == 0) {
            // 乐观锁被拒（并发保存）→ 取最新版本重试一次；仍失败则明确报错，**绝不假装保存成功**
            SetPrintConfig latest = selectByTenant(tenantId);
            if (latest != null) {
                affected = updateRow(latest, request, userId);
            }
        }
        if (affected == 0) {
            throw new IllegalStateException("打印设置保存冲突，请重试");
        }

        // 回读：返回值必须是库里真实存下的值
        SetPrintConfig saved = selectByTenant(tenantId);
        return saved != null ? saved : existing;
    }

    /**
     * 以给定行为基准执行一次部分更新，返回受影响行数。
     *
     * <p>只 {@code set} 入参里非 null 的字段：MyBatis-Plus 的 {@code updateById} 默认忽略 null，
     * 因此「客户端没提交的项」不会被覆盖（部分更新语义）。带上 id/version 以启用乐观锁。</p>
     */
    private int updateRow(SetPrintConfig base, PrintConfigRequest request, Long userId) {
        SetPrintConfig patch = new SetPrintConfig();
        patch.setId(base.getId());
        patch.setVersion(base.getVersion());
        patch.setUpdateBy(userId);
        patch.setAllowDraftPrint(toInt(request.getAllowDraftPrint()));
        patch.setAttrSummaryPrint(toInt(request.getAttrSummaryPrint()));
        patch.setBatchSummaryPrint(toInt(request.getBatchSummaryPrint()));
        patch.setPrintContent(StringUtils.hasText(request.getPrintContent()) ? request.getPrintContent() : null);
        patch.setDecimalEnabled(toInt(request.getDecimalEnabled()));
        patch.setQtyDecimal(request.getQtyDecimal());
        patch.setPriceDecimal(request.getPriceDecimal());
        patch.setAssistantEnabled(toInt(request.getAssistantEnabled()));
        patch.setRemoteEnabled(toInt(request.getRemoteEnabled()));
        return printConfigMapper.updateById(patch);
    }

    /** 按租户取行（显式租户条件：超管会话豁免租户注入时仍能正确收敛） */
    private SetPrintConfig selectByTenant(Long tenantId) {
        List<SetPrintConfig> rows = printConfigMapper.selectList(
                new LambdaQueryWrapper<SetPrintConfig>()
                        .eq(SetPrintConfig::getTenantId, tenantId)
                        .orderByAsc(SetPrintConfig::getId));
        // 正常只有 1 行（唯一索引保证）；若历史数据出现多行，取最早一行而不抛 TooManyResultsException
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 默认配置 = **ql361 抓取时刻的实拍值**（开发文档 §7.3 明确标注：这是对标值，不等于本系统产品默认值）。
     * 与建表默认值保持一致，两处需同步修改。
     */
    private SetPrintConfig buildDefault(Long tenantId) {
        SetPrintConfig config = new SetPrintConfig();
        config.setTenantId(tenantId);
        config.setAllowDraftPrint(1);
        config.setAttrSummaryPrint(1);
        config.setBatchSummaryPrint(0);
        config.setPrintContent(DEFAULT_PRINT_CONTENT);
        config.setDecimalEnabled(0);
        config.setQtyDecimal(2);
        config.setPriceDecimal(2);
        config.setAssistantEnabled(1);
        config.setRemoteEnabled(1);
        return config;
    }

    /** 布尔 → 0/1（null 保持 null = 本次不改） */
    private Integer toInt(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }
}

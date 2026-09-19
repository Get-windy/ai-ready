package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.PrintConfigRequest;
import cn.aiedge.erp.printing.entity.SetPrintConfig;

/**
 * 打印设置服务（租户级配置，表 {@code set_print_config}）
 *
 * <p>只做「配置的读写」，不碰打印执行链路 —— 模板渲染 / 本地打印 / 打印链 / 客户端注册
 * 一律复用 {@code erp-printing} 模块的既有能力（《功能/模块不重复开发》）。</p>
 */
public interface PrintConfigService {

    /**
     * 小数位取值下界（0 位 = 「整数」）。
     *
     * <p>取值域是**本系统的适配口径**，不是 ql361 的档位：ql361 的「数量 / 单价」下拉为 9 档
     * （`整数` `1位小数` … `8位小数`，2026-09-18 实测，证据 {@code _summary.md} §三 3.1），
     * 而本系统主数据的数量/单价精度是 {@code numeric(18,4)}（见开发文档 §8.2）——
     * 本系统比对标少 5~8 位，避免打出 {@code numeric(18,4)} 存不下的假精度。
     * 若后续裁定要与对标完全对齐（放宽到 8 位），只需改这两个常量 + 库列精度，前端与格式化逻辑无需改动。</p>
     */
    int DECIMAL_MIN = 0;

    /** 小数位取值上界（4 位，对齐主数据精度 {@code numeric(18,4)}；对标档位为 8 位，见 {@link #DECIMAL_MIN}） */
    int DECIMAL_MAX = 4;

    /**
     * 打印内容的默认值（ql361 实拍选中值，逐字）。
     *
     * <p>完整选项集 3 项已实测，见 {@code PrintConfigController.PRINT_CONTENT_OPTIONS}；
     * 本值为其中的第一项（ql361 rawValue=1）。</p>
     */
    String DEFAULT_PRINT_CONTENT = "批号 *数量";

    /** 打印内容长度上限（与库列 {@code VARCHAR(64)} 一致，避免超长导致 SQL 报错） */
    int PRINT_CONTENT_MAX_LENGTH = 64;

    /**
     * 读取当前租户的打印设置；**不存在则按默认值建行**（口径明确：开发文档 §10.1-8 二选一，本实现取「自动建行」）。
     *
     * @param tenantId 当前登录租户（由控制层从会话解析，不由客户端传）
     * @param userId   操作人（写入新建行的 create_by / update_by，可为 null = 无法解析）
     */
    SetPrintConfig getOrCreate(Long tenantId, Long userId);

    /**
     * 保存当前租户的打印设置（部分更新：入参为 null 的字段保持原值），随后**回读**返回库内值。
     *
     * @param tenantId 当前登录租户
     * @param request  待更新字段（可空表示不改）
     * @param userId   操作人（写入 create_by / update_by，可为 null）
     */
    SetPrintConfig save(Long tenantId, PrintConfigRequest request, Long userId);
}

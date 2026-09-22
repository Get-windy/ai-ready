package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 「本版 vs 上一版（或指定版）」的逐条差异（§13.4：反要约要能逐条 diff）。
 *
 * <h3>为什么四类差异共用一个 {@link Item} 形状</h3>
 * 条款（terms）、设定（settings）、文字（narratives）、履约方式（fulfillmentModes）
 * 在界面上都是"一行 = 一处变化"，共用一张表就能让前端一套渲染逻辑吃下四类；
 * 若各写一个结构，前端要写四遍、后端要维护四个"变更类型"枚举，迟早不一致。
 *
 * <p>{@code changeType} 只有三种：{@code ADDED 新增 / REMOVED 删除 / CHANGED 修改}。</p>
 */
@Data
public class AgreementVersionDiffVO {

    private Long agreementId;

    private Long versionId;

    private Integer versionNo;

    /** 对比基准（不传时默认 = 上一版） */
    private Long againstVersionId;

    private Integer againstVersionNo;

    /** 基准的人读说明，如「上一版（第 1 版）」 */
    private String againstLabel;

    /** 本次对比的两份**快照原文**是否逐字一致（false 说明条款/期限被改过） */
    private Boolean snapshotChanged;

    private List<Item> terms = new ArrayList<>();

    private List<Item> settings = new ArrayList<>();

    private List<Item> narratives = new ArrayList<>();

    private List<Item> fulfillmentModes = new ArrayList<>();

    /** 人读的差异摘要（"改了 3 处：新增 1、删除 1、修改 1"），界面直接显示 */
    private List<String> summary = new ArrayList<>();

    /** 一处差异（四类共用）。 */
    @Data
    public static class Item {

        /** ADDED / REMOVED / CHANGED */
        private String changeType;

        /** 中文名：新增 / 删除 / 修改 */
        private String changeTypeLabel;

        /** 编码：termCode / settingKey / sectionCode / mode */
        private String code;

        /** 中文名（条款名 / 字段名 / 段落名 / 履约方式名） */
        private String label;

        private String beforeCode;
        private String beforeText;

        private String afterCode;
        private String afterText;
    }

    public static final String ADDED = "ADDED";
    public static final String REMOVED = "REMOVED";
    public static final String CHANGED = "CHANGED";

    public static String labelOf(String changeType) {
        if (ADDED.equals(changeType)) {
            return "新增";
        }
        if (REMOVED.equals(changeType)) {
            return "删除";
        }
        if (CHANGED.equals(changeType)) {
            return "修改";
        }
        return "变化";
    }
}

package cn.aiedge.export.template;

import cn.aiedge.dev.mapper.DevTemplateMapper;
import cn.aiedge.dev.model.DevTemplate;
import cn.aiedge.export.template.ImportTemplateDefinition.TemplateField;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 导入模板仓储（数据库承载）
 *
 * <p>2026-09-19 落库改造：本类取代原 {@code cn.aiedge.export.template.ImportTemplateRegistry}
 * —— 后者把 4 个导入模板定义硬编码在进程内存（两个 {@code ConcurrentHashMap}）里，导致
 * <b>删除不落库、重启即复原、多实例各读各的内存</b>。现在模板定义真实读写库表
 * <b>{@code dev_template}</b>（迁移 {@code V11.415.0}）。
 *
 * <p><b>读写口径</b>（列映射见迁移文件头注释）：
 * <ul>
 *   <li>只处理 {@code template_kind = 'import'} 的行；同表内 7 行 {@code codegen} 遗留种子
 *       （代码生成路线，见《模板管理开发文档》§5.5）不读也不写，<b>删除操作不会误删它们</b>；</li>
 *   <li>只返回 {@code enabled = true} 的行（{@code enabled} 是该表既有的启用位，
 *       {@code /api/import-templates} 的 13 个端点无启用/停用语义，故不下发该字段）；</li>
 *   <li>{@code content} 列存 <b>JSON</b>，仅承载表列无法表达的部分
 *       （{@code fields} / {@code sampleRowCount} / {@code maxImportRows} / {@code strictValidation}），
 *       模板名称/ID/类型/描述/版本由表列承载，<b>不重复存储</b>；</li>
 *   <li>本表无 {@code tenant_id} 列 → 已登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}；
 *       无 {@code deleted} 列 → 删除是<b>物理删</b>（不使用 {@code @TableLogic}）；</li>
 *   <li>{@code created_by} / {@code updated_by} 按当前登录账号写入（原先内存态无任何审计留痕）。</li>
 * </ul>
 *
 * <p><b>注意</b>：本类不依赖 {@code cn.aiedge.dev.service.DevTemplateService}，
 * 因为 {@code cn.aiedge.dev} 包不在 {@code AiReadyApplication.scanBasePackages} 里（该 Service 未装配），
 * 而 {@code DevTemplateMapper} 由通配的 {@code @MapperScan("cn.aiedge.**.mapper")} 注册，可直接注入。
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportTemplateStore {

    /** 模板类别：导入模板（系统 → 开发工具 → 模板管理，菜单 62402） */
    public static final String KIND_IMPORT = "import";

    /**
     * content 列的 (反)序列化器。
     * 独立实例：不随全局 ObjectMapper 配置（如 Long→String、日期格式）漂移，落库格式保持稳定。
     */
    private static final ObjectMapper CONTENT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final DevTemplateMapper devTemplateMapper;

    // ==================== 读 ====================

    /**
     * 全部导入模板（按 id 升序 → 稳定的展示顺序）
     */
    public List<ImportTemplateDefinition> getAllTemplates() {
        return importRows().stream().map(this::toDefinition).toList();
    }

    /**
     * 按模板ID（= {@code code} 列）取模板；不存在或已停用返回 null
     */
    public ImportTemplateDefinition getTemplate(String templateId) {
        DevTemplate row = findImportRow(templateId);
        return row == null ? null : toDefinition(row);
    }

    /**
     * 按数据类型（= {@code type} 列）取模板；同一数据类型有多行时取 id 最小的一行
     */
    public ImportTemplateDefinition getTemplateByDataType(String dataType) {
        if (dataType == null || dataType.isBlank()) {
            return null;
        }
        return importRows().stream()
                .filter(row -> dataType.equals(row.getType()))
                .findFirst()
                .map(this::toDefinition)
                .orElse(null);
    }

    /**
     * 受支持的数据类型集合（按 id 升序去重）
     */
    public Set<String> getSupportedDataTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (DevTemplate row : importRows()) {
            if (row.getType() != null && !row.getType().isBlank()) {
                types.add(row.getType());
            }
        }
        return types;
    }

    // ==================== 写 ====================

    /**
     * 注册模板（与模板ID已存在时<b>覆盖</b>，保持落库前「后写覆盖」的语义）
     *
     * @throws IllegalArgumentException 模板ID为空、或模板名为空（{@code name} 列 NOT NULL）
     */
    public void register(ImportTemplateDefinition template) {
        requireTemplateId(template);
        if (template.getTemplateName() == null || template.getTemplateName().isBlank()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        DevTemplate existing = findImportRow(template.getTemplateId());
        String operator = currentUser();

        DevTemplate row = new DevTemplate();
        row.setTemplateKind(KIND_IMPORT);
        applyDefinition(row, template);
        row.setEnabled(true);
        row.setUpdateTime(now);
        row.setUpdatedBy(operator);

        if (existing == null) {
            row.setCreateTime(now);
            row.setCreatedBy(operator);
            devTemplateMapper.insert(row);
            template.setCreateTime(now);
            log.info("注册导入模板(新增): templateId={}, dataType={}, id={}",
                    template.getTemplateId(), template.getDataType(), row.getId());
        } else {
            row.setId(existing.getId());
            row.setCreateTime(existing.getCreateTime());
            row.setCreatedBy(existing.getCreatedBy());
            devTemplateMapper.updateById(row);
            template.setCreateTime(existing.getCreateTime());
            log.info("注册导入模板(覆盖): templateId={}, dataType={}, id={}",
                    template.getTemplateId(), template.getDataType(), existing.getId());
        }
        template.setUpdateTime(now);
    }

    /**
     * 更新模板
     *
     * @throws IllegalArgumentException 模板不存在（与落库前一致：控制器捕获后返回 400）
     */
    public void updateTemplate(ImportTemplateDefinition template) {
        requireTemplateId(template);

        DevTemplate existing = findImportRow(template.getTemplateId());
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在: " + template.getTemplateId());
        }

        LocalDateTime now = LocalDateTime.now();
        DevTemplate row = new DevTemplate();
        row.setId(existing.getId());
        row.setTemplateKind(KIND_IMPORT);
        applyDefinition(row, template);
        row.setEnabled(true);
        row.setCreateTime(existing.getCreateTime());
        row.setCreatedBy(existing.getCreatedBy());
        row.setUpdateTime(now);
        row.setUpdatedBy(currentUser());
        devTemplateMapper.updateById(row);

        template.setCreateTime(existing.getCreateTime());
        template.setUpdateTime(now);
        log.info("更新导入模板: templateId={}, id={}", template.getTemplateId(), existing.getId());
    }

    /**
     * 删除模板（物理删；作用域限定 {@code template_kind='import'}，
     * 即使 code 与代码生成种子重名也不会误删后者）
     *
     * @return 是否删除成功（模板不存在返回 false，保持落库前语义）
     */
    public boolean removeTemplate(String templateId) {
        DevTemplate existing = findImportRow(templateId);
        if (existing == null) {
            return false;
        }
        boolean removed = devTemplateMapper.deleteById(existing.getId()) > 0;
        log.info("删除导入模板: templateId={}, id={}, removed={}", templateId, existing.getId(), removed);
        return removed;
    }

    // ==================== 内部：行 ↔ 定义 ====================

    /**
     * 导入模板行（{@code template_kind='import'} 且 {@code enabled=true}），按 id 升序
     */
    private List<DevTemplate> importRows() {
        return devTemplateMapper.selectList(new LambdaQueryWrapper<DevTemplate>()
                .eq(DevTemplate::getTemplateKind, KIND_IMPORT)
                .eq(DevTemplate::getEnabled, true)
                .orderByAsc(DevTemplate::getId));
    }

    /**
     * 按模板ID找导入模板行（不存在返回 null），仅命中 {@code template_kind='import'}
     */
    private DevTemplate findImportRow(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return null;
        }
        return importRows().stream()
                .filter(row -> templateId.equals(row.getCode()))
                .findFirst()
                .orElse(null);
    }

    /** 表列 → 内存对象（含 content 反序列化） */
    private ImportTemplateDefinition toDefinition(DevTemplate row) {
        ImportTemplateDefinition definition = new ImportTemplateDefinition();
        definition.setTemplateId(row.getCode());
        definition.setDataType(row.getType());
        definition.setTemplateName(row.getName());
        definition.setDescription(row.getDescription());
        definition.setVersion(row.getVersion());
        definition.setCreateTime(row.getCreateTime());
        definition.setUpdateTime(row.getUpdateTime());

        TemplateContent content = readContent(row);
        definition.setFields(content.getFields() == null ? new ArrayList<>() : content.getFields());
        definition.setSampleRowCount(content.getSampleRowCount() == null ? 0 : content.getSampleRowCount());
        definition.setMaxImportRows(content.getMaxImportRows() == null ? 0 : content.getMaxImportRows());
        definition.setStrictValidation(Boolean.TRUE.equals(content.getStrictValidation()));
        return definition;
    }

    /** 内存对象 → 表列（含 content 序列化） */
    private void applyDefinition(DevTemplate row, ImportTemplateDefinition definition) {
        row.setCode(definition.getTemplateId());
        row.setName(definition.getTemplateName());
        row.setType(definition.getDataType());
        row.setDescription(definition.getDescription());
        row.setVersion(definition.getVersion());

        TemplateContent content = new TemplateContent();
        content.setFields(definition.getFields());
        content.setSampleRowCount(definition.getSampleRowCount());
        content.setMaxImportRows(definition.getMaxImportRows());
        content.setStrictValidation(definition.isStrictValidation());
        try {
            row.setContent(CONTENT_MAPPER.writeValueAsString(content));
        } catch (Exception e) {
            // 定义无法序列化 = 数据一定写坏 → 宁可 fail-loud，也不落半截 content
            throw new IllegalStateException("模板定义序列化失败: " + definition.getTemplateId() + " - " + e.getMessage(), e);
        }
    }

    /**
     * 反序列化 content 列。
     * <p>解析失败时<b>不抛异常、不伪造字段</b>：记 error 日志后返回空载荷（该模板预览显示 0 个字段），
     * 避免一行脏数据把整页列表打成 500。
     */
    private TemplateContent readContent(DevTemplate row) {
        String content = row.getContent();
        if (content == null || content.isBlank()) {
            log.warn("导入模板 content 为空: templateId={}, id={}", row.getCode(), row.getId());
            return new TemplateContent();
        }
        try {
            return CONTENT_MAPPER.readValue(content, TemplateContent.class);
        } catch (Exception e) {
            log.error("导入模板 content 反序列化失败(该模板字段定义将为空): templateId={}, id={}, content={}",
                    row.getCode(), row.getId(), content, e);
            return new TemplateContent();
        }
    }

    private void requireTemplateId(ImportTemplateDefinition template) {
        if (template == null || template.getTemplateId() == null || template.getTemplateId().isBlank()) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
    }

    /** 当前登录账号（未登录/无会话时不伪造操作人，返回 null） */
    private String currentUser() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * content 列承载的定义部分。
     * <p>刻意<b>不</b>包含 templateId / templateName / dataType / description / version / 时间 ——
     * 这些都由表列承载，重复存储会造成「改列不改 JSON」的双份真相。
     */
    @Data
    public static class TemplateContent {
        /** 字段定义列表（{@link TemplateField} 的 JSON 数组） */
        private List<TemplateField> fields;
        /** 示例数据行数 */
        private Integer sampleRowCount;
        /** 最大导入行数 */
        private Integer maxImportRows;
        /** 是否启用严格校验 */
        private Boolean strictValidation;
    }
}

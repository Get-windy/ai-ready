package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.party.dto.LinkedAccountQuery;
import cn.aiedge.erp.party.dto.LinkedAccountVO;
import cn.aiedge.erp.party.entity.LinkedAccount;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.ILinkedAccountService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 互联账号（资料 → 往来单位 → 互联账号）
 * <p>
 * 对标 ql361 实测口径（2026-09-11）：
 * 单入口只读+解绑列表，查询区「往来单位 + 互联账号」，数据列 3 列
 * （往来单位 / 互联用户名 / 手机号），列配置弹窗「个人配置 / 全局配置」，无页面配置弹窗。
 * 工具栏 = 刷新 / 打印(F8) / 导出 / 更多（更多 = 批量解绑 / 批量删除）。
 * </p>
 * <p>
 * P0 互联账号单一口径（红线）：本页互联账号为全局基础数据，营销/会员/商城互联统一引用，
 * 严禁另建重复互联账号表；平台 + 关联类型字典由 {@link #dict()} 单点提供。
 * </p>
 */
@Tag(name = "互联账号", description = "互联平台账号 ⇆ 往来单位 绑定关系（erp_linked_account）")
@RestController
@RequestMapping("/api/erp/md/linked-account")
@RequiredArgsConstructor
@Slf4j
public class LinkedAccountController {

    private final ILinkedAccountService linkedAccountService;
    private final PartyService partyService;

    /** 导出上限，防止全表导出拖垮服务 */
    private static final int EXPORT_MAX_ROWS = 10000;

    /** 导出列：与列表数据列一致（往来单位 / 互联用户名 / 手机号） */
    private static final String[][] EXPORT_COLUMNS = {
            {"partyName", "往来单位"},
            {"linkedUserName", "互联用户名"},
            {"phone", "手机号"},
    };

    // ==== 分页查询 ====

    @Operation(summary = "分页查询互联账号")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<LinkedAccountVO>>> page(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        LinkedAccountQuery query = new LinkedAccountQuery();
        query.setPartyId(partyId);
        query.setKeyword(keyword);
        query.setPlatform(platform);
        query.setLinkType(linkType);
        query.setStatus(status);

        IPage<LinkedAccount> page = linkedAccountService.pageQuery(query,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);

        PageResult<LinkedAccountVO> result = new PageResult<>(
                linkedAccountService.toVOList(page.getRecords()),
                page.getTotal(), page.getCurrent(), page.getSize());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==== 列表（不分页，供营销/会员/商城互联引用同一口径） ====

    @Operation(summary = "获取互联账号列表(不分页)")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<LinkedAccountVO>>> list(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "200") Integer pageSize) {
        LinkedAccountQuery query = new LinkedAccountQuery();
        query.setPartyId(partyId);
        query.setKeyword(keyword);
        query.setPlatform(platform);
        query.setStatus(status);
        IPage<LinkedAccount> page = linkedAccountService.pageQuery(query, 1,
                pageSize != null && pageSize > 0 ? pageSize : 200);
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.toVOList(page.getRecords())));
    }

    // ==== 字典（平台 / 关联类型，全局唯一） ====

    @Operation(summary = "互联账号字典（互联平台 / 关联类型）")
    @GetMapping("/dict")
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, String>>>>> dict() {
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.dict()));
    }

    // ==== 查询详情 ====

    @Operation(summary = "查询互联账号详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LinkedAccountVO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.toVO(linkedAccountService.getById(id))));
    }

    // ==== 绑定（新增） ====

    @Operation(summary = "绑定互联账号")
    @PostMapping
    public ResponseEntity<ApiResponse<LinkedAccountVO>> create(@RequestBody Map<String, Object> body) {
        LinkedAccount entity = fromBody(body);

        String invalid = validate(entity);
        if (invalid != null) {
            return ResponseEntity.ok(ApiResponse.badRequest(invalid));
        }

        // 唯一约束前置校验：给出可读原因（否则由 DB 唯一索引抛出 400「请求数据不完整或存在冲突」）
        LinkedAccount conflict = linkedAccountService.findConflict(
                entity.getPlatform(), entity.getLinkedUserName(), null);
        if (conflict != null) {
            return ResponseEntity.ok(ApiResponse.badRequest(
                    "该互联平台下互联用户名「" + entity.getLinkedUserName() + "」已绑定到「"
                            + (conflict.getPartyName() == null ? "" : conflict.getPartyName()) + "」"));
        }

        // 往来单位名称为空时从 biz_party 回填，保证列表「往来单位」列不为空
        fillPartySnapshot(entity);
        boolean success;
        try {
            success = linkedAccountService.save(entity);
        } catch (DataIntegrityViolationException e) {
            // 并发下两个请求同时通过前置校验：由唯一索引兜底，转成可读原因
            log.warn("[互联账号] 绑定唯一约束冲突 platform={} user={}", entity.getPlatform(), entity.getLinkedUserName());
            return ResponseEntity.ok(ApiResponse.badRequest(
                    "该互联平台下互联用户名「" + entity.getLinkedUserName() + "」已被绑定"));
        }
        if (!success || entity.getId() == null) {
            return ResponseEntity.ok(ApiResponse.badRequest("绑定失败，请稍后重试"));
        }
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.toVO(entity)));
    }

    // ==== 修改 ====

    @Operation(summary = "修改互联账号")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LinkedAccount exist = linkedAccountService.getById(id);
        if (exist == null) {
            return ResponseEntity.ok(ApiResponse.badRequest("互联账号不存在或已删除"));
        }
        LinkedAccount entity = fromBody(body);
        entity.setId(id);

        String invalid = validate(entity);
        if (invalid != null) {
            return ResponseEntity.ok(ApiResponse.badRequest(invalid));
        }

        LinkedAccount conflict = linkedAccountService.findConflict(
                entity.getPlatform(), entity.getLinkedUserName(), id);
        if (conflict != null) {
            return ResponseEntity.ok(ApiResponse.badRequest(
                    "该互联平台下互联用户名「" + entity.getLinkedUserName() + "」已绑定到「"
                            + (conflict.getPartyName() == null ? "" : conflict.getPartyName()) + "」"));
        }

        fillPartySnapshot(entity);
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.updateById(entity)));
    }

    /**
     * 入参校验：往来单位必须存在；互联用户名必填；字段长度对齐 DB 列宽（避免截断/插入报错）。
     *
     * @return 错误提示；校验通过返回 null
     */
    private String validate(LinkedAccount entity) {
        if (entity.getPartyId() == null) {
            return "请选择往来单位";
        }
        // 往来单位必须真实存在（逻辑删除的取不到），否则会绑定出「无主」互联账号
        if (partyService.getById(entity.getPartyId()) == null) {
            return "往来单位不存在或已被删除，请重新选择";
        }
        if (!StringUtils.hasText(entity.getLinkedUserName())) {
            return "请输入互联用户名";
        }
        if (entity.getLinkedUserName().length() > 128) {
            return "互联用户名不能超过 128 个字符";
        }
        if (!StringUtils.hasText(entity.getPhone())) {
            return "请输入手机号";
        }
        if (entity.getPhone().length() > 32) {
            return "手机号不能超过 32 个字符";
        }
        if (entity.getRemark() != null && entity.getRemark().length() > 500) {
            return "备注不能超过 500 个字符";
        }
        return null;
    }

    // ==== 状态切换（解绑 / 重新绑定） ====

    @Operation(summary = "解绑 / 重新绑定（状态切换）")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(@PathVariable Long id,
                                                             @RequestParam(required = false) Integer status) {
        String raw = status == null ? null : String.valueOf(status);
        return ResponseEntity.ok(ApiResponse.ok(
                linkedAccountService.changeStatus(List.of(id), resolveStatus(raw))));
    }

    @Operation(summary = "批量解绑 / 批量绑定")
    @PutMapping("/batch-status")
    public ResponseEntity<ApiResponse<Boolean>> batchStatus(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        Object raw = body.get("status");
        return ResponseEntity.ok(ApiResponse.ok(
                linkedAccountService.changeStatus(ids, resolveStatus(raw == null ? "0" : raw.toString()))));
    }

    // ==== 删除 ====

    @Operation(summary = "删除互联账号")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.removeById(id)));
    }

    @Operation(summary = "批量删除互联账号")
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Boolean>> batchDelete(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        return ResponseEntity.ok(ApiResponse.ok(linkedAccountService.removeByIds(ids)));
    }

    // ==== 导出 ====

    @Operation(summary = "导出互联账号(真实 Excel 流，与分页查询同一过滤口径)")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "互联账号") String title,
            HttpServletResponse response) throws IOException {

        LinkedAccountQuery query = new LinkedAccountQuery();
        query.setPartyId(partyId);
        query.setKeyword(keyword);
        query.setPlatform(platform);
        query.setLinkType(linkType);
        query.setStatus(status);

        IPage<LinkedAccount> page = linkedAccountService.pageQuery(query, 1, EXPORT_MAX_ROWS);
        List<LinkedAccountVO> voList = linkedAccountService.toVOList(page.getRecords());

        String fileName = title + "_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_COLUMNS[i][1]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (LinkedAccountVO vo : voList) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(readVoProperty(vo, EXPORT_COLUMNS[i][0]));
                }
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String readVoProperty(LinkedAccountVO vo, String property) {
        switch (property) {
            case "partyName": return vo.getPartyName() == null ? "" : vo.getPartyName();
            case "linkedUserName": return vo.getLinkedUserName() == null ? "" : vo.getLinkedUserName();
            case "phone": return vo.getPhone() == null ? "" : vo.getPhone();
            default: return "";
        }
    }

    // ============================================================
    // 内部工具
    // ============================================================

    /** 状态解析：仅 0（已解绑）/ 1（已绑定）两态，非法输入按 1 处理 */
    private int resolveStatus(String raw) {
        if (!StringUtils.hasText(raw)) return 1;
        String v = raw.trim();
        return ("0".equals(v) || "UNBOUND".equalsIgnoreCase(v) || "DISABLED".equalsIgnoreCase(v)) ? 0 : 1;
    }

    /** 往来单位快照回填：前端只传 partyId 时，从 biz_party 带出编号/名称 */
    private void fillPartySnapshot(LinkedAccount entity) {
        if (entity.getPartyId() == null) return;
        if (StringUtils.hasText(entity.getPartyName()) && StringUtils.hasText(entity.getPartyCode())) return;
        Party party = partyService.getById(entity.getPartyId());
        if (party == null) return;
        if (!StringUtils.hasText(entity.getPartyName())) {
            entity.setPartyName(party.getPartyName());
        }
        if (!StringUtils.hasText(entity.getPartyCode())) {
            entity.setPartyCode(party.getPartyCode());
        }
    }

    private LinkedAccount fromBody(Map<String, Object> body) {
        LinkedAccount entity = new LinkedAccount();
        entity.setPartyId(toLong(body.get("partyId")));
        entity.setPartyCode(str(body.get("partyCode")));
        entity.setPartyName(str(body.get("partyName")));
        entity.setPlatform(normalizeCode(body.get("platform"), "OTHER"));
        entity.setLinkType(normalizeCode(body.get("linkType"), "MEMBER"));
        entity.setLinkedUserName(str(body.get("linkedUserName")));
        entity.setPhone(str(body.get("phone")));
        entity.setRemark(str(body.get("remark")));
        // 状态默认「已绑定」（DB 默认值同为 1，这里显式赋值以便创建响应直接带出状态）
        Object status = body.get("status");
        entity.setStatus(resolveStatus(status == null ? "1" : status.toString()));
        return entity;
    }

    /** 标识归一：空值用默认值；中文文案（微信/支付宝/抖音/商城/会员/客户）按字典反查 */
    private String normalizeCode(Object raw, String defaultValue) {
        if (raw == null) return defaultValue;
        String v = raw.toString().trim();
        if (v.isEmpty()) return defaultValue;
        switch (v) {
            case "微信": return "WECHAT";
            case "支付宝": return "ALIPAY";
            case "抖音": return "DOUYIN";
            case "商城": return "MALL";
            case "会员": return "MEMBER";
            case "客户": return "CUSTOMER";
            default: return v.toUpperCase();
        }
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String && StringUtils.hasText((String) v)) {
            try {
                return Long.valueOf(((String) v).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 批量接口入参 ids 兼容 [1,2] 与 ["1","2"] */
    private List<Long> toIdList(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return ids;
        for (Object o : list) {
            Long id = toLong(o);
            if (id != null) ids.add(id);
        }
        return ids;
    }
}

package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.party.dto.MdCustomerVO;
import cn.aiedge.erp.party.dto.PartyContactRow;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.entity.PartyAttachment;
import cn.aiedge.erp.party.entity.PartyContact;
import cn.aiedge.erp.party.entity.PartyFollow;
import cn.aiedge.erp.party.entity.PartyCategory;
import cn.aiedge.erp.party.mapper.PartyContactMapper;
import cn.aiedge.erp.party.mapper.PartyQueryParam;
import cn.aiedge.erp.party.service.IPartyCategoryService;
import cn.aiedge.erp.party.service.PartyFollowService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * MD客户管理——往来单位（POS/ERP成交客户）
 * <p>
 * 使用 biz_party 表，接管原 PartnerController(erp_partner) 全部功能。
 * 字段名向前端映射：partyCode→partnerCode, partyName→partnerName, partyType=1→partnerType='customer'
 * </p>
 */
@Tag(name = "MD客户管理", description = "MD客户CRUD（biz_party），替换旧版 PartnerController(erp_partner)")
@RestController
@RequestMapping("/api/erp/md/customer")
@RequiredArgsConstructor
public class MdCustomerController {

    private final PartyService partyService;
    private final PartyFollowService partyFollowService;
    private final IPartyCategoryService partyCategoryService;
    private final PartyContactMapper partyContactMapper;
    private final cn.aiedge.erp.party.service.PartyAttachmentService partyAttachmentService;
    private final cn.aiedge.erp.party.service.IPartyContactService partyContactService;
    /** 客商合并需要跨表迁移引用 + 物理删除，走原生 SQL */
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ── 类型映射 ──
    private static final Map<String, Integer> PARTNER_TYPE_TO_PARTY = Map.of(
            "customer", 1,
            "supplier", 2,
            "logistics", 3,
            "other", 4
    );

    private static final Map<Integer, String> PARTY_TYPE_TO_PARTNER = Map.of(
            1, "customer",
            2, "supplier",
            3, "logistics",
            4, "other"
    );

    /** 往来单位类型 → 多重身份标识（用于「显示供应商中的客户」合并口径） */
    private static final Map<Integer, String> ROLE_BY_PARTY_TYPE = Map.of(
            1, "CUSTOMER",
            2, "SUPPLIER",
            3, "LOGISTICS",
            4, "OTHER"
    );

    /** 会员卡状态字典 */
    private static final Map<String, String> MEMBER_CARD_STATUS_DESC = Map.of(
            "NORMAL", "正常",
            "STOPPED", "停用",
            "EXPIRED", "已过期"
    );

    // ── 入参解析 ──

    private Integer resolvePartyType(String partnerType) {
        return partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
    }

    private Integer resolveStatus(String status) {
        if ("DISABLED".equalsIgnoreCase(status)) return 0;
        if ("ENABLED".equalsIgnoreCase(status)) return 1;
        return null;
    }

    /**
     * 结款方式：前端传「挂账/现结」文案，库中存 1/0。
     * 非枚举值（如 ALL/全部）一律视为不过滤，避免把文案直接带入整型列导致 SQL 类型错误。
     */
    private Integer resolveSettleType(String settleType) {
        if (!StringUtils.hasText(settleType)) return null;
        if ("挂账".equals(settleType) || "1".equals(settleType)) return 1;
        if ("现结".equals(settleType) || "0".equals(settleType)) return 0;
        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String v = value.replace(' ', 'T');
            return LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v);
        } catch (Exception e) {
            return null;
        }
    }

    // ==== 分页查询 ====

    @Operation(summary = "分页查询MD客户")
    @SaCheckPermission("md:customer:list")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<MdCustomerVO>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String settleType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String lastTradeStart,
            @RequestParam(required = false) String lastTradeEnd,
            @RequestParam(required = false) Boolean showHierarchy,
            @RequestParam(required = false) Boolean showAsCustomer,
            @RequestParam(required = false) Boolean onlyMallAccount,
            @RequestParam(required = false) Boolean onlyNoTrade,
            @RequestParam(required = false) String gradeName,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String promoter,
            @RequestParam(required = false) String customerSource,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Integer partyTypeVal = resolvePartyType(partnerType);

        // 「显示层次结构」：以分类为行展示该类型下的分类层级
        if (Boolean.TRUE.equals(showHierarchy)) {
            return ResponseEntity.ok(ApiResponse.ok(categoryRows(partyTypeVal, keyword, categoryId, pageNum, pageSize)));
        }

        PartyQueryParam query = new PartyQueryParam();
        query.setKeyword(keyword);
        query.setPartyType(partyTypeVal);
        query.setStatus(resolveStatus(status));
        query.setCategoryId(categoryId);
        query.setSettleType(resolveSettleType(settleType));
        query.setRegion(region);
        query.setHandler(handler);
        query.setAddress(address);
        query.setGradeName(gradeName);
        query.setWarehouse(warehouse);
        query.setPromoter(promoter);
        query.setCustomerSource(customerSource);
        // 列头排序（白名单在 XML 内映射；此处只做方向归一）
        if (StringUtils.hasText(sortField)) {
            query.setSortField(sortField);
            query.setSortOrder("desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc");
        }
        query.setOnlyMallAccount(onlyMallAccount);
        query.setOnlyNoTrade(onlyNoTrade);
        query.setCreateTimeStart(parseDate(createTimeStart));
        query.setCreateTimeEnd(parseDate(createTimeEnd));
        query.setLastTradeStart(parseDate(lastTradeStart));
        query.setLastTradeEnd(parseDate(lastTradeEnd));
        // 「显示供应商中的客户」：本类型 ∪ roles 中含该身份的单位
        query.setRole(partyTypeVal != null ? ROLE_BY_PARTY_TYPE.get(partyTypeVal) : null);
        query.setShowAsRole(showAsCustomer);

        IPage<Party> page = partyService.getCustomerPage(query, pageNum, pageSize);

        List<MdCustomerVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        fillPrimaryContacts(voList);
        fillAttachmentCounts(voList);

        PageResult<MdCustomerVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());

        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    // ==== 会员管理子标签 ====

    @Operation(summary = "分页查询客户会员（会员管理子标签）")
    @SaCheckPermission("md:customer:list")
    @GetMapping("/member/page")
    public ResponseEntity<ApiResponse<PageResult<MdCustomerVO>>> memberPage(
            @RequestParam(required = false) String memberKeyword,
            @RequestParam(required = false) String partyKeyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String lastTradeStart,
            @RequestParam(required = false) String lastTradeEnd,
            @RequestParam(required = false) String memberLevel,
            @RequestParam(required = false) String memberCardStatus,
            @RequestParam(required = false) String birthday,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) Integer pointsMin,
            @RequestParam(required = false) Integer pointsMax,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        PartyQueryParam query = new PartyQueryParam();
        query.setPartyType(1);
        query.setMemberKeyword(memberKeyword);
        query.setPartyKeyword(partyKeyword);
        query.setCategoryId(categoryId);
        query.setPhone(phone);
        query.setCustomerId(customerId);
        query.setHandler(handler);
        query.setMemberLevel(memberLevel);
        query.setMemberCardStatus(memberCardStatus);
        query.setBirthday(parseDate(birthday));
        query.setAgeMin(ageMin);
        query.setAgeMax(ageMax);
        query.setPointsMin(pointsMin);
        query.setPointsMax(pointsMax);
        query.setLastTradeStart(parseDate(lastTradeStart));
        query.setLastTradeEnd(parseDate(lastTradeEnd));

        IPage<Party> page = partyService.getMemberPage(query, pageNum, pageSize);
        List<MdCustomerVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        fillPrimaryContacts(voList);

        PageResult<MdCustomerVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    // ==== 全部联系人子标签 ====

    @Operation(summary = "分页查询客户联系人（全部联系人子标签）")
    @SaCheckPermission("md:customer:list")
    @GetMapping("/contact/page")
    public ResponseEntity<ApiResponse<PageResult<PartyContactRow>>> contactPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        PartyQueryParam query = new PartyQueryParam();
        query.setPartyType(1);
        query.setKeyword(keyword);
        query.setCustomerId(customerId);
        query.setHandler(handler);
        query.setAddress(address);
        query.setDeliveryMethod(deliveryMethod);
        query.setRegion(region);

        IPage<PartyContactRow> page = partyService.getContactPage(query, pageNum, pageSize);
        PageResult<PartyContactRow> pageResult = new PageResult<>();
        pageResult.setRecords(page.getRecords());
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    /**
     * 「显示层次结构」视图：以分类树节点为行返回（编号=分类编码、名称=分类名称、新增时间=分类创建时间）。
     * 支持按关键字与父分类下钻过滤，分页口径与列表一致。
     */
    private PageResult<MdCustomerVO> categoryRows(Integer partyTypeVal, String keyword,
                                                  Long parentCategoryId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyCategory::getDeleted, 0);
        if (partyTypeVal != null) {
            wrapper.eq(PartyCategory::getPartyType, partyTypeVal);
        }
        if (parentCategoryId != null) {
            wrapper.eq(PartyCategory::getParentId, parentCategoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PartyCategory::getCategoryCode, keyword)
                    .or().like(PartyCategory::getCategoryName, keyword));
        }
        wrapper.orderByAsc(PartyCategory::getSortOrder).orderByAsc(PartyCategory::getId);

        int num = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;
        List<PartyCategory> all = partyCategoryService.list(wrapper);
        long total = all.size();
        int from = Math.max(0, (num - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<MdCustomerVO> records = new ArrayList<>();
        if (from < to) {
            for (PartyCategory c : all.subList(from, to)) {
                MdCustomerVO vo = new MdCustomerVO();
                vo.setId(c.getId());
                vo.setPartnerCode(c.getCategoryCode());
                vo.setPartnerName(c.getCategoryName());
                vo.setCategoryId(c.getId());
                vo.setCategoryName(c.getCategoryName());
                vo.setPartnerType("category");
                vo.setAddTime(c.getCreateTime());
                vo.setCreateTime(c.getCreateTime());
                vo.setRemark(c.getRemark());
                records.add(vo);
            }
        }

        PageResult<MdCustomerVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum((long) num);
        result.setPageSize((long) size);
        return result;
    }

    // ==== 查询详情 ====

    @Operation(summary = "查询MD客户详情")
    @SaCheckPermission("md:customer:detail")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MdCustomerVO>> getById(@PathVariable Long id) {
        Party party = partyService.getPartyDetailById(id);
        if (party == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        MdCustomerVO vo = toVO(party);
        fillPrimaryContacts(Collections.singletonList(vo));
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    // ==== 搜索(下拉) ====

    @Operation(summary = "搜索MD客户(下拉)")
    @SaCheckPermission("md:customer:view")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MdCustomerVO>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String partnerType) {
        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        List<Party> list = partyService.search(keyword, partyTypeVal);
        List<MdCustomerVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(voList));
    }

    // ==== 获取列表(不分页) ====

    @Operation(summary = "获取MD客户列表(不分页)")
    @SaCheckPermission("md:customer:list")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<MdCustomerVO>>> list(
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "200") Integer pageSize) {
        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        Integer statusVal;
        if ("DISABLED".equalsIgnoreCase(status)) {
            statusVal = 0;
        } else if ("ENABLED".equalsIgnoreCase(status)) {
            statusVal = 1;
        } else {
            statusVal = null;
        }
        List<Party> list = partyService.getPartyList(partyTypeVal, statusVal, pageSize);
        List<MdCustomerVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(voList));
    }

    // ==== 新增 ====

    @Operation(summary = "新增MD客户")
    @SaCheckPermission("md:customer:create")
    @PostMapping
    public ResponseEntity<ApiResponse<MdCustomerVO>> create(@RequestBody Map<String, Object> body) {
        Party party = fromBody(body, null);
        boolean success = partyService.save(party);
        if (!success || party.getId() == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        // 返回含 id 的完整对象，供前端继续挂接网点/联系人等子表
        MdCustomerVO vo = toVO(partyService.getPartyDetailById(party.getId()));
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    // ==== 更新 ====

    @Operation(summary = "更新MD客户")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Party party = fromBody(body, id);
        boolean success = partyService.updateById(party);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 状态更新 ====

    @Operation(summary = "启用/停用MD客户")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Integer statusVal = "DISABLED".equalsIgnoreCase(status) ? 0 : 1;
        boolean success = partyService.updatePartyStatus(id, statusVal);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 删除 ====

    @Operation(summary = "删除MD客户")
    @SaCheckPermission("md:customer:delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        boolean success = partyService.removeById(id);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 下一个编号 ====

    @Operation(summary = "获取下一个编号序号")
    @SaCheckPermission("md:customer:view")
    @GetMapping("/next-seq")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getNextSeq(@RequestParam String prefix) {
        Map<String, Integer> result = new HashMap<>();
        result.put("seq", partyService.getNextSeq(prefix));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==== 跟进记录 ====

    @Operation(summary = "添加跟进记录")
    @SaCheckPermission("md:customer:create")
    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Boolean>> addFollow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PartyFollow follow = new PartyFollow();
        follow.setPartyId(id);
        if (body.get("followType") instanceof Number) {
            follow.setFollowType(((Number) body.get("followType")).intValue());
        }
        follow.setContent(str(body.get("content")));
        if (body.get("result") instanceof Number) {
            follow.setFollowResult(((Number) body.get("result")).intValue());
        }
        boolean success = partyFollowService.save(follow);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 导出 ====

    /** 导出上限，防止全表导出拖垮服务 */
    private static final int EXPORT_MAX_ROWS = 10000;

    /** 导出列（基础资料通用）：列 key（VO 属性名） / 表头。与列表默认列保持一致。 */
    private static final String[][] EXPORT_COLUMNS = {
            {"partnerCode", "编号"},
            {"partnerName", "名称"},
            {"contactPerson", "联系人"},
            {"contactPhone", "联系电话"},
            {"address", "地址"},
            {"remark", "备注"},
    };

    /** 导出列（供应商）：对标《供应商开发文档》列表 12 列。 */
    private static final String[][] EXPORT_COLUMNS_SUPPLIER = {
            {"partnerCode", "供应商编号"},
            {"partnerName", "供应商名称"},
            {"contactPerson", "联系人"},
            {"contactPhone", "联系电话"},
            {"addTime", "新增时间"},
            {"remark", "备注"},
            {"operatingSeries", "经营系列"},
            {"operatingArea", "经营面积"},
            {"taxNumber", "税号"},
            {"bankName", "开户行"},
            {"bankAccount", "银行账号"},
    };

    /** 导出列（其他往来单位）：与列表默认列保持一致。 */
    private static final String[][] EXPORT_COLUMNS_OTHER = {
            {"partnerCode", "单位编号"},
            {"partnerName", "单位名称"},
            {"categoryName", "单位类别"},
            {"contactPerson", "联系人"},
            {"contactPhone", "联系电话"},
            {"address", "联系地址"},
            {"statusDesc", "状态"},
            {"openingReceivable", "期初应收"},
            {"openingPayable", "期初应付"},
            {"addTime", "新增时间"},
            {"remark", "备注"},
    };

    /** 按往来单位类型选择导出列 */
    private String[][] exportColumns(String partnerType) {
        if ("supplier".equalsIgnoreCase(partnerType)) return EXPORT_COLUMNS_SUPPLIER;
        if ("other".equalsIgnoreCase(partnerType)) return EXPORT_COLUMNS_OTHER;
        return EXPORT_COLUMNS;
    }

    @Operation(summary = "导出MD客户(真实 Excel 流，与分页查询同一过滤口径)")
    @SaCheckPermission("md:customer:export")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean showAsCustomer,
            @RequestParam(required = false, defaultValue = "基础资料") String title,
            HttpServletResponse response) throws IOException {

        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        Integer statusVal;
        if ("DISABLED".equalsIgnoreCase(status)) {
            statusVal = 0;
        } else if ("ENABLED".equalsIgnoreCase(status)) {
            statusVal = 1;
        } else {
            statusVal = null;
        }

        IPage<Party> page = partyService.getPartyPageByRole(keyword, partyTypeVal, statusVal, categoryId,
                null, null, null, showAsCustomer, 1, EXPORT_MAX_ROWS);
        List<MdCustomerVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        fillPrimaryContacts(voList);

        String[][] columns = exportColumns(partnerType);
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
            for (int i = 0; i < columns.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(columns[i][1]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (MdCustomerVO vo : voList) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.length; i++) {
                    Object value = readVoProperty(vo, columns[i][0]);
                    Cell cell = row.createCell(i);
                    cell.setCellValue(formatExportValue(value));
                }
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    /** 导出值格式化：日期去 T、空值置空串 */
    private String formatExportValue(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate().toString();
        }
        return value.toString();
    }

    /** 读取 VO 指定属性（导出用，避免为每个字段写一段 switch）。 */
    private Object readVoProperty(MdCustomerVO vo, String property) {
        switch (property) {
            case "partnerCode": return vo.getPartnerCode();
            case "partnerName": return vo.getPartnerName();
            case "categoryName": return vo.getCategoryName();
            case "statusDesc": return vo.getStatusDesc();
            case "contactPerson": return vo.getContactPerson();
            case "contactPhone": return vo.getContactPhone();
            case "phone": return vo.getPhone();
            case "address": return vo.getAddress();
            case "openingReceivable": return vo.getOpeningReceivable();
            case "openingPayable": return vo.getOpeningPayable();
            case "remark": return vo.getRemark();
            case "addTime": return vo.getAddTime();
            case "operatingSeries": return vo.getOperatingSeries();
            case "operatingArea": return vo.getOperatingArea();
            case "taxNumber": return vo.getTaxNumber();
            case "bankName": return vo.getBankName();
            case "bankAccount": return vo.getBankAccount();
            case "createTime": return vo.getCreateTime();
            default: return null;
        }
    }

    // ==== 主联系人（表单「联系人」分区：联系人 / 联系电话 / 联系地址） ====

    @Operation(summary = "保存主联系人")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/{id}/primary-contact")
    public ResponseEntity<ApiResponse<Boolean>> savePrimaryContact(@PathVariable Long id,
                                                                   @RequestBody Map<String, Object> body) {
        String name = str(body.get("contactPerson"));
        String phone = str(body.get("contactPhone"));
        String address = str(body.get("address"));

        LambdaQueryWrapper<PartyContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyContact::getPartyId, id);
        wrapper.eq(PartyContact::getDeleted, 0);
        wrapper.orderByDesc(PartyContact::getIsPrimary).orderByAsc(PartyContact::getId);
        wrapper.last("LIMIT 1");
        PartyContact contact = partyContactMapper.selectOne(wrapper);

        boolean ok;
        if (contact == null) {
            if (!StringUtils.hasText(name) && !StringUtils.hasText(phone) && !StringUtils.hasText(address)) {
                return ResponseEntity.ok(ApiResponse.ok(true));
            }
            contact = new PartyContact();
            contact.setPartyId(id);
            contact.setContactName(name);
            contact.setPhone(phone);
            contact.setMobile(phone);
            contact.setDetailAddress(address);
            contact.setIsPrimary(1);
            contact.setStatus(1);
            ok = partyContactService.save(contact);
        } else {
            contact.setContactName(name);
            contact.setPhone(phone);
            contact.setMobile(phone);
            contact.setDetailAddress(address);
            contact.setIsPrimary(1);
            ok = partyContactService.updateById(contact);
        }

        // 联系电话同步到 biz_party.phone，供采购/应付等单据带出
        if (StringUtils.hasText(phone)) {
            Party party = new Party();
            party.setId(id);
            party.setPhone(phone);
            partyService.updateById(party);
        }
        return ResponseEntity.ok(ApiResponse.ok(ok));
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }

    /** 宽松 Long 解析：兼容 Number / 数字字符串（前端表单可能回传字符串 id） */
    private Long toLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String && StringUtils.hasText((String) v)) {
            try {
                return Long.valueOf((String) v);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 字符串字段写入：请求体未携带该键时保持原值不动，传空串按 null 处理。
     * 用于 province / city / district 这类「前端有才覆盖」的字段。
     */
    private void setIfPresent(java.util.function.Consumer<String> setter, Object value) {
        if (value instanceof String s) {
            setter.accept(s.isEmpty() ? null : s);
        } else if (value != null) {
            setter.accept(String.valueOf(value));
        }
    }

    // ==== 批量操作（列表工具栏「更多」：停用/启用/取消价格跟踪/批量删除/批量搬移） ====

    @Operation(summary = "批量启用/停用")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/batch-status")
    public ResponseEntity<ApiResponse<Boolean>> batchStatus(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        String status = body.get("status") != null ? body.get("status").toString() : "ENABLED";
        int statusVal = "DISABLED".equalsIgnoreCase(status) || "0".equals(status) ? 0 : 1;
        List<Party> updates = ids.stream().map(id -> {
            Party p = new Party();
            p.setId(id);
            p.setStatus(statusVal);
            return p;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(partyService.updateBatchById(updates)));
    }

    @Operation(summary = "批量设置价格跟踪开关")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/batch-price-track")
    public ResponseEntity<ApiResponse<Boolean>> batchPriceTrack(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        Object flag = body.get("priceTrackEnabled");
        int val = (flag instanceof Boolean && (Boolean) flag) || "1".equals(String.valueOf(flag)) ? 1 : 0;
        List<Party> updates = ids.stream().map(id -> {
            Party p = new Party();
            p.setId(id);
            p.setPriceTrackEnabled(val);
            return p;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(partyService.updateBatchById(updates)));
    }

    @Operation(summary = "批量删除")
    @SaCheckPermission("md:customer:delete")
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Boolean>> batchDelete(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        return ResponseEntity.ok(ApiResponse.ok(partyService.removeByIds(ids)));
    }

    @Operation(summary = "批量搬移（改所属分类）")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/batch-move")
    public ResponseEntity<ApiResponse<Boolean>> batchMove(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body.get("ids"));
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        Long targetCategoryId = null;
        Object raw = body.get("categoryId");
        if (raw instanceof Number) {
            targetCategoryId = ((Number) raw).longValue();
        } else if (raw instanceof String && StringUtils.hasText((String) raw)) {
            try { targetCategoryId = Long.valueOf((String) raw); } catch (NumberFormatException ignored) { }
        }
        if (targetCategoryId == null) {
            // 目标分类留空（0 / 空串）= 搬移到「未分类」，显式置 NULL
            UpdateWrapper<Party> clear = new UpdateWrapper<>();
            clear.set("category_id", null).in("id", ids);
            return ResponseEntity.ok(ApiResponse.ok(partyService.update(clear)));
        }
        final Long target = targetCategoryId;
        List<Party> updates = ids.stream().map(id -> {
            Party p = new Party();
            p.setId(id);
            p.setCategoryId(target);
            return p;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(partyService.updateBatchById(updates)));
    }

    /** 批量接口入参 ids 兼容 [1,2,3] 与 ["1","2"] 两种形态 */
    private List<Long> toIdList(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return ids;
        }
        for (Object o : list) {
            if (o instanceof Number) {
                ids.add(((Number) o).longValue());
            } else if (o instanceof String && StringUtils.hasText((String) o)) {
                try { ids.add(Long.valueOf((String) o)); } catch (NumberFormatException ignored) { }
            }
        }
        return ids;
    }

    // ==== 客商合并 ====

    /**
     * 客商合并（列表行「更多 → 客商合并」）。
     * <p>
     * 同一个往来单位既登记为供应商又登记为客户时，主数据会出现两条 <code>biz_party</code>。
     * 本接口把**源**（当前行）**彻底**并入**目标**：
     * ① 迁移源档案在**全部业务表**中的引用（联系人、附件、订单、出入库、收付款、应收应付、发票…）；
     * ② 目标承接身份（roles）/ 分类（目标为空时）/ 期初金额（累加）；
     * ③ 源档案**物理删除**（开发阶段不留历史包袱，不做逻辑删除留痕）。
     * </p>
     */
    @Operation(summary = "客商合并（源彻底并入目标）")
    @SaCheckPermission("md:customer:update")
    @PutMapping("/{id}/merge-partner")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> mergePartner(@PathVariable Long id,
                                                                        @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long targetId = toLong(body.get("targetId"));

        Party source = partyService.getPartyDetailById(id);
        Party target = targetId != null ? partyService.getPartyDetailById(targetId) : null;
        if (source == null || target == null || id.equals(targetId)) {
            result.put("merged", false);
            result.put("reason", source == null ? "源往来单位不存在"
                    : target == null ? "目标往来单位不存在" : "不能合并到自身");
            return ResponseEntity.ok(ApiResponse.ok(result));
        }

        // 1) 迁移源档案的全部业务引用（动态发现引用列，新增业务表自动覆盖，无需维护清单）
        int contacts = (int) partyContactService.count(
                new LambdaQueryWrapper<PartyContact>().eq(PartyContact::getPartyId, id));
        int attachments = (int) partyAttachmentService.count(
                new LambdaQueryWrapper<PartyAttachment>().eq(PartyAttachment::getPartnerId, id));
        int movedRefs = migratePartyReferences(id, targetId, source.getTenantId());

        // 2) 目标主档承接：身份合并 / 分类补位 / 期初累加
        Party targetUpdate = new Party();
        targetUpdate.setId(targetId);
        targetUpdate.setRoles(mergeRoles(target.getRoles(), source.getRoles(), source.getPartyType()));
        if (target.getCategoryId() == null && source.getCategoryId() != null) {
            targetUpdate.setCategoryId(source.getCategoryId());
        }
        if (source.getOpeningPayable() != null) {
            targetUpdate.setOpeningPayable(nvl(source.getOpeningPayable()).add(nvl(target.getOpeningPayable())));
        }
        if (source.getOpeningPrepaid() != null) {
            targetUpdate.setOpeningPrepaid(nvl(source.getOpeningPrepaid()).add(nvl(target.getOpeningPrepaid())));
        }
        partyService.updateById(targetUpdate);

        // 3) 源档案物理删除（绕过 @TableLogic 的逻辑删除）
        jdbcTemplate.update("DELETE FROM biz_party WHERE id = ?", id);

        result.put("merged", true);
        result.put("targetId", String.valueOf(targetId));
        result.put("targetName", target.getPartyName());
        result.put("movedContacts", contacts);
        result.put("movedAttachments", attachments);
        result.put("movedReferences", movedRefs);
        result.put("roles", targetUpdate.getRoles());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 往来单位外键列命名（业务表对 biz_party 的软引用列） */
    private static final List<String> PARTY_REF_COLUMNS =
            List.of("party_id", "partner_id", "customer_id", "supplier_id");

    /** 合法标识符（防止元数据被污染时拼接出非法 SQL） */
    private static final java.util.regex.Pattern IDENT = java.util.regex.Pattern.compile("^[a-z_][a-z0-9_]*$");

    /**
     * 把源档案在所有业务表中的引用迁移到目标档案。
     * <p>
     * 引用列由 <code>information_schema</code> 动态发现（当前 90+ 处，覆盖 ERP/CRM/WMS/DMS/财务/商城），
     * 这样后续新增业务表无需回头维护清单。表若有 <code>tenant_id</code> 则限定租户，避免跨租户误改；
     * 列可能是 <code>bigint</code> 也可能是 <code>varchar</code>（历史字符串存 ID），按列类型传参。
     * </p>
     *
     * @return 迁移的记录总数
     */
    private int migratePartyReferences(Long sourceId, Long targetId, Long tenantId) {
        // 一次查出：引用列类型 + 同表 tenant_id 列类型（库里两种列都可能建成 varchar，必须按实际类型传参）
        List<Map<String, Object>> refs = jdbcTemplate.queryForList(
                "SELECT c.table_name, c.column_name, c.data_type, t.data_type AS tenant_type "
                        + "FROM information_schema.columns c "
                        + "LEFT JOIN information_schema.columns t "
                        + "  ON t.table_schema = 'public' AND t.table_name = c.table_name AND t.column_name = 'tenant_id' "
                        + "WHERE c.table_schema = 'public' AND c.table_name <> 'biz_party' "
                        + "AND c.column_name IN ('party_id','partner_id','customer_id','supplier_id')");
        int moved = 0;
        for (Map<String, Object> ref : refs) {
            String table = String.valueOf(ref.get("table_name"));
            String column = String.valueOf(ref.get("column_name"));
            if (!IDENT.matcher(table).matches() || !IDENT.matcher(column).matches()) {
                continue;
            }
            Object newVal = castFor(ref.get("data_type"), targetId);
            Object oldVal = castFor(ref.get("data_type"), sourceId);

            Object tenantType = ref.get("tenant_type");
            if (tenantType == null) {
                moved += jdbcTemplate.update(
                        "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?", newVal, oldVal);
            } else {
                moved += jdbcTemplate.update(
                        "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ? AND tenant_id = ?",
                        newVal, oldVal, castFor(tenantType, tenantId));
            }
        }
        return moved;
    }

    /** 按列的实际类型把 ID 转成可绑定的参数（varchar 列传字符串，整型列传数字） */
    private Object castFor(Object dataType, Long value) {
        String type = dataType == null ? "" : String.valueOf(dataType).toLowerCase();
        boolean textual = type.contains("char") || type.contains("text");
        return textual ? String.valueOf(value) : value;
    }

    /** 合并身份角色：目标原有 ∪ 源原有 ∪ 源往来单位类型对应身份 */
    private String mergeRoles(String targetRoles, String sourceRoles, Integer sourcePartyType) {
        Set<String> roles = new LinkedHashSet<>();
        if (StringUtils.hasText(targetRoles)) {
            roles.addAll(Arrays.asList(targetRoles.split(",")));
        }
        if (StringUtils.hasText(sourceRoles)) {
            roles.addAll(Arrays.asList(sourceRoles.split(",")));
        }
        if (sourcePartyType != null && ROLE_BY_PARTY_TYPE.containsKey(sourcePartyType)) {
            roles.add(ROLE_BY_PARTY_TYPE.get(sourcePartyType));
        }
        roles.removeIf(r -> !StringUtils.hasText(r));
        return String.join(",", roles);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==== 导入 ====

    @Operation(summary = "导入MD客户(CSV)")
    @SaCheckPermission("md:customer:import")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Boolean>> importCustomers(@RequestBody List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        List<Party> parties = records.stream()
                .map(r -> fromBody(r, null))
                .collect(Collectors.toList());
        boolean success = partyService.saveBatch(parties);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== Excel 导入（基础资料：其他往来单位 / 物流公司 …，真实落库） ====

    /**
     * 基础资料导入表头 → 字段名。
     * <p>
     * 兼容对标模板表头写法：「物流公司编号(必填)」「单位名称*」等带必填标记的列名，
     * 先剥离 * 与括号备注，再按后缀匹配，避免为每种往来单位类型各写一套表头映射。
     * </p>
     */
    private static String baseImportField(String header) {
        if (header == null) return null;
        String h = header.replace("*", "").replace(" ", "")
                .replace("（必填）", "").replace("(必填)", "")
                .replace("（选填）", "").replace("(选填)", "")
                .trim();
        if (h.isEmpty() || "导入结果".equals(h)) return null;
        if (h.endsWith("名称")) return "partnerName";
        if (h.endsWith("编号")) return "partnerCode";
        if (h.endsWith("助记码")) return "mnemonicCode";
        if (h.endsWith("纳税人识别号") || h.endsWith("税号")) return "taxNumber";
        if (h.endsWith("联系人")) return "contactPerson";
        if (h.endsWith("联系电话") || h.endsWith("电话")) return "contactPhone";
        if (h.endsWith("地址")) return "address";
        if (h.endsWith("备注")) return "remark";
        if (h.endsWith("类别") || h.endsWith("分类")) return "categoryName";
        return null;
    }

    @Operation(summary = "下载基础资料导入模板（对标「基本信息导入」向导第 1 步）")
    @SaCheckPermission("md:customer:view")
    @GetMapping("/import-template")
    public void importTemplate(@RequestParam(required = false, defaultValue = "other") String partnerType,
                               HttpServletResponse response) throws IOException {
        String subject = importSubject(partnerType);
        String[] headers = {
                "导入结果",
                subject + "编号(必填)",
                subject + "名称(必填)",
                "联系人",
                "联系电话",
                "公司地址",
                "备注",
        };
        String[] comments = {
                "由系统在导入后回写结果，请勿填写",
                "唯一编号；留空则按「" + importCodePrefix(partnerType) + "」规则自动生成，重号将被拒绝",
                "必填，不可为空",
                "主网点/主联系人姓名",
                "主网点/主联系人电话",
                "主网点/主联系人地址",
                "备注信息",
        };
        String fileName = subject + "导入模板_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(subject + "信息");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            org.apache.poi.xssf.usermodel.XSSFDrawing drawing = ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).createDrawingPatriarch();
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                // 对标模板：首行黑体标题带批注说明录入要求
                org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor =
                        new org.apache.poi.xssf.usermodel.XSSFClientAnchor(0, 0, 0, 0, i, 0, i + 2, 3);
                org.apache.poi.xssf.usermodel.XSSFComment comment = drawing.createCellComment(anchor);
                comment.setString(new org.apache.poi.xssf.usermodel.XSSFRichTextString(comments[i]));
                comment.setAuthor("系统");
                cell.setCellComment(comment);
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    @Operation(summary = "Excel导入基础资料（真实落库；其他往来单位/物流公司共用）")
    @SaCheckPermission("md:customer:create")
    @PostMapping("/import-excel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExcelOther(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "other") String partnerType) throws IOException {
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Integer partyTypeVal = resolvePartyType(partnerType);
        if (partyTypeVal == null) {
            partyTypeVal = 4;
        }
        String role = PARTY_TYPE_TO_PARTNER.getOrDefault(partyTypeVal, "other").toUpperCase();
        String prefix = importCodePrefix(partnerType);

        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headRow = sheet.getRow(sheet.getFirstRowNum());
            if (headRow == null) {
                return ResponseEntity.ok(ApiResponse.ok(importResult(0, 0, List.of("模板缺少表头行"))));
            }
            Map<Integer, String> headerMap = new HashMap<>();
            for (Cell cell : headRow) {
                String field = baseImportField(new DataFormatter().formatCellValue(cell));
                if (field != null) {
                    headerMap.put(cell.getColumnIndex(), field);
                }
            }
            if (!headerMap.containsValue("partnerName")) {
                return ResponseEntity.ok(ApiResponse.ok(importResult(0, 0, List.of("模板缺少「名称」列"))));
            }

            for (int rowIdx = sheet.getFirstRowNum() + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                headerMap.forEach((col, field) -> values.put(field, cellText(row.getCell(col))));
                String name = values.get("partnerName");
                if (!StringUtils.hasText(name)) continue;   // 整行为空则跳过，不计入总数

                total++;
                int rowNo = rowIdx + 1;
                try {
                    String code = values.get("partnerCode");
                    if (StringUtils.hasText(code)) {
                        code = code.trim();
                        if (partyService.checkPartyCodeExists(code)) {
                            errors.add("第" + rowNo + "行：编号「" + code + "」已存在");
                            continue;
                        }
                    } else if ("logistics".equalsIgnoreCase(partnerType)) {
                        // 物流公司：与表单 next-seq 口径一致（对标实测 WuLiu001）
                        code = prefix + String.format("%03d", partyService.getNextSeq(prefix));
                    } else {
                        // 其他类型：前缀-YYYYMMDD-3位序号（序号按日重置）
                        code = prefix + "-" + today + "-" + String.format("%03d", partyService.getNextSeq(prefix));
                    }

                    Party party = new Party();
                    party.setPartyType(partyTypeVal);
                    party.setRoles(role);
                    party.setPartyName(name.trim());
                    party.setPartyCode(code);
                    party.setMnemonicCode(blankToNull(values.get("mnemonicCode")));
                    party.setTaxNumber(blankToNull(values.get("taxNumber")));
                    party.setRemark(blankToNull(values.get("remark")));
                    party.setStatus(1);
                    String categoryName = values.get("categoryName");
                    if (StringUtils.hasText(categoryName) && partyTypeVal == 4) {
                        party.setCategoryId(resolveOtherCategoryId(categoryName.trim()));
                    }
                    if (!partyService.save(party)) {
                        errors.add("第" + rowNo + "行：保存失败");
                        continue;
                    }
                    String contactPerson = values.get("contactPerson");
                    String contactPhone = values.get("contactPhone");
                    String address = values.get("address");
                    if (StringUtils.hasText(contactPerson) || StringUtils.hasText(contactPhone) || StringUtils.hasText(address)) {
                        PartyContact contact = new PartyContact();
                        contact.setPartyId(party.getId());
                        contact.setContactName(blankToNull(contactPerson));
                        contact.setLinkman(blankToNull(contactPerson));
                        contact.setPhone(blankToNull(contactPhone));
                        contact.setMobile(blankToNull(contactPhone));
                        contact.setDetailAddress(blankToNull(address));
                        contact.setIsPrimary(1);
                        contact.setStatus(1);
                        partyContactService.save(contact);
                    }
                    success++;
                } catch (Exception ex) {
                    errors.add("第" + rowNo + "行：" + ex.getMessage());
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(importResult(total, success, errors)));
    }

    /** 导入模板/文件名用的业务主体名 */
    private String importSubject(String partnerType) {
        if ("supplier".equalsIgnoreCase(partnerType)) return "供应商";
        if ("logistics".equalsIgnoreCase(partnerType)) return "物流公司";
        if ("customer".equalsIgnoreCase(partnerType)) return "客户";
        return "其他往来单位";
    }

    /** 导入编号前缀：物流公司对标实测为 WuLiu，其他往来单位沿用 WLDW */
    private String importCodePrefix(String partnerType) {
        if ("logistics".equalsIgnoreCase(partnerType)) return "WuLiu";
        if ("supplier".equalsIgnoreCase(partnerType)) return "GYS";
        if ("customer".equalsIgnoreCase(partnerType)) return "KH";
        return "WLDW";
    }

    private Map<String, Object> importResult(int total, int success, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failure", total - success);
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        return result;
    }

    /** 单元格文本（数字/日期单元格统一格式化，空单元格返回空串） */
    private String cellText(Cell cell) {
        return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 按分类名称取其他往来单位分类 id，不存在则新建（用户导入时可直接使用新类别） */
    private Long resolveOtherCategoryId(String categoryName) {
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyCategory::getPartyType, 4)
                .eq(PartyCategory::getDeleted, 0)
                .eq(PartyCategory::getCategoryName, categoryName)
                .orderByAsc(PartyCategory::getId)
                .last("LIMIT 1");
        PartyCategory exist = partyCategoryService.getOne(wrapper, false);
        if (exist != null) {
            return exist.getId();
        }
        PartyCategory category = new PartyCategory();
        category.setCategoryName(categoryName);
        category.setPartyType(4);
        category.setParentId(0L);
        category.setLevel(1);
        category.setSortOrder(0);
        category.setStatus(1);
        category.setCategoryCode(nextOtherCategoryCode());
        partyCategoryService.save(category);
        return category.getId();
    }

    /** 分类编码：与其他往来单位类型前缀 wldwml + 3 位序号（与 PartyCategoryController 口径一致） */
    private String nextOtherCategoryCode() {
        String prefix = "wldwml";
        LambdaQueryWrapper<PartyCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PartyCategory::getCategoryCode, prefix);
        wrapper.eq(PartyCategory::getDeleted, 0);
        wrapper.orderByDesc(PartyCategory::getCategoryCode);
        wrapper.last("LIMIT 1");
        PartyCategory last = partyCategoryService.getOne(wrapper, false);
        int seq = 1;
        if (last != null && last.getCategoryCode() != null && last.getCategoryCode().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(last.getCategoryCode().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    // ============================================================
    // 内部转换方法
    // ============================================================

    private MdCustomerVO toVO(Party party) {
        if (party == null) return null;
        MdCustomerVO vo = new MdCustomerVO();
        vo.setId(party.getId());
        vo.setPartnerCode(party.getPartyCode());
        vo.setPartnerName(party.getPartyName());
        vo.setPartnerShortName(party.getShortName());
        vo.setMnemonicCode(party.getMnemonicCode());
        vo.setPartnerType(PARTY_TYPE_TO_PARTNER.getOrDefault(party.getPartyType(), "customer"));
        vo.setCategoryId(party.getCategoryId());
        if (party.getCategoryId() != null) {
            PartyCategory category = partyCategoryService.getById(party.getCategoryId());
            vo.setCategoryName(category != null ? category.getCategoryName() : null);
        }
        vo.setGradeName(party.getPartyLevel());
        // settlementType Integer → settleType String
        vo.setSettleType(party.getSettlementType() != null && party.getSettlementType() == 1 ? "挂账" : "现结");
        vo.setPhone(party.getPhone());
        vo.setFax(party.getFax());
        vo.setEmail(party.getEmail());
        vo.setWebsite(party.getWebsite());
        vo.setLegalPerson(party.getLegalPerson());
        vo.setTaxNumber(party.getTaxNumber());
        vo.setBankName(party.getBankName());
        vo.setBankAccount(party.getBankAccount());
        vo.setCompanyFullName(party.getCompanyFullName());
        vo.setBankAddress(party.getBankAddress());
        // 对方地址：优先主网点地址（下述 fillPrimaryContacts 覆盖），兜底纳税人信息地址
        vo.setAddress(party.getAddress());
        // 纳税人信息地址（与「联系地址」区分，后者取主联系人 detail_address）
        vo.setTaxAddress(party.getAddress());
        vo.setCreditLimit(party.getCreditLimit());
        vo.setRemark(party.getRemark());
        // status Integer → String (兼容前端 ENABLED/DISABLED)
        vo.setStatus(party.getStatus() != null && party.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setStatusDesc(party.getStatus() != null && party.getStatus() == 1 ? "已启用" : "已停用");
        vo.setDefaultHandlerId(party.getDefaultHandlerId());
        vo.setDefaultHandlerName(party.getDefaultHandlerName());

        // ── 供应商金标准字段 ──
        vo.setRoles(party.getRoles());
        vo.setAddTime(party.getCreateTime());
        vo.setOpeningPayable(party.getOpeningPayable());
        vo.setOpeningPrepaid(party.getOpeningPrepaid());
        vo.setOperatingSeries(party.getOperatingSeries());
        vo.setOperatingArea(party.getOperatingArea());
        vo.setPaymentTermType(party.getPaymentTermType());
        vo.setPaymentDays(party.getPaymentDays());
        vo.setFixedPaymentDay(party.getFixedPaymentDay());
        vo.setSettlementDay(party.getSettlementDay());
        vo.setPriceTrackEnabled(party.getPriceTrackEnabled());

        // ── 客户金标准字段（V11.151.0） ──
        vo.setWarehouseName(party.getWarehouseName());
        vo.setRegion(party.getRegion());
        vo.setProvince(party.getProvince());
        vo.setCity(party.getCity());
        vo.setDistrict(party.getDistrict());
        vo.setPromoterId(party.getPromoterId());
        vo.setPromoterName(party.getPromoterName());
        vo.setBuyerAccount(party.getBuyerAccount());
        vo.setCustomerOnePass(party.getCustomerOnePass());
        vo.setCustomerSource(party.getCustomerSource());
        vo.setBusinessLicenseExpiry(party.getBusinessLicenseExpiry());
        vo.setLastTradeTime(party.getLastTradeTime());
        vo.setCreditDays(party.getCreditDays());
        vo.setFixedCreditDay(party.getFixedCreditDay());
        vo.setStatementDay(party.getStatementDay());
        vo.setOpeningReceivable(party.getOpeningReceivable());
        vo.setOpeningPreReceived(party.getOpeningPreReceived());

        // ── 会员管理子标签 ──
        vo.setMemberName(party.getMemberName());
        vo.setMemberCardNo(party.getMemberCardNo());
        vo.setMemberLevel(party.getMemberLevel());
        vo.setMemberCardStatus(party.getMemberCardStatus());
        vo.setMemberCardStatusDesc(party.getMemberCardStatus() != null
                ? MEMBER_CARD_STATUS_DESC.getOrDefault(party.getMemberCardStatus(), party.getMemberCardStatus())
                : null);
        vo.setMemberValidStart(party.getMemberValidStart());
        vo.setMemberValidEnd(party.getMemberValidEnd());
        vo.setBirthday(party.getBirthday());
        vo.setPoints(party.getPoints());
        vo.setMemberInitialPoints(party.getMemberInitialPoints());
        vo.setMemberTotalConsume(party.getMemberTotalConsume());
        vo.setMemberIssueTime(party.getMemberIssueTime());

        vo.setCreateTime(party.getCreateTime());
        vo.setUpdateTime(party.getUpdateTime());
        vo.setDeleted(party.getDeleted());
        return vo;
    }

    /** 批量填充附件数量（列表「附件」列） */
    private void fillAttachmentCounts(List<MdCustomerVO> voList) {
        if (voList == null || voList.isEmpty()) return;
        List<Long> ids = voList.stream().map(MdCustomerVO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, Integer> counts = partyAttachmentService.countByPartnerIds(ids);
        for (MdCustomerVO vo : voList) {
            vo.setAttachmentCount(counts.getOrDefault(vo.getId(), 0));
        }
    }

    /**
     * 批量填充主联系人/主网点信息（列表「联系人 / 联系电话 / 对方地址」列）。
     * <p>
     * 一次 IN 查询覆盖整页，避免逐行 N+1；主记录优先取 is_primary=1，否则取 id 最小的一条。
     * 网点场景下 contact_name 存网点名称、linkman 存联系人姓名，这里联系人列取 linkman 兜底 contact_name。
     * </p>
     */
    private void fillPrimaryContacts(List<MdCustomerVO> voList) {
        if (voList == null || voList.isEmpty()) return;
        List<Long> partyIds = voList.stream()
                .map(MdCustomerVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (partyIds.isEmpty()) return;

        LambdaQueryWrapper<PartyContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PartyContact::getPartyId, partyIds);
        wrapper.eq(PartyContact::getDeleted, 0);
        wrapper.orderByAsc(PartyContact::getId);
        List<PartyContact> contacts = partyContactMapper.selectList(wrapper);

        Map<Long, PartyContact> primaryMap = new HashMap<>();
        for (PartyContact c : contacts) {
            PartyContact exist = primaryMap.get(c.getPartyId());
            boolean curPrimary = c.getIsPrimary() != null && c.getIsPrimary() == 1;
            boolean oldPrimary = exist != null && exist.getIsPrimary() != null && exist.getIsPrimary() == 1;
            if (exist == null || (curPrimary && !oldPrimary)) {
                primaryMap.put(c.getPartyId(), c);
            }
        }

        for (MdCustomerVO vo : voList) {
            PartyContact c = primaryMap.get(vo.getId());
            if (c == null) continue;
            vo.setContactPerson(StringUtils.hasText(c.getLinkman()) ? c.getLinkman() : c.getContactName());
            vo.setContactPhone(StringUtils.hasText(c.getPhone()) ? c.getPhone() : c.getMobile());
            // 地址列优先主网点地址，无网点地址时保留纳税人信息地址
            if (StringUtils.hasText(c.getDetailAddress())) {
                vo.setAddress(c.getDetailAddress());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Party fromBody(Map<String, Object> body, Long existingId) {
        Party party = existingId != null ? new Party() : new Party();
        if (existingId != null) {
            party.setId(existingId);
        }
        // 基础字段
        party.setPartyCode(str(body.getOrDefault("partnerCode", body.get("partyCode"))));
        party.setPartyName(str(body.getOrDefault("partnerName", body.get("partyName"))));
        party.setShortName(str(body.getOrDefault("partnerShortName", body.get("shortName"))));
        // 助记码（物流公司等基础资料快速检索）
        String mnemonicCode = str(body.getOrDefault("mnemonicCode", body.get("mnemonic_code")));
        if (mnemonicCode != null) {
            party.setMnemonicCode(mnemonicCode.isEmpty() ? null : mnemonicCode);
        }

        // 类型映射
        String partnerType = str(body.getOrDefault("partnerType", "customer"));
        party.setPartyType(PARTNER_TYPE_TO_PARTY.getOrDefault(partnerType.toLowerCase(), 1));

        // 分类（兼容 Number 与 String 两种形态：前端 tree-select 可能回传字符串 id）
        Long categoryId = toLong(body.get("partnerCategoryId"));
        if (categoryId == null) {
            categoryId = toLong(body.get("categoryId"));
        }
        // 0 视为「未分类」（分类树虚拟根节点）
        if (categoryId != null && categoryId > 0) {
            party.setCategoryId(categoryId);
        }

        // 联系人
        String email = str(body.getOrDefault("contactEmail", body.get("email")));
        if (email != null) party.setEmail(email);

        // 结算方式
        String settleType = str(body.getOrDefault("settleType", "现结"));
        party.setSettlementType("挂账".equals(settleType) ? 1 : 0);

        // 所在地区（省/市/区县，来源 sys_region；前端保存时同步默认联系人所在地区）
        setIfPresent(party::setProvince, body.get("province"));
        setIfPresent(party::setCity, body.get("city"));
        setIfPresent(party::setDistrict, body.get("district"));

        // 级别
        String gradeName = str(body.getOrDefault("gradeName", body.get("partyLevel")));
        if (gradeName != null) party.setPartyLevel(gradeName);

        // 其他字段
        String phone = str(body.getOrDefault("phone", null));
        if (phone != null) party.setPhone(phone);
        String fax = str(body.getOrDefault("fax", null));
        if (fax != null) party.setFax(fax);
        String website = str(body.getOrDefault("website", null));
        if (website != null) party.setWebsite(website);
        String legalPerson = str(body.getOrDefault("legalPerson", null));
        if (legalPerson != null) party.setLegalPerson(legalPerson);
        String taxNumber = str(body.getOrDefault("taxNumber", null));
        if (taxNumber != null) party.setTaxNumber(taxNumber);
        String bankName = str(body.getOrDefault("bankName", null));
        if (bankName != null) party.setBankName(bankName);
        String bankAccount = str(body.getOrDefault("bankAccount", null));
        if (bankAccount != null) party.setBankAccount(bankAccount);
        // 纳税人信息（公司全称 / 地址 / 开户行地址）
        String companyFullName = str(body.getOrDefault("companyFullName", null));
        if (companyFullName != null) party.setCompanyFullName(companyFullName.isEmpty() ? null : companyFullName);
        String address = str(body.getOrDefault("address", null));
        if (address != null) party.setAddress(address.isEmpty() ? null : address);
        String bankAddress = str(body.getOrDefault("bankAddress", null));
        if (bankAddress != null) party.setBankAddress(bankAddress.isEmpty() ? null : bankAddress);
        String remark = str(body.getOrDefault("remark", null));
        if (remark != null) party.setRemark(remark);

        // ── 供应商金标准字段 ──
        // 期初信息
        if (body.get("openingPayable") instanceof Number) {
            party.setOpeningPayable(new BigDecimal(body.get("openingPayable").toString()));
        }
        if (body.get("openingPrepaid") instanceof Number) {
            party.setOpeningPrepaid(new BigDecimal(body.get("openingPrepaid").toString()));
        }
        // 其他信息
        String operatingSeries = str(body.getOrDefault("operatingSeries", null));
        if (operatingSeries != null) {
            party.setOperatingSeries(operatingSeries.isEmpty() ? null : operatingSeries);
        }
        if (body.get("operatingArea") instanceof Number) {
            party.setOperatingArea(new BigDecimal(body.get("operatingArea").toString()));
        }
        // 账期（动态付款期限 / 固定账期 / 结算期）
        String paymentTermType = str(body.getOrDefault("paymentTermType", null));
        if (paymentTermType != null) {
            party.setPaymentTermType(paymentTermType.isEmpty() ? null : paymentTermType);
        }
        if (body.get("paymentDays") instanceof Number) {
            party.setPaymentDays(((Number) body.get("paymentDays")).intValue());
        }
        if (body.get("fixedPaymentDay") instanceof Number) {
            party.setFixedPaymentDay(((Number) body.get("fixedPaymentDay")).intValue());
        }
        if (body.get("settlementDay") instanceof Number) {
            party.setSettlementDay(((Number) body.get("settlementDay")).intValue());
        }
        // 启用价格跟踪（前端传布尔或 0/1）
        Object priceTrack = body.get("priceTrackEnabled");
        if (priceTrack instanceof Boolean) {
            party.setPriceTrackEnabled(((Boolean) priceTrack) ? 1 : 0);
        } else if (priceTrack instanceof Number) {
            party.setPriceTrackEnabled(((Number) priceTrack).intValue());
        }
        // 多重身份（既是供应商又是客户）：显式传入则覆盖，否则按 partyType 推导
        String roles = str(body.get("roles"));
        if (StringUtils.hasText(roles)) {
            party.setRoles(roles);
        } else if (party.getId() == null) {
            party.setRoles(PARTY_TYPE_TO_PARTNER.getOrDefault(party.getPartyType(), "").toUpperCase());
        }

        // ── 客户金标准字段（V11.151.0） ──
        String warehouseName = str(body.getOrDefault("warehouseName", null));
        if (warehouseName != null) party.setWarehouseName(warehouseName.isEmpty() ? null : warehouseName);
        String region = str(body.getOrDefault("region", null));
        if (region != null) party.setRegion(region.isEmpty() ? null : region);
        // 所在地区（省/市/区县）
        setIfPresent(party::setProvince, body.get("province"));
        setIfPresent(party::setCity, body.get("city"));
        setIfPresent(party::setDistrict, body.get("district"));
        Object promoterId = body.get("promoterId");
        if (promoterId instanceof Number) {
            party.setPromoterId(((Number) promoterId).longValue());
        } else if (promoterId instanceof String && !((String) promoterId).isEmpty()) {
            try { party.setPromoterId(Long.valueOf((String) promoterId)); } catch (NumberFormatException ignored) { }
        }
        String promoterName = str(body.getOrDefault("promoterName", null));
        if (promoterName != null) party.setPromoterName(promoterName.isEmpty() ? null : promoterName);
        String buyerAccount = str(body.getOrDefault("buyerAccount", null));
        if (buyerAccount != null) party.setBuyerAccount(buyerAccount.isEmpty() ? null : buyerAccount);
        String customerOnePass = str(body.getOrDefault("customerOnePass", null));
        if (customerOnePass != null) party.setCustomerOnePass(customerOnePass.isEmpty() ? null : customerOnePass);
        String customerSource = str(body.getOrDefault("customerSource", null));
        if (customerSource != null) party.setCustomerSource(customerSource.isEmpty() ? null : customerSource);
        if (body.get("businessLicenseExpiry") != null) {
            party.setBusinessLicenseExpiry(parseDate(String.valueOf(body.get("businessLicenseExpiry"))));
        }
        if (body.get("lastTradeTime") != null) {
            LocalDateTime ltt = parseDateTime(String.valueOf(body.get("lastTradeTime")));
            if (ltt != null) party.setLastTradeTime(ltt);
        }
        if (body.get("creditDays") instanceof Number) {
            party.setCreditDays(((Number) body.get("creditDays")).intValue());
        }
        if (body.get("fixedCreditDay") instanceof Number) {
            party.setFixedCreditDay(((Number) body.get("fixedCreditDay")).intValue());
        }
        if (body.get("statementDay") instanceof Number) {
            party.setStatementDay(((Number) body.get("statementDay")).intValue());
        }
        if (body.get("openingReceivable") instanceof Number) {
            party.setOpeningReceivable(new BigDecimal(body.get("openingReceivable").toString()));
        }
        if (body.get("openingPreReceived") instanceof Number) {
            party.setOpeningPreReceived(new BigDecimal(body.get("openingPreReceived").toString()));
        }

        // ── 会员信息（会员管理子标签 / 表单会员信息分区） ──
        String memberName = str(body.getOrDefault("memberName", null));
        if (memberName != null) party.setMemberName(memberName.isEmpty() ? null : memberName);
        String memberCardNo = str(body.getOrDefault("memberCardNo", null));
        if (memberCardNo != null) party.setMemberCardNo(memberCardNo.isEmpty() ? null : memberCardNo);
        String memberLevel = str(body.getOrDefault("memberLevel", null));
        if (memberLevel != null) party.setMemberLevel(memberLevel.isEmpty() ? null : memberLevel);
        String memberCardStatus = str(body.getOrDefault("memberCardStatus", null));
        if (memberCardStatus != null) party.setMemberCardStatus(memberCardStatus.isEmpty() ? null : memberCardStatus);
        if (body.get("memberValidStart") != null) {
            party.setMemberValidStart(parseDate(String.valueOf(body.get("memberValidStart"))));
        }
        if (body.get("memberValidEnd") != null) {
            party.setMemberValidEnd(parseDate(String.valueOf(body.get("memberValidEnd"))));
        }
        if (body.get("memberIssueTime") != null) {
            LocalDateTime mit = parseDateTime(String.valueOf(body.get("memberIssueTime")));
            if (mit != null) party.setMemberIssueTime(mit);
        }
        if (body.get("birthday") != null) {
            party.setBirthday(parseDate(String.valueOf(body.get("birthday"))));
        }
        if (body.get("points") instanceof Number) {
            party.setPoints(((Number) body.get("points")).intValue());
        }
        if (body.get("memberInitialPoints") instanceof Number) {
            party.setMemberInitialPoints(((Number) body.get("memberInitialPoints")).intValue());
        }
        if (body.get("memberTotalConsume") instanceof Number) {
            party.setMemberTotalConsume(new BigDecimal(body.get("memberTotalConsume").toString()));
        }

        // 额度
        if (body.get("creditLimit") instanceof Number) {
            party.setCreditLimit(new BigDecimal(body.get("creditLimit").toString()));
        }

        // 默认经手人（客户主数据 → 销售单据默认带出）
        Object handlerId = body.get("defaultHandlerId");
        if (handlerId instanceof Number) {
            party.setDefaultHandlerId(((Number) handlerId).longValue());
        } else if (handlerId instanceof String && !((String) handlerId).isEmpty()) {
            try { party.setDefaultHandlerId(Long.valueOf((String) handlerId)); } catch (NumberFormatException ignored) {}
        }
        String handlerName = str(body.getOrDefault("defaultHandlerName", null));
        if (handlerName != null) {
            party.setDefaultHandlerName(handlerName.isEmpty() ? null : handlerName);
        }

        // 状态映射（兼容 Integer 和 String 两种格式）
        Object statusObj = body.get("status");
        if (statusObj instanceof Number) {
            party.setStatus(((Number) statusObj).intValue() == 1 ? 1 : 0);
        } else {
            String statusStr = statusObj != null ? statusObj.toString() : "ENABLED";
            party.setStatus("DISABLED".equalsIgnoreCase(statusStr) ? 0 : 1);
        }

        return party;
    }
}

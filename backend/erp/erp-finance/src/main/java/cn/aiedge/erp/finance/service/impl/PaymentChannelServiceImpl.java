package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.PaymentChannelDTO;
import cn.aiedge.erp.finance.dto.PaymentChannelQuery;
import cn.aiedge.erp.finance.dto.PaymentChannelVO;
import cn.aiedge.erp.finance.mapper.PaymentChannelMapper;
import cn.aiedge.erp.finance.mapper.PaymentMethodMapper;
import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import cn.aiedge.erp.finance.service.PaymentChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 支付渠道Service实现（资料 → 支付管理 → 支付渠道）
 *
 * 定位：支付渠道是「支付方式」的具体落地（同一方式可有多个渠道/商户号），
 *   服务于收付款单的资金路由；全系统唯一渠道主数据表 md_payment_channel，严禁另建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentChannelServiceImpl extends ServiceImpl<PaymentChannelMapper, PaymentChannel>
        implements PaymentChannelService {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 支付方式类型 → 中文文本 */
    private static final Map<String, String> METHOD_TYPE_TEXT = Map.of(
            "CASH", "现金",
            "BANK", "银行转账",
            "WECHAT", "微信",
            "ALIPAY", "支付宝",
            "CHECK", "支票",
            "OTHER", "其他");

    /** 允许服务端排序的列 */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("channelCode", "channelName", "sort", "status", "createTime");

    private final PaymentMethodMapper paymentMethodMapper;

    // ==================== 查询 ====================

    @Override
    public Page<PaymentChannelVO> page(PaymentChannelQuery query) {
        PaymentChannelQuery q = query != null ? query : new PaymentChannelQuery();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<PaymentChannel> entityPage = baseMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<PaymentChannelVO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(toVOList(entityPage.getRecords()));
        return result;
    }

    @Override
    public List<PaymentChannelVO> list(PaymentChannelQuery query) {
        List<PaymentChannel> rows = baseMapper.selectList(buildWrapper(query != null ? query : new PaymentChannelQuery()));
        return toVOList(rows);
    }

    @Override
    public PaymentChannelVO detail(Long id) {
        PaymentChannel entity = baseMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("支付渠道不存在: " + id);
        }
        return toVOList(List.of(entity)).get(0);
    }

    /** 查询条件装配（关键字/编码/支付方式/状态 + 服务端排序） */
    private LambdaQueryWrapper<PaymentChannel> buildWrapper(PaymentChannelQuery q) {
        LambdaQueryWrapper<PaymentChannel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getChannelCode())) {
            wrapper.eq(PaymentChannel::getChannelCode, q.getChannelCode().trim());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(PaymentChannel::getChannelCode, kw)
                    .or().like(PaymentChannel::getChannelName, kw)
                    .or().like(PaymentChannel::getMerchantNo, kw));
        }
        if (q.getMethodId() != null) {
            wrapper.eq(PaymentChannel::getMethodId, q.getMethodId());
        }
        if (q.getStatus() != null) {
            wrapper.eq(PaymentChannel::getStatus, normalizeStatus(q.getStatus()));
        }
        applySort(wrapper, q);
        return wrapper;
    }

    /** 排序：白名单列走指定方向，其余回落「排序号升序 + 创建时间倒序」（对标默认口径） */
    private void applySort(LambdaQueryWrapper<PaymentChannel> wrapper, PaymentChannelQuery q) {
        String field = q.getSortField() == null ? "" : q.getSortField().trim();
        boolean asc = !"desc".equalsIgnoreCase(q.getSortOrder());
        if (SORTABLE_FIELDS.contains(field)) {
            switch (field) {
                case "channelCode" -> wrapper.orderBy(true, asc, PaymentChannel::getChannelCode);
                case "channelName" -> wrapper.orderBy(true, asc, PaymentChannel::getChannelName);
                case "status" -> wrapper.orderBy(true, asc, PaymentChannel::getStatus);
                case "createTime" -> wrapper.orderBy(true, asc, PaymentChannel::getCreateTime);
                case "sort" -> wrapper.orderBy(true, asc, PaymentChannel::getSort);
                default -> wrapper.orderByAsc(PaymentChannel::getSort);
            }
            // 次级排序稳定分页
            wrapper.orderByDesc(PaymentChannel::getCreateTime);
            return;
        }
        wrapper.orderByAsc(PaymentChannel::getSort).orderByDesc(PaymentChannel::getCreateTime);
    }

    // ==================== 写操作 ====================

    @Override
    @Transactional
    public PaymentChannelVO create(PaymentChannelDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        String code = trimToNull(dto.getChannelCode());
        if (code == null) {
            throw BusinessException.badRequest("渠道编码不能为空");
        }
        if (!StringUtils.hasText(dto.getChannelName())) {
            throw BusinessException.badRequest("渠道名称不能为空");
        }
        if (dto.getMethodId() == null) {
            throw BusinessException.badRequest("请选择支付方式");
        }
        assertMethodExists(dto.getMethodId());
        assertCodeAvailable(code, null);

        PaymentChannel entity = new PaymentChannel();
        entity.setChannelCode(code);
        applyToEntity(entity, dto);
        if (entity.getSort() == null || entity.getSort() < 0) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        baseMapper.insert(entity);
        log.info("新增支付渠道: id={}, code={}, name={}", entity.getId(), entity.getChannelCode(), entity.getChannelName());
        return detail(entity.getId());
    }

    @Override
    @Transactional
    public PaymentChannelVO update(Long id, PaymentChannelDTO dto) {
        PaymentChannel entity = baseMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("支付渠道不存在: " + id);
        }
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        // 渠道编码是单据资金路由的稳定标识，创建后不允许变更（对标弹窗「编辑态禁用」）
        String newCode = trimToNull(dto.getChannelCode());
        if (newCode != null && !newCode.equals(entity.getChannelCode())) {
            throw BusinessException.badRequest("渠道编码不允许修改");
        }
        if (dto.getChannelName() != null && !StringUtils.hasText(dto.getChannelName())) {
            throw BusinessException.badRequest("渠道名称不能为空");
        }
        if (dto.getMethodId() != null) {
            assertMethodExists(dto.getMethodId());
        }

        applyToEntity(entity, dto);
        baseMapper.updateById(entity);
        log.info("修改支付渠道: id={}, code={}", id, entity.getChannelCode());
        return detail(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PaymentChannel entity = baseMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("支付渠道不存在: " + id);
        }
        baseMapper.deleteById(id);
        log.info("删除支付渠道: id={}, code={}", id, entity.getChannelCode());
    }

    @Override
    @Transactional
    public PaymentChannelVO updateStatus(Long id, Integer status) {
        PaymentChannel entity = baseMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("支付渠道不存在: " + id);
        }
        entity.setStatus(normalizeStatus(status));
        baseMapper.updateById(entity);
        log.info("{}支付渠道: id={}, code={}", entity.getStatus() == 1 ? "启用" : "停用", id, entity.getChannelCode());
        return detail(id);
    }

    // ==================== 导入 ====================

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要导入的 Excel 文件");
        }
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headRow = sheet.getRow(sheet.getFirstRowNum());
            if (headRow == null) {
                return importResult(0, 0, List.of("模板缺少表头行"));
            }
            Map<Integer, String> headerMap = new LinkedHashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (org.apache.poi.ss.usermodel.Cell cell : headRow) {
                String field = importField(formatter.formatCellValue(cell));
                if (field != null) {
                    headerMap.put(cell.getColumnIndex(), field);
                }
            }
            if (!headerMap.containsValue("channelName")) {
                return importResult(0, 0, List.of("模板缺少「渠道名称」列"));
            }

            for (int rowIdx = sheet.getFirstRowNum() + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                headerMap.forEach((col, field) -> values.put(field, formatter.formatCellValue(row.getCell(col)).trim()));
                if (!StringUtils.hasText(values.get("channelName"))) {
                    continue; // 整行为空则跳过
                }
                total++;
                int rowNo = rowIdx + 1;
                try {
                    MethodRef method = resolveMethod(values.get("methodCode"));
                    if (method == null) {
                        errors.add("第" + rowNo + "行：支付方式「" + values.get("methodCode") + "」不存在");
                        continue;
                    }
                    String code = trimToNull(values.get("channelCode"));
                    if (code == null) {
                        errors.add("第" + rowNo + "行：渠道编码不能为空");
                        continue;
                    }
                    Long dup = baseMapper.selectCount(new LambdaQueryWrapper<PaymentChannel>()
                            .eq(PaymentChannel::getChannelCode, code));
                    if (dup != null && dup > 0) {
                        errors.add("第" + rowNo + "行：渠道编码「" + code + "」已存在");
                        continue;
                    }

                    PaymentChannel entity = new PaymentChannel();
                    entity.setChannelCode(code);
                    entity.setChannelName(values.get("channelName").trim());
                    entity.setMethodId(method.id());
                    entity.setMerchantNo(trimToNull(values.get("merchantNo")));
                    entity.setRemark(trimToNull(values.get("remark")));
                    entity.setSort(parseInt(values.get("sort"), 0));
                    entity.setStatus(1);
                    baseMapper.insert(entity);
                    success++;
                } catch (Exception ex) {
                    log.warn("支付渠道导入第{}行失败", rowNo, ex);
                    errors.add("第" + rowNo + "行：" + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("Excel 解析失败：" + e.getMessage());
        }
        log.info("支付渠道导入完成: total={}, success={}, failure={}", total, success, errors.size());
        return importResult(total, success, errors);
    }

    /** 模板表头 → 字段名 */
    private String importField(String header) {
        if (header == null) {
            return null;
        }
        String h = header.trim();
        if (h.isEmpty() || h.startsWith("导入结果")) {
            return null;
        }
        if (h.contains("渠道编码") || h.contains("编码")) {
            return "channelCode";
        }
        if (h.contains("渠道名称") || h.contains("名称")) {
            return "channelName";
        }
        if (h.contains("支付方式")) {
            return "methodCode";
        }
        if (h.contains("商户号")) {
            return "merchantNo";
        }
        if (h.contains("排序")) {
            return "sort";
        }
        if (h.contains("备注")) {
            return "remark";
        }
        return null;
    }

    private record MethodRef(Long id, String code, String name) {
    }

    /** 支付方式文本（编码或名称）→ 方式引用 */
    private MethodRef resolveMethod(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String t = text.trim();
        PaymentMethod hit = paymentMethodMapper.selectOne(new LambdaQueryWrapper<PaymentMethod>()
                .and(w -> w.eq(PaymentMethod::getMethodCode, t).or().eq(PaymentMethod::getMethodName, t))
                .last("limit 1"));
        return hit == null ? null : new MethodRef(hit.getId(), hit.getMethodCode(), hit.getMethodName());
    }

    // ==================== 内部工具 ====================

    /** 入参 → 实体（仅覆盖非 null 字段，避免修改时清空未传字段） */
    private void applyToEntity(PaymentChannel entity, PaymentChannelDTO dto) {
        if (dto.getChannelName() != null) {
            entity.setChannelName(dto.getChannelName().trim());
        }
        if (dto.getMethodId() != null) {
            entity.setMethodId(dto.getMethodId());
        }
        if (dto.getMerchantNo() != null) {
            entity.setMerchantNo(trimToNull(dto.getMerchantNo()));
        }
        if (dto.getConfigJson() != null) {
            entity.setConfigJson(assertJson(dto.getConfigJson()));
        }
        if (dto.getSort() != null) {
            entity.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(normalizeStatus(dto.getStatus()));
        }
        if (dto.getRemark() != null) {
            entity.setRemark(trimToNull(dto.getRemark()));
        }
    }

    /** 渠道扩展配置必须是合法 JSON（留空视为清空） */
    private String assertJson(String configJson) {
        String text = trimToNull(configJson);
        if (text == null) {
            return null;
        }
        try {
            JSON.readTree(text);
        } catch (Exception e) {
            throw BusinessException.badRequest("渠道配置必须是合法的 JSON 格式");
        }
        return text;
    }

    private void assertMethodExists(Long methodId) {
        PaymentMethod method = paymentMethodMapper.selectById(methodId);
        if (method == null) {
            throw BusinessException.badRequest("支付方式不存在: " + methodId);
        }
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<PaymentChannel>()
                .eq(PaymentChannel::getChannelCode, code)
                .ne(excludeId != null, PaymentChannel::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("渠道编码「" + code + "」已存在");
        }
    }

    /** 状态归一化：仅允许 1-启用 / 0-停用 */
    private int normalizeStatus(Integer status) {
        if (status == null) {
            throw BusinessException.badRequest("状态不能为空");
        }
        if (status != 0 && status != 1) {
            throw BusinessException.badRequest("状态取值非法，仅支持 1-启用 / 0-停用");
        }
        return status;
    }

    /** 批量回填支付方式信息（一次查询，避免 N+1） */
    private List<PaymentChannelVO> toVOList(List<PaymentChannel> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> methodIds = rows.stream()
                .map(PaymentChannel::getMethodId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PaymentMethod> methodMap = methodIds.isEmpty()
                ? Map.of()
                : paymentMethodMapper.selectBatchIds(methodIds).stream()
                        .collect(Collectors.toMap(PaymentMethod::getId, m -> m, (a, b) -> a));

        List<PaymentChannelVO> list = new ArrayList<>(rows.size());
        for (PaymentChannel row : rows) {
            PaymentChannelVO vo = new PaymentChannelVO();
            vo.setId(row.getId());
            vo.setChannelCode(row.getChannelCode());
            vo.setChannelName(row.getChannelName());
            vo.setMethodId(row.getMethodId());
            vo.setMerchantNo(row.getMerchantNo());
            vo.setConfigJson(row.getConfigJson());
            vo.setSort(row.getSort() == null ? 0 : row.getSort());
            vo.setStatus(row.getStatus() == null ? 1 : row.getStatus());
            vo.setStatusText(vo.getStatus() == 1 ? "已启用" : "已停用");
            vo.setRemark(row.getRemark());
            vo.setCreateTime(row.getCreateTime());
            vo.setUpdateTime(row.getUpdateTime());

            PaymentMethod method = row.getMethodId() == null ? null : methodMap.get(row.getMethodId());
            if (method != null) {
                vo.setMethodCode(method.getMethodCode());
                vo.setMethodName(method.getMethodName());
                vo.setMethodType(method.getMethodType());
                vo.setMethodTypeText(METHOD_TYPE_TEXT.getOrDefault(method.getMethodType(), method.getMethodType()));
            } else {
                vo.setMethodName("已删除的支付方式");
            }
            list.add(vo);
        }
        return list;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private Integer parseInt(String value, Integer defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, Object> importResult(int total, int success, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failure", errors.size());
        result.put("errors", errors);
        return result;
    }
}

package cn.aiedge.dms.task.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.dispatch.service.DispatchService;
import cn.aiedge.dms.task.dto.BatchResultVO;
import cn.aiedge.dms.task.dto.DmsTaskBatchDTO;
import cn.aiedge.dms.task.dto.DmsTaskDetailDTO;
import cn.aiedge.dms.task.dto.DmsTaskItemRowDTO;
import cn.aiedge.dms.task.dto.DmsTaskQuery;
import cn.aiedge.dms.task.dto.DmsTaskSaveDTO;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.entity.DmsTaskLog;
import cn.aiedge.dms.task.service.TaskLogService;
import cn.aiedge.dms.task.service.TaskService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 配送单（配送任务）管理控制器
 *
 * <p>页面：配送 → 配送业务 → 配送单[历史]（双入口：主菜单进表单页，标签「历史」进列表页）。</p>
 */
@Slf4j
@Tag(name = "配送单管理")
@RestController
@RequestMapping("/api/dms/task")
@RequiredArgsConstructor
@SaCheckLogin
public class TaskController {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaskService taskService;
    private final TaskLogService taskLogService;
    private final DispatchService dispatchService;

    // ═══════════════════════════════════════════════
    // 号段 / 列表
    // ═══════════════════════════════════════════════

    @Operation(summary = "获取下一个配送单号（PSD-YYYYMMDD-序号）")
    @GetMapping("/next-no")
    public String nextNo() {
        return taskService.generateTaskNo();
    }

    @Operation(summary = "分页查询配送单（按单据视图，多条件）")
    @GetMapping("/page")
    public ApiResponse<IPage<DmsTask>> page(DmsTaskQuery query,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(taskService.pageQuery(query, (int) current, (int) size));
    }

    @Operation(summary = "分页查询配送单（按明细视图）")
    @GetMapping("/page-detail")
    public ApiResponse<IPage<DmsTaskItemRowDTO>> pageDetail(DmsTaskQuery query,
                                                            @RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(taskService.pageDetail(query, (int) current, (int) size));
    }

    @Operation(summary = "导出配送单列表（真实 Excel）")
    @GetMapping("/export")
    public void export(DmsTaskQuery query, HttpServletResponse response) {
        List<DmsTask> list = taskService.exportList(query);
        String fileName = "配送单_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        String[] headers = {
                "指定配送日期", "任务编号", "配送状态", "配送开始时间", "配送结束时间", "司机编号", "司机名称",
                "配送车辆", "送货员", "配送单量", "订金金额", "退货单量", "发货数量", "发货金额",
                "退货数量", "退货金额", "装箱数量", "配送里程(km)", "体积（m³）", "重量（kg）",
                "备注", "打印次数", "制单人", "制单时间"
        };
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配送单");
            CellStyle headStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowIdx = 1;
            for (DmsTask t : list) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(str(t.getDeliveryDate()));
                row.createCell(c++).setCellValue(str(t.getTaskNo()));
                row.createCell(c++).setCellValue(statusText(t.getStatus()));
                row.createCell(c++).setCellValue(str(t.getPickupTime()));
                row.createCell(c++).setCellValue(str(t.getDeliveryTime()));
                setCell(row, c++, t.getRiderId());
                row.createCell(c++).setCellValue(str(t.getRiderName()));
                row.createCell(c++).setCellValue(str(t.getVehicleName()));
                row.createCell(c++).setCellValue(str(t.getDeliverymanName()));
                setCell(row, c++, t.getOrderCount());
                setCell(row, c++, t.getDepositAmount());
                setCell(row, c++, t.getReturnOrderCount());
                setCell(row, c++, t.getTotalQuantity());
                setCell(row, c++, t.getGoodsAmount());
                setCell(row, c++, t.getReturnQuantity());
                setCell(row, c++, t.getReturnAmount());
                setCell(row, c++, t.getBoxQuantity());
                setCell(row, c++, t.getEstimatedDistance());
                setCell(row, c++, t.getTotalVolume());
                setCell(row, c++, t.getTotalWeight());
                row.createCell(c++).setCellValue(str(t.getRemark()));
                setCell(row, c++, t.getPrintCount());
                row.createCell(c++).setCellValue(str(t.getCreatorName()));
                row.createCell(c).setCellValue(str(t.getCreateTime()));
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出配送单失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════
    // 详情 / 保存 / 删除
    // ═══════════════════════════════════════════════

    @Operation(summary = "获取配送单详情（含商品明细）")
    @GetMapping("/{id}")
    public ApiResponse<DmsTaskDetailDTO> getById(@Parameter(description = "配送单ID") @PathVariable Long id) {
        return ApiResponse.ok(taskService.getDetail(id));
    }

    @Operation(summary = "保存配送单（头 + 商品明细；新建/修改）")
    @PostMapping("/save")
    public ApiResponse<DmsTaskDetailDTO> save(@Valid @RequestBody DmsTaskSaveDTO dto) {
        DmsTask task = taskService.save(dto);
        return ApiResponse.ok(taskService.getDetail(task.getId()));
    }

    @Operation(summary = "删除配送单（逻辑删除）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "配送单ID") @PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "审核配送单（待分配 → 已分配）")
    @PostMapping("/{id}/audit")
    public ApiResponse<Void> audit(@Parameter(description = "配送单ID") @PathVariable Long id) {
        taskService.audit(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "反审核配送单（已分配 → 待分配）")
    @PostMapping("/{id}/unaudit")
    public ApiResponse<Void> unaudit(@Parameter(description = "配送单ID") @PathVariable Long id) {
        taskService.unaudit(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "记录打印次数")
    @PostMapping("/{id}/print")
    public ApiResponse<Void> print(@Parameter(description = "配送单ID") @PathVariable Long id) {
        taskService.incrementPrintCount(id);
        return ApiResponse.ok(null);
    }

    // ═══════════════════════════════════════════════
    // 调度任务（配送 → 调度管理 → 调度任务）专属接口
    // ═══════════════════════════════════════════════

    @Operation(summary = "指派配送员（调度任务，等价 /api/dms/dispatch/{id}/assign）")
    @PostMapping("/{id}/assign")
    public ApiResponse<Void> assign(@Parameter(description = "任务ID") @PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        Long riderId = longOf(body, "riderId");
        if (riderId == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("配送员ID不能为空");
        }
        dispatchService.assignRider(id, riderId, textOf(body, "reason"));
        return ApiResponse.ok("指派成功", null);
    }

    @Operation(summary = "改派配送员（调度任务，等价 /api/dms/dispatch/{id}/reassign）")
    @PostMapping("/{id}/reassign")
    public ApiResponse<Void> reassign(@Parameter(description = "任务ID") @PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        Long fromRiderId = longOf(body, "fromRiderId");
        Long toRiderId = longOf(body, "toRiderId");
        if (toRiderId == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("新配送员不能为空");
        }
        DmsTask task = taskService.getById(id);
        Long from = fromRiderId != null ? fromRiderId : task.getRiderId();
        if (from == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("该任务尚未指派配送员，请直接指派");
        }
        dispatchService.reassign(id, from, toRiderId, textOf(body, "reason"));
        return ApiResponse.ok("改派成功", null);
    }

    @Operation(summary = "批量指派（逐单结果反馈）")
    @PostMapping("/batch-assign")
    public ApiResponse<BatchResultVO> batchAssign(@RequestBody DmsTaskBatchDTO dto) {
        return ApiResponse.ok("批量指派完成",
                dispatchService.batchAssign(dto.getTaskIds(), dto.getRiderId(), dto.getReason()));
    }

    @Operation(summary = "批量取消（逐单结果反馈）")
    @PostMapping("/batch-cancel")
    public ApiResponse<BatchResultVO> batchCancel(@RequestBody DmsTaskBatchDTO dto) {
        return ApiResponse.ok("批量取消完成",
                taskService.batchCancel(dto.getTaskIds(), dto.getReason()));
    }

    @Operation(summary = "批量记录打印次数（批量打印配送单后回写）")
    @PostMapping("/batch-print")
    public ApiResponse<Integer> batchPrint(@RequestBody DmsTaskBatchDTO dto) {
        return ApiResponse.ok(taskService.batchIncrementPrintCount(dto.getTaskIds()));
    }

    @Operation(summary = "任务调度审计（指派/改派/取消/异常/超时升级时间线）")
    @GetMapping("/{id}/logs")
    public ApiResponse<List<DmsTaskLog>> logs(@Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.ok(taskLogService.listByTask(id));
    }

    // ═══════════════════════════════════════════════
    // 兼容旧接口（司机端 / 调度侧）
    // ═══════════════════════════════════════════════

    @Operation(summary = "创建配送任务")
    @PostMapping
    public ApiResponse<DmsTask> create(@Parameter(description = "任务信息") @Valid @RequestBody DmsTask task) {
        return ApiResponse.ok(taskService.create(task));
    }

    @Operation(summary = "更新配送任务")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "任务ID") @PathVariable Long id,
                                    @Parameter(description = "任务信息") @RequestBody DmsTask task) {
        task.setId(id);
        taskService.update(task);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "更新任务状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@Parameter(description = "任务ID") @PathVariable Long id,
                                          @Parameter(description = "状态变更信息(fromStatus, toStatus)")
                                          @RequestBody Map<String, Integer> body) {
        Integer fromStatus = body.get("fromStatus");
        Integer toStatus = body.get("toStatus");
        taskService.updateStatus(id, fromStatus, toStatus);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "取消任务（可带取消原因，写调度审计）")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@Parameter(description = "任务ID") @PathVariable Long id,
                                    @RequestBody(required = false) Map<String, Object> body) {
        taskService.cancel(id, textOf(body, "reason"));
        return ApiResponse.ok(null);
    }

    @Operation(summary = "标记任务异常（可带原因，写调度审计）")
    @PostMapping("/{id}/exception")
    public ApiResponse<Void> markException(@Parameter(description = "任务ID") @PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        taskService.markException(id, textOf(body, "reason"));
        return ApiResponse.ok(null);
    }

    @Operation(summary = "按配送员查询任务（司机端）")
    @GetMapping("/rider/{riderId}")
    public ApiResponse<List<DmsTask>> listByRider(@PathVariable Long riderId,
                                                  @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(taskService.getByRiderId(riderId, status));
    }

    @Operation(summary = "查询配送员活跃任务数")
    @GetMapping("/rider/{riderId}/active-count")
    public ApiResponse<Long> activeCount(@PathVariable Long riderId) {
        return ApiResponse.ok(taskService.getActiveTaskCount(riderId));
    }

    // ═══════════════════════════════════════════════
    // 内部工具
    // ═══════════════════════════════════════════════

    /** 取 body 内的整型参数（缺失/非法返回 null） */
    private static Long longOf(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(body.get(key)).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取 body 内的文本参数（缺失/空串返回 null） */
    private static String textOf(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private static void setCell(Row row, int index, BigDecimal value) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
    }

    private static void setCell(Row row, int index, Integer value) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
    }

    private static void setCell(Row row, int index, Long value) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
    }

    private static String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate d) {
            return d.toString();
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DATE_TIME_FMT);
        }
        return String.valueOf(value);
    }

    private static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待分配";
            case 1 -> "已分配";
            case 2 -> "已接单";
            case 3 -> "取货中";
            case 4 -> "配送中";
            case 5 -> "已签收";
            case 6 -> "已完成";
            case 7 -> "已取消";
            case 8 -> "异常";
            default -> String.valueOf(status);
        };
    }
}

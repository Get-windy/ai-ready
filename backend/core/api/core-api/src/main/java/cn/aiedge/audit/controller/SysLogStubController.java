package cn.aiedge.audit.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 系统日志占位 Controller
 * 日志模块尚未完整实现，返回空数据避免前端报错
 */
@Tag(name = "系统日志（占位）")
@RestController
@RequestMapping("/api/log")
public class SysLogStubController {

    @Operation(summary = "日志分页（占位）")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "日志模块列表（占位）")
    @GetMapping("/modules")
    public Result<List<?>> modules() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "操作类型列表（占位）")
    @GetMapping("/operation-types")
    public Result<List<?>> operationTypes() {
        return Result.ok(Collections.emptyList());
    }
}

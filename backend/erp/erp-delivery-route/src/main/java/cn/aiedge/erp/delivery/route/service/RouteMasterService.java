package cn.aiedge.erp.delivery.route.service;

import cn.aiedge.erp.delivery.route.dto.RouteDTO;
import cn.aiedge.erp.delivery.route.dto.RouteQueryDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 线路主数据 Service（资料 → 配送管理 → 线路）
 */
public interface RouteMasterService {

    /** 分页查询（线路编号/线路名称/配送区域 模糊 + 显示状态 + 线路类型） */
    Page<RouteDTO> page(RouteQueryDTO query);

    /** 不分页查询（导出用） */
    List<RouteDTO> list(RouteQueryDTO query);

    /** 详情（含配送区域子表） */
    RouteDTO getById(Long id);

    /** 启用线路下拉（供销售订单/出库单「配送线路」等引用） */
    List<RouteDTO> options();

    /** 生成下一个线路编号（XL001 递增） */
    String nextCode();

    /** 新增 */
    RouteDTO create(RouteDTO dto);

    /** 修改 */
    RouteDTO update(Long id, RouteDTO dto);

    /** 删除（逻辑删除，级联子表） */
    void delete(Long id);

    /** 启用/停用（ENABLED / DISABLED） */
    RouteDTO updateStatus(Long id, String status);

    /** 批量启用/停用 */
    int batchStatus(List<Long> ids, String status);

    /** Excel 导入（真实落库），返回 { total, success, failure, errors } */
    Map<String, Object> importExcel(MultipartFile file);
}

package cn.aiedge.hr.service;

import cn.aiedge.hr.organization.HrPosition;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * HR 岗位（职位）服务
 *
 * <p>注意：本接口管的是 **HR 岗位主数据** `hr_position`，与系统管理域的
 * `sys_position`（系统岗位/职务）是两套主数据，勿混用。</p>
 */
public interface HrPositionService extends IService<HrPosition> {

    /** 岗位分页（条件全部可选） */
    Page<HrPosition> pagePositions(Page<HrPosition> page, Long tenantId,
                                   Long deptId, String positionName,
                                   String positionCode, Integer positionLevel, Integer status);

    List<HrPosition> getByDeptId(Long deptId);

    /** 下拉用全量列表（仅启用） */
    List<HrPosition> listEnabled();

    /** 生成下一个岗位编码（号段 HRPOS） */
    String nextPositionCode();

    Long createPosition(HrPosition position);

    void updatePosition(HrPosition position);

    /** 删除岗位（有在岗员工时拒绝） */
    void deletePosition(Long id);

    /** 重算在岗人数（按 hr_employee 的在职/试用人数回填 current_count） */
    void refreshCurrentCount(Long positionId);

    /** 岗位统计：岗位数 / 编制合计 / 在岗合计 / 超编岗位数 */
    Map<String, Object> statistics(Long deptId);
}

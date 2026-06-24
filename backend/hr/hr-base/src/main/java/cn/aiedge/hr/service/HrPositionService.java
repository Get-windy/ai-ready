package cn.aiedge.hr.service;

import cn.aiedge.hr.organization.HrPosition;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface HrPositionService extends IService<HrPosition> {
    Page<HrPosition> pagePositions(Page<HrPosition> page, Long tenantId, Long deptId, String positionName);
    List<HrPosition> getByDeptId(Long deptId);
}

package cn.aiedge.crm.visit.service;

import cn.aiedge.crm.visit.entity.VisitRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.Map;

public interface VisitRecordService extends IService<VisitRecord> {

    Page<VisitRecord> pageList(Long customerId, Long salesPersonId, Integer result,
                               LocalDate visitDateStart, LocalDate visitDateEnd, int page, int size);

    /** 签到打卡：创建拜访记录，若关联计划则将计划置为已完成 */
    VisitRecord checkIn(VisitRecord record);

    /** 拜访检视：各结果计数 + 按日期分组计数 + 分页列表 */
    Map<String, Object> reviewPage(Integer result, LocalDate visitDateStart, LocalDate visitDateEnd,
                                   int page, int size);

    /** 今日拜访数/本周拜访数/计划覆盖率 */
    Map<String, Object> statsSummary();
}

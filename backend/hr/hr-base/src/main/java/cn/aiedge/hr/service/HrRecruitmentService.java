package cn.aiedge.hr.service;

import cn.aiedge.hr.entity.HrRecruitment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 招聘职位服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface HrRecruitmentService extends IService<HrRecruitment> {

    /**
     * 分页查询招聘职位
     */
    Page<HrRecruitment> pageList(Page<HrRecruitment> page, Long tenantId,
                                  Integer status, String positionName, String channel,
                                  String startDate, String endDate);

    /** 导出用全量查询 */
    List<HrRecruitment> listForExport(Integer status, String positionName, String channel,
                                      String startDate, String endDate);

    /** 新建招聘职位（补齐发布人 / 发布日 / 计数初值） */
    Long createRecruitment(HrRecruitment recruitment);

    /** 修改招聘职位（状态字段不接受本端点直改） */
    void updateRecruitment(HrRecruitment recruitment);

    /** 更新招聘职位状态 */
    void updateStatus(Long id, Integer status);

    /** 招聘统计：招聘中 / 已完成 / 计划人数 / 应聘人数 / 录用人数 */
    Map<String, Object> statistics(Integer status);
}

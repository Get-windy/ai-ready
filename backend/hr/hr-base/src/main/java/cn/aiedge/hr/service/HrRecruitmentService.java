package cn.aiedge.hr.service;

import cn.aiedge.hr.entity.HrRecruitment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 招聘职位服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface HrRecruitmentService extends IService<HrRecruitment> {

    /**
     * 分页查询招聘职位
     *
     * @param page         分页参数
     * @param tenantId     租户ID
     * @param status       状态
     * @param positionName 岗位名称(模糊)
     * @param channel      招聘渠道
     * @param startDate    发布日期起始(yyyy-MM-dd)
     * @param endDate      发布日期截止(yyyy-MM-dd)
     * @return 分页结果
     */
    Page<HrRecruitment> pageList(Page<HrRecruitment> page, Long tenantId,
                                  Integer status, String positionName, String channel,
                                  String startDate, String endDate);

    /**
     * 更新招聘职位状态
     *
     * @param id     职位ID
     * @param status 目标状态
     */
    void updateStatus(Long id, Integer status);
}

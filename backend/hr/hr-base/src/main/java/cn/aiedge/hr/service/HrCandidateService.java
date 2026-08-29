package cn.aiedge.hr.service;

import cn.aiedge.hr.entity.HrCandidate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 候选人服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface HrCandidateService extends IService<HrCandidate> {

    /**
     * 分页查询候选人
     *
     * @param page          分页参数
     * @param tenantId      租户ID
     * @param recruitmentId 关联招聘ID
     * @param name          候选人姓名(模糊)
     * @param status        状态
     * @return 分页结果
     */
    Page<HrCandidate> pageList(Page<HrCandidate> page, Long tenantId,
                                Long recruitmentId, String name, Integer status);

    /**
     * 更新候选人状态(面试流程推进)
     *
     * @param id     候选人ID
     * @param status 目标状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 记录面试评价
     *
     * @param id              候选人ID
     * @param interviewerId   面试官ID
     * @param interviewerName 面试官姓名
     * @param interviewComment 面试评价
     * @param rating          评分(1-5)
     */
    void recordInterview(Long id, Long interviewerId, String interviewerName,
                         String interviewComment, Integer rating);
}

package cn.aiedge.hr.service;

import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.entity.HrCandidate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 候选人服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface HrCandidateService extends IService<HrCandidate> {

    /**
     * 分页查询候选人
     */
    Page<HrCandidate> pageList(Page<HrCandidate> page, Long tenantId,
                                Long recruitmentId, String name, Integer status);

    /** 新建候选人（维护招聘职位的应聘人数） */
    Long createCandidate(HrCandidate candidate);

    /** 修改候选人（状态字段不接受本端点直改） */
    void updateCandidate(HrCandidate candidate);

    /**
     * 更新候选人状态(面试流程推进)
     *
     * @param id     候选人ID
     * @param status 目标状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 记录面试评价
     */
    void recordInterview(Long id, Long interviewerId, String interviewerName,
                         String interviewComment, Integer rating);

    /**
     * 候选人**转入职**：置状态为「已入职」并创建员工档案（补上招聘 → 员工档案的断链）。
     *
     * @param candidateId 候选人ID
     * @param employee    员工档案字段（工号为空时自动编号；未填入职日期时取当天）
     * @return 新建员工ID
     */
    Long hireToEmployee(Long candidateId, HrEmployee employee);

    /** 删除候选人（逻辑删）；**已入职**的候选人不可删（已有员工档案挂在其上） */
    void deleteCandidate(Long candidateId);

    /** 候选人统计：各阶段人数 */
    Map<String, Object> statistics(Long recruitmentId);
}

package cn.aiedge.crm.lead.service;

import cn.aiedge.crm.lead.entity.Lead;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 线索服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface LeadService extends IService<Lead> {

    /**
     * 创建线索
     */
    Long createLead(Lead lead);

    /**
     * 更新线索
     */
    void updateLead(Lead lead);

    /**
     * 删除线索
     */
    void deleteLead(Long leadId);

    /**
     * 分配线索
     */
    void assignLead(Long leadId, Long ownerId);

    /**
     * 批量分配线索
     */
    void batchAssignLeads(List<Long> leadIds, Long ownerId);

    /**
     * 转化为客户
     */
    Long convertToCustomer(Long leadId);

    /**
     * 关闭线索
     */
    void closeLead(Long leadId, String reason);

    /**
     * 分页查询
     */
    Page<Lead> pageLeads(Page<Lead> page, Long tenantId, String name, 
                         Integer status, Long ownerId);

    /**
     * 获取线索详情
     */
    Lead getLeadDetail(Long leadId);

    /**
     * 获取我的线索
     */
    List<Lead> getMyLeads(Long ownerId);

    /**
     * 获取待跟进线索
     */
    List<Lead> getPendingFollowLeads(Long tenantId);
}
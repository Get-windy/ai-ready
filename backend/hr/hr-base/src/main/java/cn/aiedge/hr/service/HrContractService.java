package cn.aiedge.hr.service;

import cn.aiedge.hr.employee.HrContract;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 劳动合同服务
 */
public interface HrContractService extends IService<HrContract> {

    Page<HrContract> pageContracts(Page<HrContract> page, Long tenantId,
                                   Long employeeId, String contractNo,
                                   Integer contractType, Integer status);

    List<HrContract> getByEmployeeId(Long employeeId);

    /** 生成下一个合同编号（号段 HT） */
    String nextContractNo();

    Long createContract(HrContract contract);

    void updateContract(HrContract contract);

    /** 变更合同状态：0-待签 1-生效 2-到期 3-终止 */
    void updateContractStatus(Long id, Integer status);

    void deleteContract(Long id);

    /** 即将到期合同（end_date 在未来 days 天内，且状态为生效） */
    List<HrContract> listExpiring(int days);
}

package cn.aiedge.erp.finance.otherincome.service;

import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeCreateDTO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeItemDetailVO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeVO;
import cn.aiedge.erp.finance.otherincome.entity.OtherIncomeDoc;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

/**
 * 其他收入单Service（金标准）
 */
public interface OtherIncomeDocService extends IService<OtherIncomeDoc> {

    /** 按单据多条件分页 */
    Page<OtherIncomeVO> pageList(String keyword, String docNo, String partnerName, String handlerName,
                                 String departmentName, String creatorName, String bookkeeperName,
                                 Integer status, Integer settleStatus, String incomeSubject,
                                 String summary, String remark, Integer showRed,
                                 LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    /** 按明细分页 */
    Page<OtherIncomeItemDetailVO> pageDetail(String keyword, String docNo, String partnerName, String handlerName,
                                             String departmentName, String creatorName, String bookkeeperName,
                                             Integer status, String incomeSubject,
                                             LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    /** 生成下一个单号（QTSRD-） */
    String nextNo();

    /** 保存（save草稿 / confirm记账）；id 为空新建，否则更新 */
    OtherIncomeVO saveDoc(Long id, OtherIncomeCreateDTO dto, boolean confirm);

    /** 明细（id 拉取详情，含收入项） */
    OtherIncomeVO getDetail(Long id);

    /** 记账（confirm） */
    OtherIncomeVO confirm(Long id);

    /** 删除 */
    void removeDoc(Long id);
}

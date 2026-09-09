package cn.aiedge.erp.finance.cashtransfer.service;

import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferItemVO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferQuery;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferSaveDTO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferVO;
import cn.aiedge.erp.finance.cashtransfer.entity.CashTransfer;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 提存（提存现金转账）Service接口
 */
public interface CashTransferService {

    /** 多条件分页查询(按单据) */
    Page<CashTransfer> pageQuery(CashTransferQuery query);

    /** 多条件分页查询(按明细) */
    Page<CashTransferItemVO> pageDetail(CashTransferQuery query);

    /** 生成下一提存单号：YHZKD-YYYYMMDD-序号 */
    String generateDocNo();

    /** 详情（含转入账户明细） */
    CashTransferVO getDetail(Long id);

    /** 保存草稿（含转入账户明细） */
    CashTransfer saveDraft(CashTransferSaveDTO dto);

    /** 更新草稿（明细整体替换） */
    CashTransfer update(CashTransferSaveDTO dto);

    /** 记账（生成凭证 + 动账户余额 + 记资金流水） */
    CashTransfer confirm(Long id, Long operatorId, String operatorName);

    /** 取消单据（仅草稿） */
    void cancel(Long id);

    /** 删除单据（仅草稿/已取消） */
    void remove(Long id);
}

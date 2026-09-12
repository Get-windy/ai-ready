package cn.aiedge.erp.finance.bankaccount.service;

import cn.aiedge.erp.finance.bankaccount.dto.BankAccountDTO;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountQueryDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 银行账户（资金账户）Service
 *
 * 单一口径复用 finance_account（与《支付账户》同源），不另建银行账户表。
 */
public interface BankAccountService {

    /** 分页查询（支持「显示停用」「显示层次结构」） */
    Page<BankAccountDTO> page(BankAccountQueryDTO query);

    /** 查询全部匹配数据（导出用，不分页） */
    List<BankAccountDTO> list(BankAccountQueryDTO query);

    /** 按ID查询 */
    BankAccountDTO getById(Long id);

    /** 新增银行账户 */
    BankAccountDTO create(BankAccountDTO dto);

    /** 修改银行账户 */
    BankAccountDTO update(Long id, BankAccountDTO dto);

    /** 删除银行账户（逻辑删除） */
    void delete(Long id);

    /** 启用/停用 */
    BankAccountDTO updateStatus(Long id, Integer status);

    /** 生成下一个银行编号（parentId 为空时取顶级编号） */
    String nextCode(Long parentId);

    /** 上级账户下拉（树形顺序，仅启用） */
    List<BankAccountDTO> options();
}

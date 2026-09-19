package cn.aiedge.erp.finance.initial.service;

import cn.aiedge.erp.finance.initial.dto.InitialFinancePartnerDTO;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceQuery;
import cn.aiedge.erp.finance.initial.dto.InitialFinanceSubjectDTO;
import cn.aiedge.erp.finance.initial.vo.CurrentYearVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinancePartnerVO;
import cn.aiedge.erp.finance.initial.vo.InitialFinanceSubjectVO;
import cn.aiedge.erp.finance.initial.vo.PeriodStatusVO;
import cn.aiedge.erp.finance.initial.vo.TrialBalanceVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 财务期初服务（设置 → 数据录入 → 财务期初，菜单 70551）。
 *
 * <p>业务校验失败一律抛 {@link IllegalArgumentException}，由 Controller 统一转成
 * {@code code=400} 的业务错误响应（前端 request 拦截器会弹出 message）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface InitialFinanceService {

    // ── 按科目（银行现金 / 固定资产 / 资产负债） ──

    /** 分页查询「按科目」期初 */
    Page<InitialFinanceSubjectVO> pageSubject(InitialFinanceQuery query);

    /** 导出「按科目」期初（与分页同口径，不分页） */
    List<InitialFinanceSubjectVO> listSubject(InitialFinanceQuery query);

    /** 批量保存「按科目」期初（id 有值则更新，无值则新增；返回受影响行数） */
    int saveSubject(List<InitialFinanceSubjectDTO> rows);

    /** 更新单条「按科目」期初（科目不可改） */
    int updateSubject(InitialFinanceSubjectDTO dto);

    /** 删除单条「按科目」期初（逻辑删除） */
    int deleteSubject(Long id);

    // ── 按往来单位（应付 / 应收） ──

    /** 分页查询「按往来单位」期初 */
    Page<InitialFinancePartnerVO> pagePartner(InitialFinanceQuery query);

    /** 导出「按往来单位」期初 */
    List<InitialFinancePartnerVO> listPartner(InitialFinanceQuery query);

    /** 批量保存「按往来单位」期初 */
    int savePartner(List<InitialFinancePartnerDTO> rows);

    /** 更新单条「按往来单位」期初（往来单位不可改） */
    int updatePartner(InitialFinancePartnerDTO dto);

    /** 删除单条「按往来单位」期初（逻辑删除） */
    int deletePartner(Long id);

    // ── 试算平衡 ──

    /** 期初试算平衡（借贷合计校验 + 存货对平检查；仅提示不阻断保存） */
    TrialBalanceVO trialBalance(Integer periodYear);

    // ── 会计期间联动 ──

    /**
     * 当前会计年（取代前端写死的自然年）。
     * 回退链：开启中且今天落在期间内的年度 → 该租户最大会计年度 → 服务器系统年。
     */
    CurrentYearVO currentYear();

    /**
     * 指定年度的会计期间开启情况（关账保护判定）。
     * {@code periodYear} 传 null 时按 {@link #currentYear()} 的结果判定。
     */
    PeriodStatusVO periodStatus(Integer periodYear);
}

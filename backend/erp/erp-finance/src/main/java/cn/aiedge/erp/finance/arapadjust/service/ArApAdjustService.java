package cn.aiedge.erp.finance.arapadjust.service;

import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustQuery;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustSaveDTO;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustVO;
import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjust;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 应收应付调整 Service接口
 * 不动资金账户的往来余额调整，记账经会计凭证（KJPZ-）。
 */
public interface ArApAdjustService {

    /** 多条件分页查询（单表） */
    Page<ArApAdjust> pageQuery(ArApAdjustQuery query);

    /** 生成下一调整单号：YSKZJ-YYYYMMDD-序号 */
    String generateDocNo();

    /** 详情（含科目明细） */
    ArApAdjustVO getDetail(Long id);

    /** 保存草稿（含科目明细） */
    ArApAdjust saveDraft(ArApAdjustSaveDTO dto);

    /** 更新草稿（明细整体替换） */
    ArApAdjust update(ArApAdjustSaveDTO dto);

    /** 记账（生成凭证 + 调整应收/应付余额） */
    ArApAdjust confirm(Long id, Long operatorId, String operatorName);

    /** 取消单据（仅草稿） */
    void cancel(Long id);

    /** 删除单据（仅草稿/已取消） */
    void remove(Long id);
}

package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.OtherIncomeDoc;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

public interface OtherIncomeDocService extends IService<OtherIncomeDoc> {

    Page<OtherIncomeDoc> pageList(String docNo, String incomeType, Integer status,
                                  LocalDate startDate, LocalDate endDate,
                                  int pageNum, int pageSize);

    OtherIncomeDoc createDoc(OtherIncomeDoc doc);

    void submitDoc(Long id);

    void approveDoc(Long id, Long approvedBy, String note);

    void cancelDoc(Long id, String reason);
}

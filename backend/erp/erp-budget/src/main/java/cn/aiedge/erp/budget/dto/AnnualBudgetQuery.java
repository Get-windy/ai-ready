package cn.aiedge.erp.budget.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 预算编制单多条件查询参数
 */
@Data
public class AnnualBudgetQuery {

    /** 模糊关键词（编号/部门/摘要） */
    private String keyword;

    /** 单据编号 */
    private String budgetNo;

    /** 财政年度 */
    private Integer fiscalYear;

    /** 部门名称（模糊） */
    private String departmentName;

    /** 部门 ID（精确，兼容旧调用方 `views/budget/annual`） */
    private String departmentId;

    /** 制单人 */
    private String creatorName;

    /** 经手人 */
    private String handlerName;

    /** 审核人 */
    private String auditorName;

    /** 单据状态（精确） */
    private String status;

    /** 单据状态集合（逗号分隔，任一匹配） */
    private String statusIn;

    /** 单据备注 */
    private String remark;

    /** 摘要 */
    private String description;

    /** 编制日期起 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateStart;

    /** 编制日期止 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateEnd;

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    public int zeroBasedPage() {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        return p - 1;
    }

    public int safePageSize() {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }
}

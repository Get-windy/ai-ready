package cn.aiedge.common.result;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果封装（可变版本，兼容旧代码）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long pageNum;  // 当前页码
    private long pageSize; // 每页大小
    private long pages;    // 总页数

    /**
     * 默认构造函数
     */
    public PageResult() {
        this.records = new ArrayList<>();
        this.total = 0;
        this.pageNum = 1;
        this.pageSize = 10;
        this.pages = 0;
    }

    /**
     * 带参数构造函数
     */
    public PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records != null ? records : new ArrayList<>();
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
    }

    /**
     * 全参数构造函数
     */
    public PageResult(List<T> records, long total, long pageNum, long pageSize, long pages) {
        this.records = records != null ? records : new ArrayList<>();
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
    }

    // Getter methods
    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public long getPageNum() { return pageNum; }
    public long getPageSize() { return pageSize; }
    public long getPages() { return pages; }
    
    // current 别名（兼容）
    public long getCurrent() { return pageNum; }
    
    // size 别名（兼容）
    public long getSize() { return pageSize; }

    // Setter methods
    public void setRecords(List<T> records) {
        this.records = records != null ? records : new ArrayList<>();
    }

    public void setTotal(long total) {
        this.total = total;
        // 自动计算总页数
        if (this.pageSize > 0) {
            this.pages = (total + this.pageSize - 1) / this.pageSize;
        }
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
        // 自动计算总页数
        if (pageSize > 0) {
            this.pages = (this.total + pageSize - 1) / pageSize;
        }
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    /**
     * 创建空分页
     */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(new ArrayList<>(), 0, pageNum, pageSize, 0);
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNum < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }
    
    /**
     * 工厂方法 - 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
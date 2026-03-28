package cn.aiedge.common.result;

import java.util.List;

/**
 * 分页结果封装
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public record PageResult<T>(
    List<T> records,
    long total,
    long current,
    long size,
    long pages
) {
    /**
     * 从MyBatis-Plus Page转换
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageResult<>(
            page.getRecords(),
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getPages()
        );
    }

    /**
     * 创建空分页
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(List.of(), 0, current, size, 0);
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return current < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return current > 1;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }
}
package com.family.genealogy.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应封装
 */
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
    
    /**
     * 从MyBatis-Plus的Page对象转换为PageResult
     */
    public static <T> PageResult<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPage((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }
}

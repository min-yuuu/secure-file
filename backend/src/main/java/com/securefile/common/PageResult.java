package com.securefile.common;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> implements Serializable {
    private List<T> list;
    private Long total;
    private Long current;
    private Long size;
    private Long pages;

    public static <T> PageResult<T> of(List<T> list, Long total, Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages((total + size - 1) / size);
        return result;
    }
}

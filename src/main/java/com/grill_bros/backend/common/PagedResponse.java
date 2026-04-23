package com.grill_bros.backend.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedResponse<T> {

    private final List<T> content;
    private final int     pageNumber;
    private final int     pageSize;
    private final long    totalElements;
    private final int     totalPages;
    private final boolean first;
    private final boolean last;

    private PagedResponse(Page<T> page) {
        this.content       = page.getContent();
        this.pageNumber    = page.getNumber();
        this.pageSize      = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages    = page.getTotalPages();
        this.first         = page.isFirst();
        this.last          = page.isLast();
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page);
    }
}
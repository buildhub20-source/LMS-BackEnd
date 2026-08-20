package com.lms.common.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Guards incoming paging parameters against abusive page sizes. */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return org.springframework.data.domain.PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }
}

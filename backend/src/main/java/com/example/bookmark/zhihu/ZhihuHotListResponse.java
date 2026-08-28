package com.example.bookmark.zhihu;

import java.time.Instant;
import java.util.List;

public record ZhihuHotListResponse(
        String source,
        String sourceName,
        Instant updatedAt,
        List<ZhihuHotItem> items) {
}

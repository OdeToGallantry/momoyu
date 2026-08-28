package com.example.bookmark.zhihu;

import java.time.Instant;

public record ZhihuHotItem(
        String id,
        String title,
        String url,
        Long heat,
        Instant published,
        String description,
        String image) {
}

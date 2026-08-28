package com.example.bookmark.dish;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页菜单")
public record DishPage(
        List<Dish> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

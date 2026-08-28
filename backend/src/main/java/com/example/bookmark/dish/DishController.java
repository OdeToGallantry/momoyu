package com.example.bookmark.dish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dishes")
@Tag(name = "菜单", description = "菜品入册、抽取、导入")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("/list")
    @Operation(summary = "菜单列表", description = "分页返回。page 从 0 起；size 默认 20，最大 100。")
    public DishPage list(
            @Parameter(description = "搜菜名或标签") @RequestParam(required = false) String q,
            @Parameter(description = "只返回收藏") @RequestParam(defaultValue = "false") boolean favoriteOnly,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return dishService.list(q, favoriteOnly, page, size);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "菜品详情")
    public Dish get(@PathVariable Long id) {
        return dishService.get(id);
    }

    @GetMapping("/random")
    @Operation(summary = "随机抽一道", description = "菜单为空时返回 404。")
    public Dish random(
            @Parameter(description = "只从收藏里抽") @RequestParam(defaultValue = "false") boolean favoriteOnly) {
        return dishService.random(favoriteOnly);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "加一道菜")
    public Dish create(@Valid @RequestBody DishRequest request) {
        return dishService.create(request);
    }

    @GetMapping("/import-template")
    @Operation(summary = "下载 Excel 导入模板")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = dishService.importTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dishes-import.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "从 Excel 导入")
    public DishImportResult importFromExcel(
            @Parameter(description = "xlsx / xls，表头：名称、标签、备注、收藏", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file) {
        return dishService.importFromExcel(file);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "更新菜品")
    public Dish update(@PathVariable Long id, @Valid @RequestBody DishRequest request) {
        return dishService.update(id, request);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除菜品")
    public void delete(@PathVariable Long id) {
        dishService.delete(id);
    }
}

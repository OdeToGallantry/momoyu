package com.example.bookmark.dish;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Excel 导入结果")
public class DishImportResult {

    @Schema(description = "新写入条数")
    private int created;
    @Schema(description = "跳过条数（空行或重名）")
    private int skipped;
    @Schema(description = "本次新建的菜")
    private List<Dish> dishes = new ArrayList<>();

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<Dish> getDishes() {
        return dishes;
    }

    public void setDishes(List<Dish> dishes) {
        this.dishes = dishes;
    }
}

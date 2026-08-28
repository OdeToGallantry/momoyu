package com.example.bookmark.dish;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建 / 更新菜品")
public class DishRequest {

    @NotBlank
    @Schema(description = "菜名", example = "黄焖鸡")
    private String name;

    @Schema(description = "备注", example = "微辣")
    private String note;

    @Schema(description = "标签，逗号分隔", example = "快餐,鸡肉")
    private String tags;

    @Schema(description = "是否常吃 / 收藏", example = "true")
    private Boolean favorite;

    @Min(0)
    @Max(5)
    @Schema(description = "辣 0–5，不传则按名称/标签/备注估算")
    private Integer spice;

    @Min(0)
    @Max(5)
    @Schema(description = "咸 0–5")
    private Integer salt;

    @Min(0)
    @Max(5)
    @Schema(description = "清淡 0–5")
    private Integer light;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public Integer getSpice() {
        return spice;
    }

    public void setSpice(Integer spice) {
        this.spice = spice;
    }

    public Integer getSalt() {
        return salt;
    }

    public void setSalt(Integer salt) {
        this.salt = salt;
    }

    public Integer getLight() {
        return light;
    }

    public void setLight(Integer light) {
        this.light = light;
    }
}

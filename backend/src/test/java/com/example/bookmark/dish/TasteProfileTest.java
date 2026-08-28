package com.example.bookmark.dish;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TasteProfileTest {

    @Test
    void infersSpiceFromMapoTofuStyleText() {
        TasteProfile.Scores scores = TasteProfile.infer("麻婆豆腐", "川菜,麻辣", "重口");
        assertThat(scores.spice()).isEqualTo(5);
        assertThat(scores.salt()).isEqualTo(5);
    }

    @Test
    void infersLightFromSteamedFish() {
        TasteProfile.Scores scores = TasteProfile.infer("清蒸鲈鱼", "粤菜", "清淡少油");
        assertThat(scores.light()).isEqualTo(5);
        assertThat(scores.salt()).isLessThanOrEqualTo(2);
    }
}

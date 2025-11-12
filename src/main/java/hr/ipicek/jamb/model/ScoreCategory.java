package hr.ipicek.jamb.model;

import java.io.Serializable;
import java.util.Map;

public enum ScoreCategory {
    ONES("Jedinice"),
    TWOS("Dvojke"),
    THREES("Trojke"),
    FOURS("Četvorke"),
    FIVES("Petice"),
    SIXES("Šestice"),
    THREE_OF_A_KIND("Tri iste"),
    FOUR_OF_A_KIND("Poker"),
    FULL_HOUSE("Full House"),
    SMALL_STRAIGHT("Mala Skala"),
    LARGE_STRAIGHT("Velika Skala"),
    YAHTZEE("Jamb"),
    CHANCE("Šansa");

    private final String displayName;

    ScoreCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
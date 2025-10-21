package hr.ipicek.jamb.util;
import hr.ipicek.jamb.model.*;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ScoreCalculator {

    public static int calculate(ScoreCategory category, DiceSet diceSet) {
        var values = diceSet.getDiceValues();
        Map<Integer, Long> counts = values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        switch (category) {
            case ONES: return sumOf(values, 1);
            case TWOS: return sumOf(values, 2);
            case THREES: return sumOf(values, 3);
            case FOURS: return sumOf(values, 4);
            case FIVES: return sumOf(values, 5);
            case SIXES: return sumOf(values, 6);

            case THREE_OF_A_KIND:
                return hasOfAKind(counts, 3) ? sum(values) : 0;
            case FOUR_OF_A_KIND:
                return hasOfAKind(counts, 4) ? sum(values) : 0;
            case FULL_HOUSE:
                return counts.containsValue(3L) && counts.containsValue(2L) ? 25 : 0;
            case SMALL_STRAIGHT:
                return hasStraight(values, 4) ? 30 : 0;
            case LARGE_STRAIGHT:
                return hasStraight(values, 5) ? 40 : 0;
            case YAHTZEE:
                return hasOfAKind(counts, 5) ? 50 : 0;
            case CHANCE:
                return sum(values);
            default:
                return 0;
        }
    }

    private static int sumOf(List<Integer> values, int target) {
        return values.stream().filter(v -> v == target).mapToInt(Integer::intValue).sum();
    }

    private static int sum(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    private static boolean hasOfAKind(Map<Integer, Long> counts, int n) {
        return counts.values().stream().anyMatch(c -> c >= n);
    }

    private static boolean hasStraight(List<Integer> values, int length) {
        var set = new TreeSet<>(values);
        int max = 1;
        int streak = 1;
        Integer prev = null;
        for (int v : set) {
            if (prev != null && v == prev + 1) {
                streak++;
                max = Math.max(max, streak);
            } else streak = 1;
            prev = v;
        }
        return max >= length;
    }
}
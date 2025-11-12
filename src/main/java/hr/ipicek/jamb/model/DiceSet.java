package hr.ipicek.jamb.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

public class DiceSet {

    private final ObservableList<Die> dice =
            FXCollections.observableArrayList(List.of(
                    new Die(), new Die(), new Die(), new Die(), new Die()
            ));

    public void roll() {
        dice.stream()
                .filter(d -> !d.isHeld())
                .forEach(Die::roll);
    }

    public void resetHolds() {
        dice.forEach(d -> d.setHeld(false));
    }

    public ObservableList<Die> getDice() {
        return FXCollections.unmodifiableObservableList(dice);
    }

    public List<Integer> getDiceValues() {
        return dice.stream()
                .map(Die::getValue)
                .collect(Collectors.toList());
    }

}
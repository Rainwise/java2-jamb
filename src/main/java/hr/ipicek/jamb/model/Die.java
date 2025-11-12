package hr.ipicek.jamb.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.Random;

public class Die {

    private static final Random RNG = new Random();
    private final IntegerProperty value = new SimpleIntegerProperty(this, "value", 1 + RNG.nextInt(6));
    private final BooleanProperty held = new SimpleBooleanProperty(this, "held", false);

    public void roll() {
        if (!isHeld()) {
            value.set(1 + RNG.nextInt(6));
        }
    }

    public void setValue(int value) {
        this.value.set(value);
    }

    public int getValue() {
        return value.get();
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    public boolean isHeld() {
        return held.get();
    }

    public void setHeld(boolean h) {
        held.set(h);
    }

    public BooleanProperty heldProperty() {
        return held;
    }
}
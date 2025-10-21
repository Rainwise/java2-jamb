package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.model.*;
import hr.ipicek.jamb.util.TableUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {

    @FXML private Label lblPlayer, lblRolls, lblTotal;
    @FXML private Button btnRoll;
    @FXML private ToggleButton die1, die2, die3, die4, die5;
    @FXML private TableView<Row> tblScores;
    @FXML private TableColumn<Row, String> colCat;
    @FXML private TableColumn<Row, Number> colP1;
    @FXML private TableColumn<Row, Number> colP2;

    private GameEngine engine;

    public void init(GameEngine engine) {
        this.engine = engine;

        // Header bindings
        lblPlayer.textProperty().bind(Bindings.createStringBinding(
                () -> engine.getCurrentPlayer().getName(),
                engine.currentPlayerIndexProperty()));
        lblRolls.textProperty().bind(engine.rollCountProperty().asString());
        btnRoll.disableProperty().bind(engine.rollCountProperty().greaterThanOrEqualTo(GameEngine.MAX_ROLLS));

        // Dice bindings
        var dice = engine.getDiceSet().getDice();
        bindDieButton(die1, dice.get(0));
        bindDieButton(die2, dice.get(1));
        bindDieButton(die3, dice.get(2));
        bindDieButton(die4, dice.get(3));
        bindDieButton(die5, dice.get(4));

        btnRoll.setOnAction(e -> engine.roll());

        // Build table
        refreshScoreTable();

        // Auto refresh when player changes
        engine.currentPlayerIndexProperty().addListener((obs, oldVal, newVal) -> refreshScoreTable());
    }

    private void refreshScoreTable() {
        var rows = FXCollections.<Row>observableArrayList();
        var players = engine.getPlayers();
        var p1 = players.get(0);
        var p2 = players.size() > 1 ? players.get(1) : p1;

        for (var c : ScoreCategory.values()) {
            rows.add(new Row(c, p1, p2));
        }
        tblScores.setItems(rows);

        colCat.setCellValueFactory(data -> data.getValue().nameProperty());
        colP1.setCellValueFactory(data -> data.getValue().score1Property());
        colP2.setCellValueFactory(data -> data.getValue().score2Property());

        // Set up player columns with click + style logic
        TableUtils.setupPlayerColumn(tblScores, colP1, engine, 0);
        TableUtils.setupPlayerColumn(tblScores, colP2, engine, 1);

        // Update total label dynamically
        var sheet = engine.getCurrentPlayer().getSheet();
        lblTotal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(sheet.total()),
                engine.currentPlayerIndexProperty(),
                sheet.scoreProperty(ScoreCategory.ONES),
                sheet.scoreProperty(ScoreCategory.TWOS),
                sheet.scoreProperty(ScoreCategory.THREES),
                sheet.scoreProperty(ScoreCategory.FOURS),
                sheet.scoreProperty(ScoreCategory.FIVES),
                sheet.scoreProperty(ScoreCategory.SIXES),
                sheet.scoreProperty(ScoreCategory.THREE_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FOUR_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FULL_HOUSE),
                sheet.scoreProperty(ScoreCategory.SMALL_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.LARGE_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.YAHTZEE),
                sheet.scoreProperty(ScoreCategory.CHANCE)
        ));
    }

    private void bindDieButton(ToggleButton btn, Die die) {
        btn.textProperty().bind(die.valueProperty().asString());
        btn.selectedProperty().bindBidirectional(die.heldProperty());
    }

    // Row class for the TableView
    public static class Row {
        private final ScoreCategory category;
        private final StringProperty name;
        private final IntegerProperty score1;
        private final BooleanProperty filled1;
        private final IntegerProperty score2;
        private final BooleanProperty filled2;

        public Row(ScoreCategory c, Player p1, Player p2) {
            this.category = c;
            this.name = new SimpleStringProperty(c.displayName());
            this.score1 = p1.getSheet().scoreProperty(c);
            this.filled1 = p1.getSheet().filledProperty(c);
            this.score2 = p2.getSheet().scoreProperty(c);
            this.filled2 = p2.getSheet().filledProperty(c);
        }

        public ScoreCategory category() { return category; }
        public StringProperty nameProperty() { return name; }
        public IntegerProperty score1Property() { return score1; }
        public IntegerProperty score2Property() { return score2; }
        public BooleanProperty filled1Property() { return filled1; }
        public BooleanProperty filled2Property() { return filled2; }
    }
}
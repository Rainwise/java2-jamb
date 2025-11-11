package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.model.*;
import hr.ipicek.jamb.util.TableUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MainController {

    @FXML private Label lblPlayer;
    @FXML private Label lblRolls;
    @FXML private Label lblTotal;
    @FXML private Button btnRoll;
    @FXML private TableView<Row> tblScores;
    @FXML private TableColumn<Row, String> colCat;
    @FXML private TableColumn<Row, Number> colP1;
    @FXML private TableColumn<Row, Number> colP2;
    @FXML private ImageView imgDie1;
    @FXML private ImageView imgDie2;
    @FXML private ImageView imgDie3;
    @FXML private ImageView imgDie4;
    @FXML private ImageView imgDie5;

    private GameEngine engine;

    public void init(GameEngine engine) {
        this.engine = engine;

        lblPlayer.textProperty().bind(Bindings.createStringBinding(
                () -> engine.getCurrentPlayer().getName(),
                engine.currentPlayerIndexProperty()));
        lblRolls.textProperty().bind(engine.rollCountProperty().asString());
        btnRoll.disableProperty().bind(engine.rollCountProperty().greaterThanOrEqualTo(GameEngine.MAX_ROLLS));

        btnRoll.setOnAction(e -> engine.roll());

        var dice = engine.getDiceSet().getDice();
        var diceImages = engine.getDiceImagePaths();

        bindDie(imgDie1, dice.get(0), diceImages.get(0));
        bindDie(imgDie2, dice.get(1), diceImages.get(1));
        bindDie(imgDie3, dice.get(2), diceImages.get(2));
        bindDie(imgDie4, dice.get(3), diceImages.get(3));
        bindDie(imgDie5, dice.get(4), diceImages.get(4));

        refreshScoreTable();

        engine.currentPlayerIndexProperty().addListener((obs, oldVal, newVal) -> refreshScoreTable());
    }

    private void bindDie(ImageView img, Die die, StringProperty imagePath) {
        img.setFitWidth(72);
        img.setFitHeight(72);
        img.setPreserveRatio(true);
        imagePath.addListener((obs, old, newPath) -> updateImage(img, newPath));
        updateImage(img, imagePath.get());

        die.heldProperty().addListener((obs, oldVal, held) ->
                img.setOpacity(Boolean.TRUE.equals(held) ? 0.6 : 1.0)
        );
        img.setOpacity(die.heldProperty().get() ? 0.6 : 1.0);

        img.setOnMouseClicked(e -> die.heldProperty().set(!die.heldProperty().get()));

        img.setOnMouseEntered(e -> {
            img.setScaleX(1.08);
            img.setScaleY(1.08);
        });
        img.setOnMouseExited(e -> {
            img.setScaleX(1.0);
            img.setScaleY(1.0);
        });
    }

    //Test
    private void updateImage(ImageView img, String path) {
        var url = getClass().getResource(path);
        if (url != null) {
            img.setImage(new Image(url.toExternalForm()));
        }
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

        TableUtils.setupPlayerColumn(tblScores, colP1, engine, 0);
        TableUtils.setupPlayerColumn(tblScores, colP2, engine, 1);

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
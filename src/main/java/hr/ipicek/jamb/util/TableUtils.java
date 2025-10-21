package hr.ipicek.jamb.util;

import hr.ipicek.jamb.controller.MainController.Row;
import hr.ipicek.jamb.model.GameEngine;
import hr.ipicek.jamb.model.ScoreCategory;
import javafx.scene.control.*;

public class TableUtils {

    public static void setupPlayerColumn(TableView<Row> table, TableColumn<Row, Number> column,
                                         GameEngine engine, int playerIndex) {

        column.setCellFactory(col -> {
            TableCell<Row, Number> cell = new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    var row = getTableRow().getItem();
                    boolean isFilled = (playerIndex == 0)
                            ? row.filled1Property().get()
                            : row.filled2Property().get();

                    setText(item == null ? "" : item.toString());
                    setStyle(isFilled ? "-fx-background-color: lightgray; -fx-text-fill: gray;" : "");

                    if (engine.getCurrentPlayerIndex() == playerIndex) {
                        setStyle(getStyle() + "-fx-border-color: #0078d4; -fx-border-width: 0 0 2 0;");
                    }
                }
            };

            cell.setOnMouseClicked(e -> {
                if (cell.isEmpty()) return;

                Row row = cell.getTableRow().getItem();
                if (engine.getCurrentPlayerIndex() != playerIndex) return;

                boolean filled = (playerIndex == 0)
                        ? row.filled1Property().get()
                        : row.filled2Property().get();

                if (!filled) {
                    ScoreCategory category = row.category();
                    DialogUtils.showScoreConfirmation(engine, category);
                }
            });

            return cell;
        });
    }
}
package hr.ipicek.jamb.util;

import hr.ipicek.jamb.controller.MainController.Row;
import hr.ipicek.jamb.model.GameEngine;
import javafx.scene.control.*;

public class TableUtils {

    private TableUtils() {}

    public static void setupPlayerColumn(TableColumn<Row, Number> column,
                                         GameEngine engine, int playerIndex) {

        column.setCellFactory(col -> createPlayerCell(engine, playerIndex));
    }

    private static TableCell<Row, Number> createPlayerCell(GameEngine engine, int playerIndex) {
        TableCell<Row, Number> cell = new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                updateCellStyleAndText(this, item, empty, engine, playerIndex);
            }
        };

        cell.setOnMouseClicked(e -> handleCellClick(cell, engine, playerIndex));
        return cell;
    }

    private static void updateCellStyleAndText(TableCell<Row, Number> cell, Number item, boolean empty,
                                               GameEngine engine, int playerIndex) {

        if (empty || cell.getTableRow() == null || cell.getTableRow().getItem() == null) {
            cell.setText(null);
            cell.setStyle("");
            return;
        }

        Row row = cell.getTableRow().getItem();
        boolean isFilled = getIsFilled(row, playerIndex);

        cell.setText(item == null ? "" : item.toString());
        applyCellStyle(cell, isFilled, engine, playerIndex);
    }

    private static void applyCellStyle(TableCell<Row, Number> cell, boolean isFilled,
                                       GameEngine engine, int playerIndex) {

        StringBuilder style = new StringBuilder();
        if (isFilled) {
            style.append("-fx-background-color: lightgray; -fx-text-fill: gray;");
        }
        if (engine.getCurrentPlayerIndex() == playerIndex) {
            style.append("-fx-border-color: #0078d4; -fx-border-width: 0 0 2 0;");
        }
        cell.setStyle(style.toString());
    }

    private static void handleCellClick(TableCell<Row, Number> cell, GameEngine engine, int playerIndex) {
        if (cell.isEmpty()) return;

        Row row = cell.getTableRow().getItem();
        if (engine.getCurrentPlayerIndex() != playerIndex) return;

        boolean filled = getIsFilled(row, playerIndex);
        if (!filled) {
            DialogUtils.showScoreConfirmation(engine, row.category());
        }
    }

    private static boolean getIsFilled(Row row, int playerIndex) {
        return playerIndex == 0 ? row.filled1Property().get() : row.filled2Property().get();
    }
}
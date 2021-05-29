package server.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class ServerController {

    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonStart, buttonStop;
    @FXML
    private TextField pathToFolder;

    public Button getButtonStart() {
        return buttonStart;
    }

    public void setButtonStart(Button buttonStart) {
        this.buttonStart = buttonStart;
    }

    public Button getButtonStop() {
        return buttonStop;
    }

    public TextField getPathToFolder() {
        return pathToFolder;
    }

    public String getPathToFolderString() {
        return pathToFolder.getText();
    }

    public void setTextArea(String textArea) {
        this.textArea.setText(textArea);
    }

    public void initialize() {
        textArea.setEditable(false);
        // при первом запуске приложения окно с просьбой ввести путь к папке, где будут файлы клиентов
        textArea.setText("Введите путь к папке сервера");
    }
}

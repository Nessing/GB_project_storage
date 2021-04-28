package client.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientControllerSynchronization {

    @FXML
    private Label labelUserName;
    @FXML
    private TextField path;
    @FXML
    private Button buttonBack, buttonCheck, buttonLoadFromServer, buttonLoadToServer;
    @FXML
    private TextArea message;

    public void setMessage(String message) {
        this.message.setText(message);
    }

    public void setLabelUserName(String labelUserName) {
        this.labelUserName.setText(labelUserName);
    }

    public String getPath() {
        return path.getText();
    }

    public Button getButtonBack() {
        return buttonBack;
    }

    public Button getButtonCheck() {
        return buttonCheck;
    }

    public Button getButtonLoadFromServer() {
        return buttonLoadFromServer;
    }

    public Button getButtonLoadToServer() {
        return buttonLoadToServer;
    }

    public void back() {
        message.setText("");
        path.setText("");
    }

    public void synchronization() {

    }
    public void initialize() {
        message.setEditable(false);
    }
}

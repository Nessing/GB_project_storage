package client.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class ClientControllerAuthorization {
    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonAuthorization, buttonRegistration;
    @FXML
    private TextField login, password;
    @FXML
    private ClientGui authorization;

    private boolean isActivated = false;

    public Button getButtonAuthorization() {
        return buttonAuthorization;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public ClientControllerAuthorization() {
    }

    public void setTextArea(String textArea) {
        this.textArea.setText(textArea);
    }

    public String getLogin() {
        return login.getText();
    }

    public void setLogin(String login) {
        this.login.setText(login);
    }

    public String getPassword() {
        return password.getText();
    }

    public void setPassword(String password) {
        this.password.setText(password);
    }

    // проверка логина и пароля
    public void setTextAreaErrorAuthorization() {

    }

    // регистраци и проверка на уже существующий логин в БД
    public void setTextAreaErrorRegistration() {
        textArea.setText("Такой пользователь уже существует");
    }

    public void initialize() {
        textArea.setEditable(false);
        // при первом запуске приложения окно с просьбой ввести логин и пароль
        textArea.setText("Введите логин и пароль для входа или регистрации");
    }
}

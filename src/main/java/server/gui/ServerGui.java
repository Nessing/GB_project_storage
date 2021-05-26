package server.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import server.Server;
import server.ServerEcho;

import java.io.InputStream;

public class ServerGui extends Application {
    private Server server = new Server(2000);
    private ServerEcho serverEcho;
    private ServerController serverController;

    public static void main(String[] args) {
        ServerGui.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Stage serverGui = new Stage();
        serverGui.initOwner(stage);
        serverGui.initModality(Modality.APPLICATION_MODAL);
        serverController = (ServerController) init(stage, "fxml/ScreenServer.fxml", "Server");
        if (serverEcho == null) {
            serverEcho = server.getServerEcho();
        }
        // кнопка "Stop" неактивна
        serverController.getButtonStop().setDisable(true);
        // при нажатии на кнопку "Start" запускается сервер и береться путь к папке сервера
        serverController.getButtonStart().setOnMouseClicked(buttonStartServer -> {
            if (serverController.getPathToFolderString().equals("")) serverController.setTextArea("Неверно указан путь");
            else {
                serverEcho.setPathToFolder(serverController.getPathToFolderString());
                new Thread(() -> {
                    try {
                        server.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                // после нажатия, кнопка "Stop" становиться активной, а "Stop" нет
                serverController.getButtonStart().setDisable(true);
                serverController.getButtonStop().setDisable(false);
                // неактивна строка ввода пути
                serverController.getPathToFolder().setDisable(true);
                serverController.getButtonStart().setText("Started");
                serverController.setTextArea("Сервер запущен");
            }
        });
        // при нажатии на кнопку "Stop", сервер завершается и кнопка "Stop" становиться неактивной,
        // а "Start" активной
        serverController.getButtonStop().setOnMouseClicked(buttonStopServer -> {
            serverController.getButtonStop().setDisable(true);
            serverController.getButtonStart().setDisable(false);
            // активна строка ввода пути
            serverController.getPathToFolder().setDisable(false);
            serverController.setTextArea("Сервер остановлен");
            server.closeServer();
        });
        stage.setOnCloseRequest(windowEvent -> {
            Platform.exit();
            server.closeServer();
        });
        stage.show();
    }

    private Object init(Stage stage, String source, String title) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        stage.setResizable(false);
        stage.setTitle(title);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(source)) {
            Parent parent = fxmlLoader.load(inputStream);
            Scene scene = new Scene(parent);
            stage.setScene(scene);
        }
        return fxmlLoader.getController();
    }
}

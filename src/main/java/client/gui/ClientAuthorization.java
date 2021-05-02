package client.gui;

import client.Client;
import client.ClientHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFuture;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientAuthorization extends Application {
    Logger LOGGER;
    Client client = new Client("localhost", 2000);
    ChannelFuture future;
    ClientHandler clientHandler;
    private String message;
    private File directory;
    private String pathToDirectory;
    private StringBuilder listFiles;

    public static void main(String[] args) {
        ClientAuthorization.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        LOGGER = LOGGER.getLogger(ClientAuthorization.class.getName());
        LOGGER.log(Level.INFO, "старт");

        Stage clientGui = new Stage();
        clientGui.initOwner(stage);
        clientGui.initModality(Modality.APPLICATION_MODAL);
        ClientControllerAuthorization authorizationController = (ClientControllerAuthorization) init(stage, "fxml/ScreenAuthorization.fxml", "Authorization");
        ClientControllerSynchronization synchronizationController = (ClientControllerSynchronization) init(clientGui, "fxml/ScreenSynchronization.fxml", "Synchronization");
        authorizationController.getButtonAuthorization().setOnMouseClicked(mouseEvent -> {
            if (future == null) {
                future = client.getFuture();
            }
            if (clientHandler == null) {
                clientHandler = client.getClientHandler();
            }
            // проверка правильности введенного логина и пароля
            if (authorizationController.getLogin().contains(" ") || authorizationController.getPassword().contains(" ") ||
                    authorizationController.getLogin().equals("") || authorizationController.getPassword().equals("")) {
                authorizationController.setTextArea("Логин и пароль введены не коректно");
            } else {
                // отправка на сервер логин и пароль для авторизации
                future.channel().writeAndFlush("/authorization " + authorizationController.getLogin() +
                        " " + authorizationController.getPassword() + "\n");

                // цикл на получение входящего сообщения от сервера (ответ)
                // выполняется цикл, пока сообщение пустое (ожидание сообщения с сервера)
                String message = readMessageAuthorization(authorizationController);
                // если авторизация прошла успешно
                if (message.startsWith("true")) {
                    String[] arrMsg = message.split(" ");
                    authorizationController.setActivated(Boolean.parseBoolean(arrMsg[0]));
                    if (authorizationController.isActivated() == true) {
                        // очищение полей и восстановление первоначального сообщения
                        authorizationController.setTextArea("Введите логин и пароль для входа или регистрации");
                        authorizationController.setLogin("");
                        authorizationController.setPassword("");
                        synchronizationController.setLabelUserName(arrMsg[1]);
                        // при закрытии окна, завершается приложение
                        clientGui.setOnCloseRequest(windowEvent -> {
                            // закрывается соединение после закрытия окна "синхронизация"
                            client.closeClient();
                            Platform.exit();
                        });
                        // открывается новое окно
                        synchronizationController.getButtonBack().setOnMouseClicked(back -> {
                            clientGui.hide();
                        });
                        clientGui.show();
                    }
                } else {
                    authorizationController.setTextArea(message);
                }
            }
        });

        stage.setOnCloseRequest(windowEvent -> {
            // закрывается соединение после закрытия окна
            client.closeClient();
        });

        /** проверка файлов **/
        synchronizationController.getButtonCheck().setOnMouseClicked(check -> {
            pathToDirectory = synchronizationController.getPath();
            try {
                directory = new File(pathToDirectory);
                listFiles = new StringBuilder();
                for (String x : directory.list()) {
                    listFiles.append("\\" + x + "%%");
                }
                future.channel().writeAndFlush("/check " + listFiles + "\n");
                readMessageSynchronization(synchronizationController);
            } catch (NullPointerException e) {
                synchronizationController.setMessage("Неверно указан путь к папке!");
            }
        });

        /** загрузка файлов на сервер (файлы, которых нет на сервере, удаляются) **/
        synchronizationController.getButtonLoadToServer().setOnMouseClicked(loadToServer -> {
            pathToDirectory = synchronizationController.getPath();
            try {
                directory = new File(pathToDirectory);
                listFiles = new StringBuilder();
                for (String x : directory.list()) {
                    listFiles.append("\\" + x + "%%");
                }
                future.channel().writeAndFlush("/loadToServer " + listFiles + "\n");
//                readMessageSynchronization(synchronizationController);
                readMessageSynchronizationByte(synchronizationController);
            } catch (NullPointerException e) {
                synchronizationController.setMessage("Неверно указан путь к папке!");
            }
            /** **/


            /** **/
        });

        /** загрузка файлов с сервера **/
        synchronizationController.getButtonLoadFromServer().setOnMouseClicked(loadFromServer -> {
            pathToDirectory = synchronizationController.getPath();
            try {
                directory = new File(pathToDirectory);
                listFiles = new StringBuilder();
                for (String x : directory.list()) {
                    listFiles.append("\\" + x + "%%");
                }
                future.channel().writeAndFlush("/loadFromServer " + listFiles + "\n");
                readMessageSynchronization(synchronizationController);
            } catch (NullPointerException e) {
                synchronizationController.setMessage("Неверно указан путь к папке!");
            }
        });

        stage.show();
        // старт клиента в другом потоке
        new Thread(() -> {
            try {
                // стартую клиента с фреймворком netty
                client.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String readMessageAuthorization(ClientControllerAuthorization client) {
        String msg = "";
        while (message == null) {
            // происходи инициализация message по методу от clientHandler
            message = clientHandler.getMessage();
            // если сообщение не равно нулю, тогда полученное сообщение передается в GUI,
            // очищается message и message в clientHandler для ожидания нового сообщения
            // в конце завершается цикл и продолжается выполнение программы
            if (message != null) {
                msg = message;
                message = null;
                clientHandler.setMessage(null);
                break;
            }
        }
        return msg;
    }

    private void readMessageSynchronization(ClientControllerSynchronization client) {
        while (message == null) {
            message = clientHandler.getMessage();
            if (message != null) {
                client.setMessage(message);
                message = null;
                clientHandler.setMessage(null);
                break;
            }
        }
    }

/** Тестовый **/
    private byte[] readMessageSynchronizationByte(ClientControllerSynchronization client) {
        try(FileOutputStream fileOutputStream = new FileOutputStream("H:\\JavaGeekBrains\\GB_Project_Java_1\\папка для синхронизации\\working.docx")) {
            File file = new File("H:\\JavaGeekBrains\\GB_Project_Java_1\\папка для синхронизации\\working.docx");
            long sizeFile = file.length();
            boolean check = false;
            long sizeFileServer = 0;
            while (true) {
                if (sizeFile == sizeFileServer && check) break;
                System.out.println("client: " + sizeFile + "\nServer: " + clientHandler.getSizeFile());
                while (message == null) {
                    if (sizeFile == sizeFileServer && check) break;
                    message = clientHandler.getMessage();
                    if (message != null) {
                        client.setMessage(message);
                        fileOutputStream.write(clientHandler.getBytes());
                        sizeFileServer = clientHandler.getSizeFile();
                        clientHandler.setBytes(null);
//                    System.out.println(clientHandler.getMessage());
                        message = null;
                        clientHandler.setMessage(null);
                        check = true;
                        sizeFile = file.length();
                        break;
                    }
                }
                System.out.println("client: " + sizeFile + "\nServer: " + clientHandler.getSizeFile());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

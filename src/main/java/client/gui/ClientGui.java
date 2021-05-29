package client.gui;

import client.Client;
import client.ClientHandler;
import io.netty.channel.ChannelFuture;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

public class ClientGui extends Application {
    private Client client = new Client("localhost", 2000);
    private ChannelFuture future;
    private ClientHandler clientHandler;
    private String message, pathToDirectory, command;
    private File directory;
    private StringBuilder listFiles;

    public static void main(String[] args) {
        ClientGui.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
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
                        " " + authorizationController.getPassword());

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
                            client.closeConnect();
                            Platform.exit();
                        });
                        // при нажатии "Back", закрывается окно синхронизации. Доступно окно авторизации
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
            client.closeConnect();
        });

        /* ЧЕК ФАЙЛОВ */
        synchronizationController.getButtonCheck().setOnMouseClicked(check -> {
            command = "check";
            checkFiles(synchronizationController, command);
        });

        /* ДЛЯ ОТПРАВКИ ПАКЕТА ФАЙЛОВ НА СЕРВЕР */
        /** загрузка файлов на сервер (файлы, которых нет на сервере, удаляются) **/
        synchronizationController.getButtonLoadToServer().setOnMouseClicked(loadToServer -> {
            command = "loadToServer";
            pathToDirectory = synchronizationController.getPath();
            synchronizationController.setMessage("Происходит отправка файлов на сервер...");
            // устанавливает путь к файлу (получает со строки ввода)
            clientHandler.setPathFolder(pathToDirectory);
            try {
                // проверка статусов файлов
                checkFiles(synchronizationController, command);
                // (ВЫВОД РЕЗУЛЬТАТА В ОКНО) ожидание получения имени скаченного файла
                while (!clientHandler.isReadNameFile()) {
                    // если имя файла получено, тогда выводиться имя файла на экран
                    // и устанавливается значение false
                    // очищается поле в окне информации GUI
                    synchronizationController.setMessage("");
                    if (clientHandler.isReadNameFile()) {
                        System.out.println(true);
                        synchronizationController.setMessage(clientHandler.getMessage());
                        System.out.println(clientHandler.getMessage());
                        clientHandler.setReadNameFile(false);
                        break;
                    }
                }
            } catch (NullPointerException e) {
                synchronizationController.setMessage("Неверно указан путь к папке!");
            }
        });

        /** загрузка файлов с сервера **/
        synchronizationController.getButtonLoadFromServer().setOnMouseClicked(loadFromServer -> {
            pathToDirectory = synchronizationController.getPath();
            synchronizationController.setMessage("Происходит загрузка файлов с сервера...");
            try {
                directory = new File(pathToDirectory);
                listFiles = new StringBuilder();
                for (String x : directory.list()) {
                    listFiles.append("\\" + x + "%%");
                }
                future.channel().writeAndFlush("/loadFromServer " + listFiles + "\n");
                // (ВЫВОД РЕЗУЛЬТАТА В ОКНО) ожидание получения имени скаченного файла
                while (!clientHandler.isReadNameFile()) {
                    // если имя файла получено, тогда выводиться имя файла на экран
                    // и устанавливается значение false
                    // очищается поле в окне информации GUI
                    synchronizationController.setMessage("");
                    if (clientHandler.isReadNameFile()) {
                        System.out.println(true);
                        synchronizationController.setMessage(clientHandler.getMessage());
                        System.out.println(clientHandler.getMessage());
                        clientHandler.setReadNameFile(false);
                        break;
                    }
                }
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
                // если соединение разорвалось
                synchronizationController.setMessage("Соединение разорвано\nперезапустите приложение");
                synchronizationController.getButtonCheck().setDisable(true);
                synchronizationController.getButtonLoadFromServer().setDisable(true);
                synchronizationController.getButtonLoadToServer().setDisable(true);
                synchronizationController.getButtonBack().setDisable(true);
                // если клиент не установил соединение с сервером
            } catch (InterruptedException | NullPointerException e) {
                authorizationController.setTextArea("Нет соединения с сервером!\nперезапустите приложение");
                // выключает активность у кнопок авторизации и регистрации
                authorizationController.getButtonAuthorization().setDisable(true);
                authorizationController.getButtonRegistration().setDisable(true);
                client.closeConnect();
                e.printStackTrace();
            }
        }).start();
    }

    // метод для проверки файлов
    private void checkFiles(ClientControllerSynchronization synchronizationController, String command) {
        pathToDirectory = synchronizationController.getPath();
        clientHandler.setPathFolder(pathToDirectory);
        try {
            directory = new File(pathToDirectory);
            listFiles = new StringBuilder();
            for (String x : directory.list()) {
                listFiles.append("\\" + x + "%%");
            }
            if (command.equals("check")) future.channel().writeAndFlush("/check " + listFiles);
            else if (command.equals("loadToServer")) future.channel().writeAndFlush("/loadToServer " + listFiles);
            // цикл для ожидания получения всего сообщения с сервера
//            while (!clientHandler.isCheck()) {
//                // если все сообщение было прочитано, устанавливается значение false и прекращается цикл
//                if (clientHandler.isCheck()) {
//                    clientHandler.setCheck(false);
//                    break;
//                }
//            }
            // для отображения в GUI полученного результата по файлам
            synchronizationController.setMessage(clientHandler.getCheckFiles());
        } catch (NullPointerException e) {
            synchronizationController.setMessage("Неверно указан путь к папке!");
        }
    }

    // получение сообщения с сервера
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

//    private void readMessageSynchronization(ClientControllerSynchronization client) {
//        while (message == null) {
//            message = clientHandler.getMessage();
//            if (message != null) {
//                client.setMessage(message);
//                message = null;
//                clientHandler.setMessage(null);
//                break;
//            }
//        }
//    }

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

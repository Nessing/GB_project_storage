package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;
import java.util.Arrays;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private String login, password, listFiles, result, pathFolderOfClient, nameFile, fileWorking;
    private String[] arrayFiles;
    private StringBuilder resultAboutFiles = new StringBuilder();
    private File folderServer;
    private long sizeFileServer = 0;
    private boolean isWriteFiles = false;
    private long sizeFile;
    private OutputStream outputStream;
    private File file;
    private byte[] bytes;
    private ServerEcho serverEcho = ServerEcho.getInstance();
    // путь к хранилищу сервера
    private String directoryClient = serverEcho.getPathToFolder() + "\\";

//    private String directoryClient = "H:\\JavaGeekBrains\\GB_Project_Java_1\\folderServer\\";
    // H:\JavaGeekBrains\GB_Project_Java_1\folderServer

    public void setSizeFileServer(long sizeFileServer) {
        this.sizeFileServer = sizeFileServer;
    }

    public void setSizeFile(long sizeFile) {
        this.sizeFile = sizeFile;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        System.out.println(msg);
        // проверка запроса на авторизацию
        if (msg.startsWith("/authorization")) {
            String[] str = msg.split(" ");
            login = str[1];
            password = str[2];
            System.out.printf("логин: %s пароль: %s\n", login, password);
            // проверка совпадения логина и пароля с отправкой квитанции клиенту
            if (login.equals("graf") && password.equals("123")) {
                serverEcho.setMessage("Клиент: " + login + " авторизировался");
                // путь к папке клиента на сервере
                pathFolderOfClient = directoryClient + login + "\\";
                folderServer = new File(pathFolderOfClient);
                channelHandlerContext.writeAndFlush("true " + login);
            } else channelHandlerContext.writeAndFlush("неверный логин или пароль");
        }

        /** ОБРАБОТКА ЗАПРОСОВ ОТ КЛИЕНТА **/
        /** ПРОВЕРКА ФАЙЛОВ **/
        if (msg.startsWith("/check ")) {
            channelHandlerContext.writeAndFlush("/checkFiles%%Проверка файлов:\n\n" + checkFile(msg) + "\n");
        }

/* ПОЛУЧЕНИЯ ФАЙЛА */
        // получает команду для готовности получения файла на сервер и отправляет ответ о готовности
        if (msg.startsWith("/loadToServer ")) {
            channelHandlerContext.writeAndFlush("/readyToGet%%" + checkFile(msg) + "\n");
        }
        // блок для записи файла
        if (isWriteFiles) {
            file = new File(fileWorking);
            System.out.println(file.length() + " FILE");
            bytes = msg.getBytes("ISO-8859-1");
            outputStream.write(bytes);
            setSizeFile(file.length());
            // когда размер файла с сервера равен скаченному файлу на клиенте, закрывается работа с файлом записи
            if (sizeFile == sizeFileServer) {
                outputStream.flush();
                outputStream.close();
                isWriteFiles = false;
                resultAboutFiles.append(file.getName() + " == Загружен на сервер\n");
                System.out.println("CHECK");
            } else {
                System.out.println("NO CHECK");
            }
            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
        }

        // если файла нет на клиенте, но есть на сервере - этот файл удаляется с сервера
        if (msg.startsWith("/deleteFile%%")) {
            String nameFile = msg.replace("/deleteFile%%", "");
            File file = new File(pathFolderOfClient + nameFile);
            file.delete();
            resultAboutFiles.append(nameFile + " == удален с сервера");
        }

        // возвращается команда получения файла
        // получаем данные о файле (имя и размер)
        if (msg.startsWith("/sendFile%%")) {
            // флаг, для вхождения в блок операции скачивания файла
            isWriteFiles = true;
            // второй элемент массива - размер файла на сервере
            String[] str = msg.split("%%");
            this.setSizeFileServer(Long.parseLong(str[1]));
            // получает имя файла, которое будет сохранено на клиенте
            nameFile = str[2];
            StringBuilder addNameFile = new StringBuilder(pathFolderOfClient + nameFile);
            fileWorking = addNameFile.toString();
            System.out.println(fileWorking + " ===== FILE");
            outputStream = new FileOutputStream(fileWorking, true);
            if (this.sizeFileServer == 0) {
                outputStream.flush();
                outputStream.close();
                isWriteFiles = false;
                resultAboutFiles.append(nameFile.replace("/nameFile%%", "") + " == Загружен на сервер\n");
            }

            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
        }




/* ОТПРАВКА ФАЙЛОВ НА КЛИЕНТ */
        if (msg.startsWith("/loadFromServer ")) {
            // путь к файлу, который будет отправляться с сервера
            String files = checkFile(msg);
            String[] listFiles = files.split("\n");
            StringBuilder nameListFiles = new StringBuilder();
            System.out.println("====================");
            for (String x : listFiles) {
                System.out.println("[" + x + "]");
                // если файл находиться на сервере (результат файла "на сервере"), тогда происходит отправка файла
                if (x.endsWith("на сервере")) {
                    // из имени файла убираются слеш и результат "на сервере", который заменяется на статус "скачен"
                    x = x.replace("\\", "");
                    x = x.replace(" == на сервере", "");
                    nameListFiles.append(x + " == скачен\n");
                    x = pathFolderOfClient + x;
                    // метод отправки файла
                    sendFiles(x, channelHandlerContext);
                }
            }
            // отправляет результат о скачиваемом файле
            channelHandlerContext.writeAndFlush("/nameFile%%" + nameListFiles.toString());
            System.out.println("====================");
        }
        // когда все файлы были проверены (скачены и удалены), отправляется результат
        if (msg.startsWith("/endFiles ")) {
            channelHandlerContext.writeAndFlush("/nameFile%%" + resultAboutFiles.toString() + "\n");
            resultAboutFiles.setLength(0);
        }
    }

    // метод для проверки файлов
    private String checkFile(String msg) {
        result = " == на клиенте\n";
        // убираем команду с входящего запроса
        if (msg.startsWith("/check ")) listFiles = msg.replace("/check ", "");
        if (msg.startsWith("/loadFromServer ")) listFiles = msg.replace("/loadFromServer ", "");
        if (msg.startsWith("/loadToServer ")) listFiles = msg.replace("/loadToServer ", "");

        // получение массива с разделением строк по спец. символу
        arrayFiles = listFiles.split("%%");
        // установка всем файлам, которые пришли на сервер, статус "на клиенте"
        listFiles = listFiles.replaceAll("%%", " == на клиенте\n");
        // цикл для прохода по папке с файлами на сервере
        for (String x : folderServer.list()) {
            // добавление в начало название файла слеша
            x = "\\" + x;
            boolean isExist = false;
            // цикл для прохода по папке с файлами на клиенте
            for (String y : arrayFiles) {
                // если файл на клиенте совпадает с файлом на сервере,
                // тогда у файла меняется статус на "синхронизирован"
                if (x.contains(y)) {
                    listFiles = listFiles.replace(x + " == на клиенте", x + " == синхронизирован");
                    System.out.println("файл в папке " + x + " == " + y);
                    isExist = true;
                    break;
                }
            }
            // если файла на клиенте нет, но есть на сервере, тогда файлу присваивается статус "на сервере"
            if (!isExist) {
                listFiles = listFiles + x + " == на сервере\n";
            }
        }
        System.out.println(listFiles);
        return listFiles;
    }

    // метод для отправки файла на сервер
    private void sendFiles(String fileWorking, ChannelHandlerContext channelHandlerContext) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(fileWorking)) {
            // File служит для получения размера файла
            File file = new File(fileWorking);
            long sizeFile = file.length();
            long sizeFileControl = file.length();
            long sizeFrameByte = 8192;
            int count = 0;
            String stringOut = "";
            byte[] bytes = new byte[(int) sizeFrameByte];
            channelHandlerContext.writeAndFlush("/downloadFile%%" + sizeFile + "%%" + file.getName());
            // если файл меньше фрейма, тогда устанавливается новый размер массива байтов
            if (sizeFileControl <= sizeFrameByte) {
                bytes = new byte[(int) sizeFileControl];
            }
            while (fileInputStream.read(bytes) > 0) {
                System.out.println("\nFILE NAME = " + file.getName());
                System.out.println("sizeFile " + sizeFile);
                System.out.println("sizeFileControl " + sizeFileControl);
                count++;
                System.out.println("== FRAME == " + count + " " + bytes.length);
                stringOut = new String(bytes, "ISO-8859-1");
                System.out.println("SIZE BYTES == " + bytes.length);
                channelHandlerContext.writeAndFlush(stringOut);
                sizeFileControl -= sizeFrameByte;
                if (sizeFileControl <= sizeFrameByte && sizeFileControl > 0) {
                    bytes = new byte[(int) sizeFileControl];
                }
                if (sizeFileControl <= 0) break;
                Arrays.fill(bytes, (byte) 0);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println(cause);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("подключение выполнено");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("клиент отключился");
    }
}

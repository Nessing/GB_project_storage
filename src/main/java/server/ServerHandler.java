package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private String login, password, listFiles, result, pathFolderOfClient, nameFile, path;
    private String[] arrayFiles;
    private File folderServer;
    private long sizeFileServer = 0;
    private boolean isWriteFiles = false;
    private long sizeFile;
    private OutputStream outputStream;
    private File file;
    private byte[] bytes;

    String fileWorking = "H:\\JavaGeekBrains\\GB_Project_Java_1\\folderServer\\";

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
                // путь к папке на сервере
                pathFolderOfClient = fileWorking + login + "\\";
                folderServer = new File(pathFolderOfClient);
//                String s = "true " + login;
//                byte[] bytes = s.getBytes("US-ASCII");
//                System.out.println(Arrays.toString(bytes));
                channelHandlerContext.writeAndFlush("true " + login);
            } else channelHandlerContext.writeAndFlush("неверный логин или пароль");
        }

        /** ОБРАБОТКА ЗАПРОСОВ ОТ КЛИЕНТА **/
        /** ПРОВЕРКА ФАЙЛОВ **/
        if (msg.startsWith("/check ")) {
            result = " == на клиенте\n";
            // убираем команду с входящего запроса
            listFiles = msg.replace("/check ", "");

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
            // отправляем строку ответа клиенту
            channelHandlerContext.writeAndFlush("/checkFiles%%Проверка файлов:\n\n" + listFiles + "\n");
        }

        /** ЗАГРУЗКА ФАЙЛОВ НА СЕРВЕР ОТ КЛИЕНТА **/
//        if (msg.startsWith("/loadToServer ")) {
//            // убираем команду с входящего запроса
//            listFiles = msg.replace("/loadToServer ", "");
//            // получение массива с разделением строк по спец. символу
//            arrayFiles = listFiles.split("%%");
//            // разделяем пробелами входное сообщение по спец. символу
//            listFiles = listFiles.replaceAll("%%", "\n");
//            // цикл для прохода по папке с файлами на клиенте (поиск несуществующих на сервере)
//            for (String x : arrayFiles) {
//                boolean isExist = false;
//                String fileClients = x;
//                // цикл для прохода по папке с файлами на сервере
//                for (String y : folderServer.list()) {
//                    // если файл на клиенте совпадает с файлом на сервере,
//                    // тогда у файла меняется статус на "синхронизирован"
//                    if (x.contains(y)) {
//                        listFiles = listFiles.replace(x + "\n", "");
//                        isExist = true;
//                        break;
//                    }
//                }
//                // если файла на клиенте нет, но есть на сервере, тогда файлу присваивается статус "на сервере"
//                if (!isExist) {
//                    listFiles = listFiles.replace(fileClients + "\n", fileClients + " == загружен на сервер\n");
//                }
//            }
//            System.out.println(listFiles);
//        }

/* ТЕСТ ПОЛУЧЕНИЯ ФАЙЛА */
        // получает команду для готовности получения файла на сервер и отправляет ответ о готовности
        if (msg.startsWith("/loadToServer ")) channelHandlerContext.writeAndFlush("/readyToGet%%\n");
        // блок для записи файла
        if (isWriteFiles) {
            file = new File(pathFolderOfClient);
            System.out.println(file.length() + " FILE");
            bytes = msg.getBytes("ISO-8859-1");
            System.out.println("SIZE BYTES == " + bytes.length + " [page 146]");
            outputStream.write(bytes);
            setSizeFile(file.length());
            // когда размер файла с сервера равен скаченному файлу на клиенте, закрывается работа с файлом записи
            if (sizeFile == sizeFileServer) {
                outputStream.flush();
                outputStream.close();
                isWriteFiles = false;
                channelHandlerContext.writeAndFlush("/nameFile%%" + file.getName() + " == Загружен на сервер");
                System.out.println("CHECK");
            } else {
                System.out.println("NO CHECK");
            }
            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
        }
        // возвращается команда получения файла
        if (msg.startsWith("/sendFile%%")) {
            // флаг, для вхождения в блок операции скачивания файла
            isWriteFiles = true;
            // второй элемент массива - размер файла на сервере
            String[] str = msg.split("%%");
            System.out.println("[" + str[1] + "]");
            this.setSizeFileServer(Long.parseLong(str[1]));
            // получает имя файла, которое будет сохранено на клиенте
            nameFile = str[2];
            StringBuilder addNameFile = new StringBuilder(pathFolderOfClient + nameFile);
            pathFolderOfClient = addNameFile.toString();
            System.out.println(pathFolderOfClient + " ===== FILE");
            outputStream = new FileOutputStream(pathFolderOfClient, true);

            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
        }


        /** ЗАГРУЗКА ФАЙЛОВ С СЕРВЕРА НА КЛИЕНТ **/
        if (msg.startsWith("/loadFromServer ")) {

/* ОТПРАВКА ФАЙЛА НА КЛИЕНТ */
            // путь к файлу, который будет отправляться с сервера
            fileWorking = pathFolderOfClient + "Microsoft Access База данных.accdb";
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
                channelHandlerContext.writeAndFlush("/nameFile%%" + file.getName() + " == скачен");
            }


//            String outMessage = "";
//            // убираем команду с входящего запроса
//            listFiles = msg.replace("/loadFromServer ", "");
//            // получение массива с разделением строк по спец. символу
//            arrayFiles = listFiles.split("%%");
//            // разделяем пробелами входное сообщение по спец. символу
//            listFiles = listFiles.replaceAll("%%", "\n");
//            // цикл для прохода по папке с файлами на сервере
//            for (String x : folderServer.list()) {
//                // добавление в начало название файла слеша
//                x = "\\" + x;
//                boolean isExist = false;
//                String fileClients = x;
//                // цикл для прохода по папке с файлами на клиенте
//                for (String y : arrayFiles) {
//                    // если файл на клиенте совпадает с файлом на сервере,
//                    // тогда у файла меняется статус на "синхронизирован"
//                    if (x.contains(y)) {
//                        listFiles = listFiles.replace(x + "\n", "");
//                        isExist = true;
//                        break;
//                    }
//                }
//                // если файла на клиенте нет, но есть на сервере, тогда файлу присваивается статус "на сервере"
//                if (!isExist) {
//                    outMessage += fileClients + " == загружен с сервера\n";
//                }
//            }
//            System.out.println(outMessage);
//            // отправляем строку ответа клиенту
//            channelHandlerContext.writeAndFlush("Скаченные файлы с сервера:\n\n" + outMessage + "\n");
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

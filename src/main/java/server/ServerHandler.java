package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
    String login, password, listFiles, result;
    String[] arrayFiles;
    File folderServer = new File("H:\\JavaGeekBrains\\GB_Project_Java_1\\folderServer");

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
                channelHandlerContext.writeAndFlush("true " + login);
            } else channelHandlerContext.writeAndFlush("неверный логин или пароль");
        }

        /** ОБРАБОТКА ЗАПРОСОВ ОТ КЛИЕНТА **/
        if (msg.startsWith("/check ")) {
            result = " == на клиенте\n";
            // убираем команду с входящего запроса
            listFiles = msg.replace("/check ", "");

            /** ПРОВЕРКА ФАЙЛОВ **/
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
            channelHandlerContext.writeAndFlush(listFiles);
        }

        /** ЗАГРУЗКА ФАЙЛОВ НА СЕРВЕР **/
        if (msg.startsWith("/loadToServer ")) {
            // убираем команду с входящего запроса
            listFiles = msg.replace("/loadToServer ", "");
            // получение массива с разделением строк по спец. символу
            arrayFiles = listFiles.split("%%");
            // разделяем пробелами входное сообщение по спец. символу
            listFiles = listFiles.replaceAll("%%", "\n");
            // цикл для прохода по папке с файлами на клиенте (поиск несуществующих на сервере)
            for (String x : arrayFiles) {
                boolean isExist = false;
                String fileClients = x;
                // цикл для прохода по папке с файлами на сервере
                for (String y : folderServer.list()) {
                    // если файл на клиенте совпадает с файлом на сервере,
                    // тогда у файла меняется статус на "синхронизирован"
                    if (x.contains(y)) {
                        listFiles = listFiles.replace(x + "\n", "");
                        isExist = true;
                        break;
                    }
                }
                // если файла на клиенте нет, но есть на сервере, тогда файлу присваивается статус "на сервере"
                if (!isExist) {
                    listFiles = listFiles.replace(fileClients, fileClients + " == загружен на сервер");
                }
            }
            System.out.println(listFiles);
            // отправляем строку ответа клиенту
            channelHandlerContext.writeAndFlush(listFiles);
        }

        /** ЗАГРУЗКА ФАЙЛОВ С СЕРВЕРА НА КЛИЕНТ **/
        if (msg.startsWith("/loadFromServer ")) {
            String outMessage = "";
            // убираем команду с входящего запроса
            listFiles = msg.replace("/loadFromServer ", "");
            // получение массива с разделением строк по спец. символу
            arrayFiles = listFiles.split("%%");
            // разделяем пробелами входное сообщение по спец. символу
            listFiles = listFiles.replaceAll("%%", "\n");
            // цикл для прохода по папке с файлами на сервере
            for (String x : folderServer.list()) {
                // добавление в начало название файла слеша
                x = "\\" + x;
                boolean isExist = false;
                String fileClients = x;
                // цикл для прохода по папке с файлами на клиенте
                for (String y : arrayFiles) {
                    // если файл на клиенте совпадает с файлом на сервере,
                    // тогда у файла меняется статус на "синхронизирован"
                    if (x.contains(y)) {
                        listFiles = listFiles.replace(x + "\n", "");
//                        System.out.println("файл в папке " + y + " == есть на сервере");
                        isExist = true;
                        break;
                    }
                }
                // если файла на клиенте нет, но есть на сервере, тогда файлу присваивается статус "на сервере"
                if (!isExist) {
                    outMessage += fileClients + " == загружен с сервера\n";
                }
            }
            System.out.println(outMessage);
            // отправляем строку ответа клиенту
            channelHandlerContext.writeAndFlush(outMessage);
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

package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;
import java.util.Arrays;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String message, path, nameFile, checkFiles, fileWorking;
    private StringBuilder listBuf = new StringBuilder();
    private byte[] bytes;
    private long sizeFile;
    private boolean isWriteFiles = false;
    private boolean isSendFiles = false;
    private boolean isCheck = false;
    private boolean isReadNameFile = false;
    private long sizeFileServer = 0;
    private OutputStream outputStream;

    private String pathToDirectory = "H:\\JavaGeekBrains\\GB_Project_Java_1\\папка для синхронизации\\";

    // файл служит для котроля размера файла
    private File file;

    public boolean isReadNameFile() {
        return isReadNameFile;
    }

    public void setReadNameFile(boolean readNameFile) {
        isReadNameFile = readNameFile;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public String getCheckFiles() {
        return checkFiles;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setFile(String file) {
        this.file = new File(file);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSizeFile(long sizeFile) {
        this.sizeFile = sizeFile;
    }

    public void setSizeFileServer(long sizeFileServer) {
        this.sizeFileServer = sizeFileServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        System.out.println(msg + " === сообщение входа [77]");
        /** для скачанивания файла с сервера **/
        // если принятое сообщение начинается со служебной команды "/downloadFile ", это означает, что сервер передает
        // размер и имя файла, который необходимо принять
        if (msg.startsWith("/downloadFile%%")) {
            // флаг, для вхождения в блок операции скачивания файла
            isWriteFiles = true;
            // второй элемент массива - размер файла на сервере
            String[] str = msg.split("%%");
            System.out.println("[" + str[1] + "]");
            this.setSizeFileServer(Long.parseLong(str[1]));
            // получает имя файла, которое будет сохранено на клиенте
            nameFile = str[2];
            StringBuilder addNameFile = new StringBuilder(path + "\\" + nameFile);
            path = addNameFile.toString();
            outputStream = new FileOutputStream(path, true);
            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);

            /** для чека файлов **/
        } else if (msg.startsWith("/checkFiles%%")) {
            // убирается служебная команда с принятного сообщения
            checkFiles = msg.replace("/checkFiles%%", "");
            // после прочтения, устанавливается значение true (сообщение прочитано)
            isCheck = true;
            /** для отправки файла на сервер **/
            // сервер готов к получению файла
        } else if (msg.startsWith("/readyToGet%%")) {

/** Тестовый отправки файла на сервер **/
            // путь к файлу, который будет отправляться с сервера
            fileWorking = pathToDirectory + "working.docx";
            try (FileInputStream fileInputStream = new FileInputStream(fileWorking)) {
                // File служит для получения размера файла
                file = new File(fileWorking);
                sizeFile = file.length();
                long sizeFileControl = file.length();
                long sizeFrameByte = 8192;
                int count = 0;
                String stringOut = "";
                byte[] bytes = new byte[(int) sizeFrameByte];
                channelHandlerContext.writeAndFlush("/sendFile%%" + sizeFile + "%%" + file.getName());
                while (fileInputStream.read(bytes) > 0) {
                    System.out.println("SEND FILE TO SERVER");
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

        } else {
            // в ином случае считывается принятое сообщение
            this.message = msg;
            System.out.println("!!! channelRead !!!");
            // блок для записи файла
            if (isWriteFiles) {
                file = new File(path);
                System.out.println(file.length() + " FILE");
                bytes = msg.getBytes("ISO-8859-1");
                System.out.println("SIZE BYTES == " + bytes.length);
                outputStream.write(bytes);
                setSizeFile(file.length());
                // когда размер файла с сервера равен скаченному файлу на клиенте, закрывается работа с файлом записи
                if (sizeFile == sizeFileServer) {
                    outputStream.flush();
                    outputStream.close();
                    isWriteFiles = false;
                    System.out.println("CHECK");
                } else {
                    System.out.println("NO CHECK");
                }
                System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
            }
            // чтение имени файла
        }
        if (msg.startsWith("/nameFile%%")) {
            System.out.println("NAME FILE ==== ");
            message = msg.replace("/nameFile%%", "");
            System.out.println("MESSAGE: " + message);
            isReadNameFile = true;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("!! ПРОЧИТАНО !!");
        System.out.println("End");
        super.channelReadComplete(ctx);
    }
}

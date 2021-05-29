package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;
import java.util.Arrays;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String message, pathFolder, nameFile, checkFiles, fileWorking, pathFile;
    private byte[] bytes;
    private long sizeFile;
    private boolean isWriteFiles = false;
    private boolean isCheck = false;
    private boolean isReadNameFile = false;
    private long sizeFileServer = 0;
    private OutputStream outputStream;

    // файл служит для котроля размера файла
    private File file;

    public boolean isReadNameFile() {
        return isReadNameFile;
    }

    public void setReadNameFile(boolean readNameFile) {
        isReadNameFile = readNameFile;
    }

    public String getCheckFiles() {
        return checkFiles;
    }

    public void setPathFolder(String pathFolder) {
        this.pathFolder = pathFolder;
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
        /** для скачанивания файла с сервера **/
        // если принятое сообщение начинается со служебной команды "/downloadFile ", это означает, что сервер передает
        // размер и имя файла, который необходимо принять
        /* ПОЛУЧЕНИЕ ФАЙЛА С СЕРВЕРА */
        if (msg.startsWith("/downloadFile%%")) {
            // флаг, для вхождения в блок операции скачивания файла
            isWriteFiles = true;
            // второй элемент массива - размер файла на сервере
            String[] str = msg.split("%%");
            System.out.println("[" + str[1] + "]");
            this.setSizeFileServer(Long.parseLong(str[1]));
            // получает имя файла, которое будет сохранено на клиенте
            nameFile = str[2];
            StringBuilder addNameFile = new StringBuilder(pathFolder + "\\" + nameFile);
            pathFile = addNameFile.toString();
            outputStream = new FileOutputStream(pathFile, true);
            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
            /* ЧЕК ФАЙЛОВ */
        } else if // чтение имени файла
        (msg.startsWith("/nameFile%%")) {
            message = msg.replace("/nameFile%%", "");
            isReadNameFile = true;
            isCheck = true;
        } else if (msg.startsWith("/checkFiles%%")) {
            // убирается служебная команда с принятного сообщения
            checkFiles = msg.replace("/checkFiles%%", "");
            // после прочтения, устанавливается значение true (сообщение прочитано)
            isCheck = true;

            /** для отправки файла на сервер **/
            // сервер готов к получению файла
        } else if (msg.startsWith("/readyToGet%%")) {

            /* ОТПРАВКА ФАЙЛА НА СЕРВЕРА */
            checkFiles = msg.replace("/readyToGet%%", "");
            String[] files = checkFiles.split("\n");
            for (String x : files) {
                if (x.endsWith(" == на клиенте")) {
                    x = x.replace("\\", "");
                    x = x.replace(" == на клиенте", "");
                    sendFiles(x, channelHandlerContext);
                } else if (x.endsWith(" == на сервере")) {
                    x = x.replace("\\", "");
                    x = x.replace(" == на сервере", "");
                    channelHandlerContext.writeAndFlush("/deleteFile%%" + x);
                }
            }
            channelHandlerContext.writeAndFlush("/endFiles ");
        }
        else {
            // в ином случае считывается принятое сообщение
            this.message = msg;
            // блок для записи файла
            if (isWriteFiles) {
                file = new File(pathFile);
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
        }
    }

    private void sendFiles(String fileWorking, ChannelHandlerContext channelHandlerContext) throws IOException {
        this.fileWorking = pathFolder + "\\" + fileWorking;
        try (FileInputStream fileInputStream = new FileInputStream(this.fileWorking)) {
            // File служит для получения размера файла
            file = new File(this.fileWorking);
            sizeFile = file.length();
            long sizeFileControl = file.length();
            long sizeFrameByte = 8192;
            int count = 0;
            String stringOut = "";
            byte[] bytes = new byte[(int) sizeFrameByte];
            channelHandlerContext.writeAndFlush("/sendFile%%" + sizeFile + "%%" + file.getName());
            // если файл меньше фрейма, тогда устанавливается новый размер массива байтов
            if (sizeFileControl <= sizeFrameByte) {
                bytes = new byte[(int) sizeFileControl];
            }
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
    }
}

package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String message;
    private byte[] bytes;
    private long sizeFile;
    private boolean isRead = false;
    long sizeFileServer = 0;
    OutputStream outputStream;

    // файл служит для котроля размера файла
    File file = new File("E:\\IDEA\\GeekBrains\\testsTo\\open_server_panel_5_3_8_setup.exe");

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
        // если принятое сообщение начинается со служебной команды "/sizeFile ", это означает, что сервер передает
        // размер файла, который необходимо принять
        if (msg.startsWith("/sizeFile ")) {
            // флаг, для вхождения в блок операции записи скачивания файла
            isRead = true;
            // второй элемент массива - размер файла на сервере
            String[] str = msg.split(" ");
            System.out.println("[" + str[1] + "]");
            this.setSizeFileServer(Long.parseLong(str[1]));
            System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
        } else {
            this.message = msg;
            System.out.println("!!! channelRead !!!");
            // блок для чтения чтения файла
            if (isRead) {
                // путь, куда будет сохранен скаченный файл
                String name = "E:\\IDEA\\GeekBrains\\testsTo\\open_server_panel_5_3_8_setup.exe";
                System.out.println(file.length() + " FILE");
                outputStream = new FileOutputStream(file, true);
                bytes = msg.getBytes("ISO-8859-1");
                System.out.println("SIZE BYTES == " + bytes.length);
                outputStream.write(bytes);
                setSizeFile(file.length());
                // когда размер файла с сервера равен скаченному файлу на клиенте, закрывается работа с файлом записи
                if (sizeFile == sizeFileServer) {
                    outputStream.close();
                    isRead = false;
                    System.out.println("CHECK");
                } else {
                    System.out.println("NO CHECK");
                }
                System.out.println("client: " + sizeFile + "\nServer: " + sizeFileServer);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("!! ПРОЧИТАНО !!");
        System.out.println("End");
        super.channelReadComplete(ctx);
    }
}

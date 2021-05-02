package client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String message;
    private byte[] bytes;
    private ByteBuf byteBuf;
    private long sizeFile;
    private boolean isRead = true;

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public long getSizeFile() {
        return sizeFile;
    }

    public void setSizeFile(long sizeFile) {
        this.sizeFile = sizeFile;
    }



    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        isRead = true;
        System.out.println(msg + "\n == Check");
        if (msg.startsWith("/sizeFile ")) {
            String[] str = msg.split(" ");
            System.out.println("[" + str[1] + "]");
            this.setSizeFile(Long.parseLong(str[1]));
            System.out.println("S = " + getSizeFile());
            System.out.println("SIZE = " + msg);
            System.out.println(getSizeFile());
        } else {
//            System.out.println(msg);
            bytes = msg.getBytes("ISO-8859-1");
            this.message = msg;
            System.out.println("!!! channelRead !!!");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        System.out.println(Arrays.toString(bytes));
        System.out.println("!! ПРОЧИТАНО !!");
        System.out.println("End");
        isRead = false;
        super.channelReadComplete(ctx);
    }
}

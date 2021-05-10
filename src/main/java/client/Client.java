package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
    private final int port;
    private final String serverName;
    private ChannelFuture future;
    private ClientHandler clientHandler;
    private NioEventLoopGroup group;

    public static void main(String[] args) throws InterruptedException {
//        new Client("localhost", 2000).start();
    }

    public Client(String serverName, int port) {
        this.serverName = serverName;
        this.port = port;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public void start() throws InterruptedException {
        group = new NioEventLoopGroup(1);
        try {
            Bootstrap client = new Bootstrap();
            clientHandler = new ClientHandler();
            client.group(group);
            client.channel(NioSocketChannel.class);
            client.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) {
                    nioSocketChannel.pipeline().addLast(
                            new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                            new StringEncoder(),
                            new StringDecoder(),
                            clientHandler
                    );
                }
            });
            client.option(ChannelOption.SO_KEEPALIVE, true);
            System.out.println("Client started");

            // подключение к серверу
            future = client.connect(serverName, port).sync();

            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void closeClient() {
        future.channel().closeFuture();
        group.shutdownGracefully();
        System.out.println("client close");
    }
}

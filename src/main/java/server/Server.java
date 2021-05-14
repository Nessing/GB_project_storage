package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Server {
    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new Server(2000).start();
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        // авторизация
        NioEventLoopGroup loginGroup = new NioEventLoopGroup(1);
        // работа с подключенным клиентом
        NioEventLoopGroup eventsGroup = new NioEventLoopGroup();

        try {
            // создание сервера
            ServerBootstrap server = new ServerBootstrap();
            server.group(loginGroup, eventsGroup);
            // регистрация сокета для формирования новых соединений
            server.channel(NioServerSocketChannel.class);
            // настройка параметров для работы с подключенным клиентом
            server.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel nioSocketChannel) {
                    nioSocketChannel.pipeline().addLast(
                            new LengthFieldBasedFrameDecoder(16384, 0, 4, 0, 4),
                            new StringDecoder(),
                            new LengthFieldPrepender(4),
                            new StringEncoder(),
                            new ServerHandler()
                    );
                }
            });
            // добавление опций на сервер (128 необработанных запросов могут находиться в очереди
            // при подключении на сервер)
            server.option(ChannelOption.SO_BACKLOG, 128);
            // добавление опций клиенту при подключении (оставлять соединение до явного отключения клиента от сервера)
            server.childOption(ChannelOption.SO_KEEPALIVE, true);

            // запуск сервера на указанном порту (привязка port к серверу) и ожидание его запуска (.sync)
            ChannelFuture channelFuture = server.bind(port).sync();
            System.out.println("Server get up");
            // получение задачи на закрытие сервера и ожидание её выполнения (.sync)
            channelFuture.channel().closeFuture().sync();
        }
        finally {
            // завершение тредПотоков
            loginGroup.shutdownGracefully();
            eventsGroup.shutdownGracefully();
        }
    }
}

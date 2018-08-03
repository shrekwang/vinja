package com.github.vinja.nio;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class VinjaServer extends Thread {

    public void run() {

        VinjaMsgHandler vinjaMsgHandler = new VinjaMsgHandler();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(9528))
                .childHandler(new ChannelInitializer<SocketChannel>(){
                    public void initChannel(SocketChannel ch) throws Exception {
                        //ch.pipeline().addLast("decoder", new StringDecoder());
                        ch.pipeline().addLast("encoder", new StringEncoder());
                        ch.pipeline().addLast("jsonDecoder", new JsonObjectDecoder());
                        ch.pipeline().addLast(vinjaMsgHandler);
                    }
                });
            ChannelFuture f = b.bind().sync(); 
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try { bossGroup.shutdownGracefully().sync(); } catch (Exception e) {}
            try {workGroup.shutdownGracefully().sync(); } catch (Exception e) {}
        }

    }

    public static void main(String[] args) throws Exception {
        System.out.println("sfas");
        VinjaServer server = new VinjaServer();
        server.start();
    }

}

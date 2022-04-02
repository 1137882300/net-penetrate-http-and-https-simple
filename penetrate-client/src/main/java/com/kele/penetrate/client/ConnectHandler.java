package com.kele.penetrate.client;

import com.alibaba.fastjson.JSONObject;
import com.kele.penetrate.config.Config;
import com.kele.penetrate.factory.Autowired;
import com.kele.penetrate.factory.Recognizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
@SuppressWarnings("unused")
@Recognizer
public class ConnectHandler
{
    private Bootstrap bootstrap;
    private Channel channel;
    private long lastReplyTime;
    private ConcurrentHashMap<String, JSONObject> monitoringDataMap = new ConcurrentHashMap<>();

    @Autowired
    private Config config;


    //<editor-fold desc="构造">
    public ConnectHandler()
    {
    }
    //</editor-fold>

    //<editor-fold desc="启动">
    public void start()
    {
        bootstrap = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();

        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        //第3步 给NIoSocketChannel初始化handler， 处理读写事件
        bootstrap.handler(new NettyClientChannelInitializerHandler());
        doConnect();
    }
    //</editor-fold>

    //<editor-fold desc="连接">
    protected void doConnect()
    {
        if (this.channel != null && this.channel.isActive())
            return;
        ChannelFuture future = bootstrap.connect(config.getServiceConnectInfo().getIp(), config.getServiceConnectInfo().getPort());

        future.addListener((ChannelFutureListener) futureListener ->
        {
            if (futureListener.isSuccess())
            {
                this.channel = futureListener.channel();
                log.info("与服务器连接成功：" + config.getServiceConnectInfo().getIp() + ":" + config.getServiceConnectInfo().getPort());
            }
            else
            {
                log.info("5秒之后自动重连：" + config.getServiceConnectInfo().getIp() + ":" + config.getServiceConnectInfo().getPort());
                futureListener.channel().eventLoop().schedule(this::doConnect, 5, TimeUnit.SECONDS);
            }
        });
    }
    //</editor-fold>

    //<editor-fold desc="发送消息">
    public void send(Object msg)
    {
        if (this.channel == null || !this.channel.isActive())
            return;

        this.channel.writeAndFlush(msg);
    }
    //</editor-fold>
}

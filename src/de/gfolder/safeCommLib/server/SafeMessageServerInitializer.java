package de.gfolder.safeCommLib.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class SafeMessageServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final String websocketPath;
    private final SafeMessageServer safeMessageServer;

    public SafeMessageServerInitializer(SslContext sslCtx, String websocketPath, SafeMessageServer safeMessageServer) {
        this.sslCtx = sslCtx;
        this.websocketPath = websocketPath;
        this.safeMessageServer = safeMessageServer;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new SafeMessageServerHandler(websocketPath, !(sslCtx==null), safeMessageServer));
    }
}

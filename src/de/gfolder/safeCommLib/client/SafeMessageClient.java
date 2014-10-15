package de.gfolder.safeCommLib.client;

import de.gfolder.safeCommLib.SSLInitializer;
import de.gfolder.safeCommLib.messageHandler.MessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */
public class SafeMessageClient {

    private URI uri;
    private SslContext sslCtx;
    private String host;
    private int port;
    private boolean ssl;
    private Channel ch;
    private EventLoopGroup group;
    private MessageHandler<String> messageHandler;

    /**
     * Getter for property 'messageHandler'.
     *
     * @return Value for property 'messageHandler'.
     */
    public MessageHandler<String> getMessageHandler() {
        return messageHandler;
    }

    /**
     * Constructor
     *
     * @param url URL the server is bound to (must be of scheme ws:// or wss://
     * @throws URISyntaxException
     */
    public SafeMessageClient(String url, MessageHandler<String> messageHandler) throws URISyntaxException {
        this.messageHandler = messageHandler;

        uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        port = uri.getPort();

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only WS(S) is supported.");
        }

        ssl = "wss".equalsIgnoreCase(scheme);
    }

    /**
     * Initialize connection
     *
     * @throws InterruptedException
     * @throws CertificateException
     * @throws SSLException
     */
    public void init() throws InterruptedException, CertificateException, SSLException {
        /**
         * Init SSL Context if secure scheme was chosen
         */
        if (ssl) {
            sslCtx = SSLInitializer.getContext();
        } else {
            sslCtx = null;
        }

        group = new NioEventLoopGroup();

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        final SafeMessageClientHandler handler =
                new SafeMessageClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()), this);

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }
                        p.addLast(
                                new HttpClientCodec(),
                                new HttpObjectAggregator(8192),
                                handler);
                    }
                });

        ch = b.connect(uri.getHost(), port).sync().channel();
        messageHandler.setChannel(ch);
        handler.handshakeFuture().sync();
    }

    /**
     * Request channel to close by sending closing frame
     * @throws InterruptedException
     */
    public void requestClosing() throws InterruptedException
    {
        ch.writeAndFlush(new CloseWebSocketFrame());
        ch.closeFuture().sync();
    }

    /**
     * Send ping request
     */
    public void sendPing()
    {
        messageHandler.sendPing();
    }

    /**
     * Send the given message over the channel
     *
     * @param msg message to send
     */
    public void sendMessage(String msg)
    {
        messageHandler.sendMessage(msg);
    }

    /**
     * Close connection of this client
     */
    public void close()
    {
        group.shutdownGracefully();
    }
}

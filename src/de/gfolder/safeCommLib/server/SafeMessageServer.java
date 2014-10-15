package de.gfolder.safeCommLib.server;

import de.gfolder.safeCommLib.SSLInitializer;
import de.gfolder.safeCommLib.connector.BreakupHandler;
import de.gfolder.safeCommLib.connector.MessageReceiver;
import de.gfolder.safeCommLib.message.SafeMessage;
import de.gfolder.safeCommLib.messageHandler.JSONMessageHandler;
import de.gfolder.safeCommLib.messageHandler.MessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.HashMap;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class SafeMessageServer {
    private boolean ssl;
    private int port;
    private String websocketPath;

    protected SslContext sslCtx;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel ch;

    private HashMap<Channel, MessageHandler<String>> handlers;
    private HashMap<String, Channel> channels;

    private MessageReceiver messageReceiver;
    private BreakupHandler breakupHandler;

    /**
     * Constructor
     *
     * @param port port to use
     * @param ssl true if ssl shall be used, false if not
     * @param messageReceiver object implementing MessageReceiver to handle the message receiving
     * @param breakupHandler object implementing BreakupHandler to handle the breakup
     */
    public SafeMessageServer(int port, String websocketPath, boolean ssl, MessageReceiver messageReceiver, BreakupHandler breakupHandler) {
        this.port = port;
        this.websocketPath = websocketPath;
        this.ssl = ssl;
        this.messageReceiver = messageReceiver;
        this.breakupHandler = breakupHandler;
        handlers = new HashMap<>();
        channels = new HashMap<>();
    }

    /**
     * Initialize Server
     *
     * @throws CertificateException
     * @throws SSLException
     * @throws InterruptedException
     */
    public void init() throws CertificateException, SSLException, InterruptedException {
        /**
         * Init SSL Context if secure scheme was chosen
         */
        if (ssl) {
            sslCtx = SSLInitializer.getContext();
        } else {
            sslCtx = null;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new SafeMessageServerInitializer(sslCtx, websocketPath, this));

        ch = b.bind(port).sync().channel();
    }

    /**
     * Send a message
     *
     * @param channelIdentifier channel to send the message to
     * @param msg message to send
     * @throws InterruptedException
     */
    public void sendMessage(String channelIdentifier, String msg) throws InterruptedException {
        sendMessage(getChannel(channelIdentifier), msg);
    }

    /**
     * Send a message
     *
     * @param channel channel to send the message to
     * @param msg message to send
     * @throws InterruptedException
     */
    public void sendMessage(Channel channel, String msg) throws InterruptedException {
        getHandler(channel).sendMessage(msg);
    }

    /**
     * Send a ping frame to the channel identified by the given name
     *
     * @param channelIdentifier name of the channel
     */
    public void sendPing(String channelIdentifier)
    {
        sendPing(getChannel(channelIdentifier));
    }

    /**
     * Send a ping frame to the given channel
     *
     * @param channel channel to send the ping to
     */
    public void sendPing(Channel channel)
    {
        getHandler(channel).sendPing();
    }

    /**
     * Request the server to prepare for close/exit
     */
    public void requestClose()
    {
        //Close the channel for incoming connections
        ch.close();
    }

    /**
     * Close/exit server
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {
        ch.closeFuture().sync();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    /**
     * Add a new handler to list of handlers
     *
     * @param channel channel identifying the handler
     * @param handler the handler
     */
    public void addHandler(Channel channel, MessageHandler<String> handler)
    {
        handlers.put(channel, handler);
        channels.put(channel.toString(), channel);
    }

    /**
     * Get the handler belonging to the given channel from the list of handlers
     *
     * @param channel channel identifying the handler
     */
    public MessageHandler<String> getHandler(Channel channel)
    {
        return handlers.get(channel);
    }

    /**
     * Remove the handler belonging to the given channel from the list of handlers
     *
     * @param channel channel identifying the handler
     */
    public void removeHandler(Channel channel)
    {
        handlers.remove(channel);
        channels.remove(channel.toString());
    }

    /**
     * Get channel by given name
     *
     * @param channelIdentifier name identifying the channel
     * @return the corresponding channel for the name
     */
    public Channel getChannel(String channelIdentifier)
    {
        return channels.get(channelIdentifier);
    }

    /**
     * Get the identifier of the first registered channel
     * This should be used for debug purposes only
     *
     * Attention: Which channel is seen as "the first one" does not
     * depend on the order in which they were added to the list
     *
     * @return identifier of the first registered channel
     */
    public String getFirstChannelIdentifier()
    {
        return channels.values().toArray()[0].toString();
    }

    /**
     * Create a message handler (after handshake)
     *
     * @return a suiting message handler
     */
    public MessageHandler<String> createMessageHandler()
    {
        return new JSONMessageHandler() {
            @Override
            public void handleReceived(SafeMessage message, Channel channel) {
                messageReceiver.receive(message.getData(), channel.toString());
            }

            @Override
            public void handleBreakup(Channel channel) {
                removeHandler(channel);
                breakupHandler.handleBreakup(channel.toString());
            }
        };
    }
}

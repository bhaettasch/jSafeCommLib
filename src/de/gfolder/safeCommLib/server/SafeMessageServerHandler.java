package de.gfolder.safeCommLib.server;

import de.gfolder.safeCommLib.messageHandler.MessageHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class SafeMessageServerHandler  extends SimpleChannelInboundHandler<Object> {
    private String websocketPath;
    private boolean ssl;

    private WebSocketServerHandshaker handshaker;
    private SafeMessageServer safeMessageServer;

    /**
     * Constructor
     *
     * @param websocketPath Path were to switch to websocket
     * @param ssl whether to use ssl
     */
    public SafeMessageServerHandler(String websocketPath, boolean ssl, SafeMessageServer safeMessageServer) {
        this.websocketPath = websocketPath;
        this.ssl = ssl;
        this.safeMessageServer = safeMessageServer;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * Handle HTTP Request
     *
     * @param ctx Context
     * @param req Request
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ChannelFuture shakedChannel = handshaker.handshake(ctx.channel(), req);

            MessageHandler<String> handler = safeMessageServer.createMessageHandler();
            handler.setChannel(shakedChannel.channel());
            handler.activateConnectionSupervisor();
            safeMessageServer.addHandler(shakedChannel.channel(), handler);
        }
    }

    /**
     * Handle WebSocket Request
     *
     * @param ctx Context
     * @param frame Frame to handle
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
    {
        //Get matching handler for handshaked channel
        MessageHandler<String> handler = safeMessageServer.getHandler(ctx.channel());

        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            safeMessageServer.removeHandler(ctx.channel());
        }
        else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        }
        else if (frame instanceof PongWebSocketFrame) {
            handler.receivedPong();
        }
        else if (frame instanceof TextWebSocketFrame) {
            handler.receive(((TextWebSocketFrame) frame).text());
        }
        else
        {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * Get location of the WebSocket as string
     *
     * @param req request handled
     * @return matching WebSocket as string
     */
    private String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HOST) + websocketPath;
        if (ssl) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}

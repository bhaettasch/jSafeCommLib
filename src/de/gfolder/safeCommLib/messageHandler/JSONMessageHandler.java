package de.gfolder.safeCommLib.messageHandler;

import de.gfolder.safeCommLib.message.SafeMessage;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class JSONMessageHandler extends MessageHandler<String> {

    @Override
    public String encodeTransportMessage(SafeMessage message) {
        if(message == null)
            return null;

        JSONObject jObj = new JSONObject();
        jObj.put("sequenceNumber", message.getSequenceNumber());
        jObj.put("storedHash", message.getStoredHash());
        jObj.put("timestamp", message.getTimeStamp());
        jObj.put("type", message.getType().ordinal());
        jObj.put("data", message.getData());
        return jObj.toJSONString();
    }

    @Override
    public SafeMessage decodeTransportMessage(String transportMessage) {
        JSONObject jObj = (JSONObject) JSONValue.parse(transportMessage);
        long sequenceNumber = (Long) jObj.get("sequenceNumber");
        int storedHash = ((Long) jObj.get("storedHash")).intValue();
        long timestamp = (Long) jObj.get("timestamp");
        SafeMessage.Type type = SafeMessage.Type.values()[((Long) jObj.get("type")).intValue()];
        String data = (String) jObj.get("data");
        return new SafeMessage(sequenceNumber, storedHash, timestamp, data, type);
    }

    @Override
    protected void send(String encodedMessage) {
        WebSocketFrame frame = new TextWebSocketFrame(encodedMessage);
        channel.writeAndFlush(frame);
    }
}

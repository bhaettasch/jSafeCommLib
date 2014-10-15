package de.gfolder.safeCommLib.messageHandler;

import de.gfolder.safeCommLib.message.SafeMessage;
import de.gfolder.safeCommLib.message.SafeMessageFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public abstract class MessageHandler<T> {

    private long lastReceivedMessage;
    private boolean receivedPong;
    private LinkedList<StoredMessage<T>> sendingBuffer;
    private LinkedList<StoredMessage<SafeMessage>> receivingBuffer;
    protected Channel channel;
    private Timer pingTimer;

    private AtomicLong sendingSequenceNumber = new AtomicLong();

    /**
     * Interval between two checks of the supervisor in milliseconds
     */
    public static int SUPERVISOR_INTERVAL = 2000;

    /**
     * Constructor
     */
    protected MessageHandler() {
        sendingBuffer = new LinkedList<>();
        receivingBuffer = new LinkedList<>();
        lastReceivedMessage = -1;
        receivedPong = true;
    }

    /**
     * Getter for property 'channel'.
     *
     * @return Value for property 'channel'.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Setter for property 'channel'.
     *
     * @param channel Value to set for property 'channel'.
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Encode a safeMessage to be sended in the given format
     *
     * @param message message to be encoded
     * @return message in sendable format
     */
    public abstract T encodeTransportMessage(SafeMessage message);

    /**
     * Decode a received message back to safeMessage format
     *
     * @param transportMessage received message in transport format
     * @return decoded SafeMessage
     */
    public abstract SafeMessage decodeTransportMessage(T transportMessage);

    /**
     * Handle receiving of a message
     *
     * @param message message that was received
     */
    public void receive(T message)
    {
        SafeMessage safeMessage = decodeTransportMessage(message);

        switch(safeMessage.getType())
        {
            case MESSAGE:
                receiveMessage(safeMessage);
                break;
            case FEEDBACK:
                receiveFeedback(safeMessage);
                break;
            default:
                System.err.println("Received message of unsupported type");
        }
    }

    /**
     * Handle receiving of a normal message
     *
     * @param message received normal message
     */
    protected void receiveMessage(SafeMessage message)
    {
        SafeMessage answer = null;
        long sequenceNumber = message.getSequenceNumber();

        StoredMessage.State newState = checkMessage(message)? StoredMessage.State.OK : StoredMessage.State.MISSING;

        /*
        Store message in buffer
         */
        long start;
        if(receivingBuffer.size() == 0)
            start = lastReceivedMessage + 1;
        else
            start = receivingBuffer.getFirst().getSequenceNumber();
        int index = Long.valueOf(sequenceNumber - start).intValue();

        //Message placeholder already in buffer? Just set state and content
        try
        {
            StoredMessage<SafeMessage> storedMessage = receivingBuffer.get(index);
            storedMessage.setMessage(message);
            storedMessage.setState(newState);
        //Otherwise, create all needed placeholders in between and one for the message itself
        } catch (IndexOutOfBoundsException e) {
            for(int i = receivingBuffer.size(); i < index; i++)
            {
                receivingBuffer.add(new StoredMessage<SafeMessage>(start + index));
            }
            receivingBuffer.add(new StoredMessage<SafeMessage>(sequenceNumber, newState, message));
        }

        /*
        Confirm everything in buffer
         */
        long lastOK = -1;
        for (StoredMessage<SafeMessage> storedMessage : receivingBuffer) {
            if (storedMessage.getState() == StoredMessage.State.OK) {
                lastOK = storedMessage.getSequenceNumber();
                handleReceived(storedMessage.getMessage(), channel);
                receivingBuffer.removeFirst();
            }
            else
            {
                break;
            }
        }

        if(lastOK != -1)
            answer = SafeMessageFactory.createFeedbackMessageOK(lastOK);
        else
            answer = SafeMessageFactory.createFeedbackMessageFailed(lastReceivedMessage+1);

        send(encodeTransportMessage(answer));
        lastReceivedMessage = lastOK;
    }

    /**
     * Handle receiving of a feedback message
     *
     * @param message received feedback message
     */
    protected void receiveFeedback(SafeMessage message)
    {
        if(message.getData().equals("OK")) {
            for (StoredMessage<T> storedMessage : sendingBuffer) {
                if (storedMessage.getSequenceNumber() < message.getSequenceNumber())
                    sendingBuffer.removeFirst();
                else {
                    if (message.getData().equals("OK")) {
                        sendingBuffer.removeFirst();
                        System.out.println("Message was OK");
                    } else {
                        storedMessage.setState(StoredMessage.State.NEW);
                        System.out.println("Message was not OK. Resending");
                        sendBuffer();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Start an interval timer, that will periodically send ping messages
     * and check at the end of the interval if a corresponding pong was received
     */
    public void activateConnectionSupervisor()
    {
        pingTimer = new javax.swing.Timer(SUPERVISOR_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!receivedPong)
                {
                    pingTimer.stop();
                    handleBreakup(channel);
                }
                else
                {
                    receivedPong = false;
                    sendPing();
                }

            }
        });
        pingTimer.setRepeats(true);
        pingTimer.start();
    }

    /**
     * Perform actions to bring system to safe state after connection breakup
     *
     * @param channel channel that was closed
     */
    public void handleBreakup(Channel channel)
    {
        System.err.print("Client disconnected unexpectedly");
    }

    /**
     * Notify handler that a pong message has been received
     */
    public void receivedPong()
    {
        receivedPong = true;
    }

    /**
     * Send the given message over the channel
     *
     * @param data raw message
     */
    public void sendMessage(String data)
    {
        SafeMessage message = SafeMessageFactory.createSafeMessage(sendingSequenceNumber.getAndAdd(1), data);
        sendMessage(message);
    }

    /**
     * Send the given message over the channel
     *
     * @param message SafeMessage containing the real message
     */
    protected void sendMessage(SafeMessage message)
    {
        T encodedMessage = encodeTransportMessage(message);
        sendingBuffer.add(new StoredMessage<>(message.getSequenceNumber(), encodedMessage));
        sendBuffer();
    }

    /**
     * Send ping frame over the channel
     */
    public void sendPing()
    {
        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
        channel.writeAndFlush(frame);
    }

    /**
     * Try to send the content of the buffer (if any)
     */
    private void sendBuffer()
    {
        if(sendingBuffer.size() > 0)
        {
            Iterator<StoredMessage<T>> iterator = sendingBuffer.iterator();
            while ((iterator.hasNext()))
            {
                StoredMessage<T> storedMessage = iterator.next();
                if(storedMessage.getState() == StoredMessage.State.NEW)
                {
                    storedMessage.setSent();
                    send(storedMessage.getMessage());
                }
            }
        }
    }

    /**
     * Really send the raw message - has to be implemented based upon message encoding
     *
     * @param encodedMessage encoded message
     */
    protected abstract void send(T encodedMessage);

    /**
     * Check if the message was corrupted
     *
     * @param message message to check
     * @return true if message is recognized as safe and correct
     */
    public boolean checkMessage(SafeMessage message)
    {
        return message.matchingHash();
    }

    /**
     * Call an external action to react on received message
     * This has to be overridden
     *
     * @param message received safe message
     * @param channel channel the message arrived on
     */
    public void handleReceived(SafeMessage message, Channel channel)
    {
        System.out.println("'"+message.getData()+"' on "+channel.toString());
    }

    /**
     * Perform an action when the channel was closed
     */
    public void handleClosing()
    {
        System.out.println("Channel closed");
    }
}

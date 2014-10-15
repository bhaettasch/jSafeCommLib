package de.gfolder.safeCommLib.messageHandler;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class StoredMessage<T> {

    enum State {NEW, SENT, OK, CONFIRMED, MISSING}

    private long sequenceNumber;
    private State state;
    private T message;

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param sequenceNumber number of the message
     * @param state state of the message
     * @param message message to store/stored message
     */
    public StoredMessage(long sequenceNumber, State state, T message) {
        this.sequenceNumber = sequenceNumber;
        this.state = state;
        this.message = message;
    }

    /**
     * Constructor for new message (without given state)
     *
     * @param sequenceNumber number of the message
     * @param message message to store/stored message
     */
    public StoredMessage(long sequenceNumber, T message) {
        this.sequenceNumber = sequenceNumber;
        this.message = message;
        this.state = State.NEW;
    }

    /**
     * Constructor for missing message
     *
     * @param sequenceNumber number of the message
     */
    public StoredMessage(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        this.state = State.MISSING;
        this.message = null;
    }

    public void setSent()
    {
        this.state = State.SENT;
    }

    public void setOK()
    {
        this.state = State.OK;
    }
}

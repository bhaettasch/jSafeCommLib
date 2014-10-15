package de.gfolder.safeCommLib.message;

import java.util.Date;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class SafeMessage {
    public enum Type {MESSAGE, FEEDBACK}

    private long sequenceNumber;
    private int storedHash;
    private long timeStamp;
    private Type type;
    private String data;

    /**
     * Getter for property 'sequenceNumber'.
     *
     * @return Value for property 'sequenceNumber'.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Getter for property 'storedHash'.
     *
     * @return Value for property 'storedHash'.
     */
    public int getStoredHash() {
        return storedHash;
    }

    /**
     * Getter for property 'timeStamp'.
     *
     * @return Value for property 'timeStamp'.
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Getter for property 'type'.
     *
     * @return Value for property 'type'.
     */
    public Type getType() {
        return type;
    }

    /**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'.
     */
    public String getData() {
        return data;
    }

    /**
     * Constructor
     *
     * @param sequenceNumber sequence number for this message
     * @param data data to send
     */
    public SafeMessage(long sequenceNumber, String data, Type type) {
        this.sequenceNumber = sequenceNumber;
        this.data = data;
        this.type = type;
        this.storedHash = hashCode();
        Date date = new Date();
        this.timeStamp = date.getTime();
    }

    /**
     * Constructor for restoring the received message
     *
     * @param sequenceNumber sequence number for this message
     * @param storedHash original hash (for checking)
     * @param timeStamp original time stamp
     * @param data contained data
     */
    public SafeMessage(long sequenceNumber, int storedHash, long timeStamp, String data, Type type) {
        this.sequenceNumber = sequenceNumber;
        this.storedHash = storedHash;
        this.timeStamp = timeStamp;
        this.data = data;
        this.type = type;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    /**
     * Check whether the current data has still the correct hash value it was send with
     *
     * @return true if stored hash matches current hash, false if not
     */
    public boolean matchingHash()
    {
        return storedHash == hashCode();
    }
}

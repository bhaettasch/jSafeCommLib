package de.gfolder.safeCommLib.message;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class SafeMessageFactory {

    /**
     * Create a new safe message for sending
     *
     * @param sequenceNumber sequence number to use
     * @param data payload to send
     * @return the SafeMessage object containing data and meta information
     */
    public static SafeMessage createSafeMessage(long sequenceNumber, String data)
    {
        return new SafeMessage(sequenceNumber, data, SafeMessage.Type.MESSAGE);
    }

    /**
     * Create a special SafeMessage containing a confirmation for a received one
     *
     * @param sequenceNumber sequence number of the message to acknowledge
     * @return the SafeMessage object containing the acknowledgement
     */
    public static SafeMessage createFeedbackMessageOK(long sequenceNumber)
    {
        return new SafeMessage(sequenceNumber, "OK", SafeMessage.Type.FEEDBACK);
    }

    /**
     * Create a special SafeMessage containing a discarding information for a received one
     *
     * @param sequenceNumber sequence number of the message to discard
     * @return the SafeMessage object containing the discarding information
     */
    public static SafeMessage createFeedbackMessageFailed(long sequenceNumber)
    {
        return new SafeMessage(sequenceNumber, "FAILED", SafeMessage.Type.FEEDBACK);
    }
}

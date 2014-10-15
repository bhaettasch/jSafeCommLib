package de.gfolder.safeCommLib.connector;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public interface MessageReceiver {
    public void receive(String msg, String channelIdentifier);
}

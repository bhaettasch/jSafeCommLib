package de.gfolder.safeCommLib.demo;

import de.gfolder.safeCommLib.connector.BreakupHandler;
import de.gfolder.safeCommLib.connector.MessageReceiver;
import de.gfolder.safeCommLib.server.SafeMessageServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class Server {
    public static void main(String[] args) throws Exception {
        // Configure SSL.
        boolean ssl = System.getProperty("ssl") != null;
        int port = Integer.parseInt(System.getProperty("port", ssl ? "8443" : "8080"));
        String websocketPath = "/websocket";

        SafeMessageServer safeMessageServer = new SafeMessageServer(port, websocketPath, ssl, new MessageReceiver() {
            @Override
            public void receive(String msg, String channelIdentifier) {
                System.out.println("Received message on channel "+channelIdentifier+":");
                System.out.println(msg);
            }
        }, new BreakupHandler() {
            @Override
            public void handleBreakup(String channelIdentifier) {
                System.err.println("Connection of "+channelIdentifier+" closed");
            }
        });
        try {
            safeMessageServer.init();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();

                if (msg == null ||"exit".equals(msg.toLowerCase())) {
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    safeMessageServer.sendPing(safeMessageServer.getFirstChannelIdentifier());
                } else {
                    safeMessageServer.sendMessage(safeMessageServer.getFirstChannelIdentifier(), msg);
                }
            }
            safeMessageServer.requestClose();
        } finally {
            safeMessageServer.close();
        }
    }
}

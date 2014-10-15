package de.gfolder.safeCommLib.demo;

import de.gfolder.safeCommLib.client.SafeMessageClient;
import de.gfolder.safeCommLib.messageHandler.JSONMessageHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * safeCommLib
 *
 * @author Benjamin Haettasch <Benjamin.Haettasch@googlemail.com>
 * @version 0.1
 */

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Creating client");
        SafeMessageClient safeMessageClient = new SafeMessageClient(System.getProperty("url", "ws://127.0.0.1:8080/websocket"), new JSONMessageHandler());
        System.out.println("Starting");

        try {
            safeMessageClient.init();

            System.out.println("Init finished");
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();

                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    safeMessageClient.requestClosing();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    safeMessageClient.sendPing();
                } else {
                    safeMessageClient.sendMessage(msg);
                }
            }
        }finally {
            safeMessageClient.close();
        }
    }
}

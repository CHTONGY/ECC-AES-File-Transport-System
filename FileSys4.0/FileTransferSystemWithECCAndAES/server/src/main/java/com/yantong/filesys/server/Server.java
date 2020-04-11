package com.yantong.filesys.server;

import com.yantong.filesys.foo.constants.TCPConstants;

import java.util.ArrayList;
import java.util.List;

public class Server {
    static BroadcastMessage clientInfoMessage;
    static TCPServer tcpServer;
    public static List<String> serverPromptMessages;

    public void start() {
        serverPromptMessages = new ArrayList<>();
        clientInfoMessage = new BroadcastMessage();
        tcpServer = new TCPServer(TCPConstants.PORT_SERVER, clientInfoMessage);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            serverPromptMessages.add("Start TCP server failed!");
            return;
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);
    }

    public void stop() {
        UDPProvider.stop();
        tcpServer.stop();
        System.exit(0);
    }

    public BroadcastMessage getClientInfoMessage() {
        return clientInfoMessage;
    }
}

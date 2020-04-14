package com.yantong.filesys.server;

import com.yantong.filesys.foo.constants.TCPConstants;

import java.util.ArrayList;
import java.util.List;

public class Server {
    static BroadcastMessage clientInfoMessage;
    static TCPServer tcpServer;
    static TCPFileServer tcpFileServer;

    public static List<String> serverPromptMessages;
    public static String tempFilePath = "/Users/yantong/ApplicationCode/JavaProject/ECCProject/FileTransferSystemWithECCAndAES/server/src/main/java/com/yantong/filesys/server/temp_file/temp.txt";

    public void start() {
        serverPromptMessages = new ArrayList<>();
        clientInfoMessage = new BroadcastMessage();
        tcpServer = new TCPServer(TCPConstants.PORT_SERVER, clientInfoMessage);
        tcpFileServer = new TCPFileServer(TCPConstants.PORT_FILE_SERVER);
        boolean isSucceed = tcpServer.start();
        boolean fileServerSuccess = tcpFileServer.start();
        if (!isSucceed || !fileServerSuccess) {
            serverPromptMessages.add("Start TCP server or TCP File Server failed!");
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

package com.yantong.filesys.client;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.client.bean.ServerInfo;
import com.yantong.filesys.lib.utils.ECCUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public static ServerInfo serverInfo;
    public static ClientInfo clientInfo;
    public static List<String> promptMessages;
    public static String receiveTempFilePath = "/Users/yantong/ApplicationCode/JavaProject/ECCProject/FileTransferSystemWithECCAndAES/client/src/main/java/com/yantong/filesys/client/temp_file/receiveTempFile.txt";
    public static String sendTempFilePath = "/Users/yantong/ApplicationCode/JavaProject/ECCProject/FileTransferSystemWithECCAndAES/client/src/main/java/com/yantong/filesys/client/temp_file/sendTempFile.txt";

    public static void start(String name) {
        promptMessages = new ArrayList<>();
        serverInfo = UDPSearcher.searchServer(10000);
        try {
            clientInfo = initClient(name);
        } catch (Exception e) {
            String prompt = "Fail to Initialize Client";
            promptMessages.add(prompt);
        }

        if (serverInfo != null && clientInfo != null) {
            try {
                TCPClient.linkWith(serverInfo, clientInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ClientInfo initClient(String name) throws IOException {
        // Generate ECC Key
        String eccPublicKeyStr = null;
        String eccPrivateKeyStr = null;
        try {
            KeyPair keyPair = ECCUtils.getKeyPair();
            eccPublicKeyStr = ECCUtils.getPublicKey(keyPair);
            eccPrivateKeyStr = ECCUtils.getPrivateKey(keyPair);
            String prompt = "Generate ECC Key Pair successfully.";
            promptMessages.add(prompt);
        } catch (Exception e) {
            promptMessages.add("Fail to generate ECC Key Pair.");
            e.printStackTrace();
        }

        ClientInfo clientInfo = new ClientInfo(name, eccPublicKeyStr, eccPrivateKeyStr);
        return clientInfo;
    }
}

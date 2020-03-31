package com.yantong.filesys.client;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.client.bean.ServerInfo;
import com.yantong.filesys.lib.utils.ECCUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;

public class Client {
    public static void main(String[] args) {
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + serverInfo);

        ClientInfo clientInfo;
        try {
            clientInfo = initClient();
        } catch (Exception e) {
            System.out.println("Fail to Initialize Client");
            return;
        }

        if (serverInfo != null && clientInfo != null) {
            try {
                TCPClient.linkWith(serverInfo, clientInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static ClientInfo initClient() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter your name:");
        String name = bufferedReader.readLine();

        // Generate ECC Key
        String eccPublicKeyStr = null;
        String eccPrivateKeyStr = null;
        try {
            KeyPair keyPair = ECCUtils.getKeyPair();
            eccPublicKeyStr = ECCUtils.getPublicKey(keyPair);
            eccPrivateKeyStr = ECCUtils.getPrivateKey(keyPair);
            System.out.println("Generate ECC Key Pair successfully.");
        } catch (Exception e) {
            System.out.println("Fail to generate ECC Key Pair.");
            e.printStackTrace();
        }

        ClientInfo clientInfo = new ClientInfo(name, eccPublicKeyStr, eccPrivateKeyStr);
        return clientInfo;
    }
}

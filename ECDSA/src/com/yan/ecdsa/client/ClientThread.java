package com.yan.ecdsa.client;

import com.yan.aes.AESUtil;
import com.yan.ecc.ECCUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.interfaces.ECPrivateKey;

public class ClientThread extends Thread {
    private BufferedReader reader;
    private Client client;

    public ClientThread(Socket socket, Client client) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            JsonObject jsonObject = Json.createReader(reader).readObject();
            if (jsonObject.containsKey("eccPublicKeyStr")) {
                System.out.println("[system]: receive public message ==> " + jsonObject.toString());
                client.setOtherPartyName(jsonObject.getString("name"));
                client.setOtherPartyEccPublicKeyStr(jsonObject.getString("eccPublicKeyStr"));
                if (client.getPrompt() != null) {
                    System.out.println(client.getPrompt());
                }
                client.setReadyFlag(true);
            } else if (jsonObject.containsKey("encryptedAesKeyStr")) {
                try {
                    System.out.println("[system]: receive encrypted message ==> " + jsonObject.toString());
                    String encryptedAesKeyStr = jsonObject.getString("encryptedAesKeyStr");

                    // 使用自己私钥对aes进行解密
                    ECPrivateKey privateKey = ECCUtil.string2PrivateKey(client.getEccPrivateKeyStr());
                    byte[] decryptedAesKey = ECCUtil.privateDecrypt(AESUtil.base642Byte(encryptedAesKeyStr), privateKey);
                    String decryptedAesKeyStr = new String(decryptedAesKey);
                    System.out.println("Encrypted AES Key has been decrypted.");

                    // 使用解密后的aes对加密文件进行解密
                    AESUtil.decryptFile(decryptedAesKeyStr, client.encryption, client.decryption);
                    System.out.println("File has been decrypted.");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

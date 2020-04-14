package com.yantong.filesys.client.handler;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.lib.utils.CloseUtils;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Queue;

public class ReadHandler extends Thread{
    public static Queue<JsonObject> encryptedMessagesList;
    private final InputStream inputStream;
    private boolean done = false;
    private ClientInfo clientInfo;

    public ReadHandler(InputStream inputStream, ClientInfo clientInfo) {
        this.inputStream = inputStream;
        this.clientInfo = clientInfo;
        encryptedMessagesList = new ArrayDeque<>();
    }

    @Override
    public void run() {
        super.run();
        try {
            // 得到输入流，用于接收数据
            BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
            JsonObject jsonObject;
            do {
                try {
                    jsonObject = Json.createReader(socketInput).readObject();
                    if (jsonObject == null) {
                        continue;
                    } else if (jsonObject.containsKey("BroadCastAllClientsInfo")) {
                        JsonArray clientInfos = jsonObject.getJsonArray("BroadCastAllClientsInfo");
                        synchronized (clientInfo) {
                            for (JsonValue jsonValue : clientInfos) {
                                JsonObject otherClientInfo = jsonValue.asJsonObject();
                                System.out.println("[system]: receive public message ==> " + otherClientInfo.toString());
                                if (!clientInfo.getOtherPartyEccPublicKeyMap().containsKey(otherClientInfo.getString("name"))) {
                                    clientInfo.setOtherPartyNames(otherClientInfo.getString("name"));
                                    clientInfo.setOtherPartyEccPublicKeyStrs(otherClientInfo.getString("name"), otherClientInfo.getString("eccPublicKeyStr"));
                                }
                            }
//                                System.out.println("Update neighbors:");
//                                PrintUtils.printList(clientInfo.getOtherPartyNames());
                        }
                    } else if (jsonObject.containsKey("encryptedAesKeyStr")) {
                        synchronized (encryptedMessagesList) {
                            encryptedMessagesList.offer(jsonObject);
                        }
//                            try {
//                                System.out.println("[system]: receive encrypted message ==> " + jsonObject.toString());
//                                String encryptedAesKeyStr = jsonObject.getString("encryptedAesKeyStr");
//
//                                // 使用自己私钥对aes进行解密
//                                ECPrivateKey privateKey = ECCUtils.string2PrivateKey(clientInfo.getEccPrivateKeyStr());
//                                byte[] decryptedAesKey = ECCUtils.privateDecrypt(AESUtils.base642Byte(encryptedAesKeyStr), privateKey);
//                                String decryptedAesKeyStr = new String(decryptedAesKey);
//                                System.out.println("Encrypted AES Key has been decrypted.");
//
//                                // 使用解密后的aes对加密文件进行解密
//                                AESUtils.decryptFile(decryptedAesKeyStr, clientInfo.getEncryption(), clientInfo.getDecryption());
//                                System.out.println("File has been decrypted.");
//                                System.out.println("You can send files t neighbor(s), please choose one:");
//                            } catch (Exception e) {
//                                System.out.println("Not send to me. Can not decrypt the key.");
//                                System.out.println("You can send files t neighbor(s), please choose one:");
////                                e.printStackTrace();
//                            }
                    }
                } catch (Exception e) {
//                        e.printStackTrace();
                    continue;
                }
            } while (!done);
        } catch (Exception e) {
            if (!done) {
                System.out.println("连接异常断开：" + e.getMessage());
            }
            e.printStackTrace();
        } finally {
            // 连接关闭
            CloseUtils.close(inputStream);
        }
    }

    void exit() {
        done = true;
        CloseUtils.close(inputStream);
    }
}

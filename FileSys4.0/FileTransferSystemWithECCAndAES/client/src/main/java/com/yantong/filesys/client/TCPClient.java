package com.yantong.filesys.client;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.client.bean.ServerInfo;
import com.yantong.filesys.lib.utils.AESUtils;
import com.yantong.filesys.lib.utils.CloseUtils;
import com.yantong.filesys.lib.utils.ECCUtils;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayDeque;
import java.util.Queue;

public class TCPClient {
    public static Socket socket;

    public static void linkWith(ServerInfo serverInfo, ClientInfo clientInfo) throws IOException {
        socket = new Socket();
        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);

        Client.promptMessages.add("已发起服务器TCP连接.");
        Client.promptMessages.add("客户端信息：" + socket.getLocalAddress() + " P:" + socket.getLocalPort());
        Client.promptMessages.add("服务器信息：" + socket.getInetAddress() + " P:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream(), clientInfo);
            readHandler.start();
            // 发送接收数据
            broadcastClientInitInfo(socket, clientInfo);
        } catch (Exception e) {
            Client.promptMessages.add("异常关闭");
            e.printStackTrace();
        }
    }

    public static void encryptAndSendFile(String name, String originFilePath) throws IOException {
        ClientInfo clientInfo = Client.clientInfo;
        PrintStream socketPrintStream = new PrintStream(socket.getOutputStream());
        if (!clientInfo.getOtherPartyNames().isEmpty()) {
            String otherPartyEccPublicKeyStr = clientInfo.getOtherPartyEccPublicKeyStr(name);
            if (otherPartyEccPublicKeyStr == null) {
                Client.promptMessages.add("The name is not in the list. Please try again.");
                return;
            }
            try {
                // 1. Create AES Key and encrypt file
                String aesKey = AESUtils.genKeyAES();
                File originFile = new File(originFilePath);
                AESUtils.encryptFile(aesKey, originFile, clientInfo.getEncryption());
                Client.promptMessages.add("File has been encrypted.");
                // 2. 使用对方公钥对密钥进行加密
                ECPublicKey otherPartyEccPublicKey = ECCUtils.string2PublicKey(otherPartyEccPublicKeyStr);
                byte[] encryptedAesKey = ECCUtils.publicEncrypt(aesKey.getBytes(), otherPartyEccPublicKey);
                String encryptedAesKeyStr = AESUtils.byte2Base64(encryptedAesKey);
                Client.promptMessages.add("AES Key has been encrypted.");
                // 3. 发送加密后的AES
                StringWriter stringWriter = new StringWriter();
                Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                        .add("name", clientInfo.getName())
                        .add("encryptedAesKeyStr", encryptedAesKeyStr)
                        .build());
                Client.promptMessages.add("Successfully send!");
                Client.promptMessages.add("[" + clientInfo.getName() + "] send to [" + name + "]: " + stringWriter.toString());
                socketPrintStream.println(stringWriter);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void receiveAndDecryptFile(String savePath) {
        JsonObject jsonObject;
        ClientInfo clientInfo = Client.clientInfo;
        synchronized (ReadHandler.encryptedMessagesList) {
            jsonObject = ReadHandler.encryptedMessagesList.poll();
        }
        try {
            Client.promptMessages.add("[system]: receive encrypted message ==> " + jsonObject.toString());
            String encryptedAesKeyStr = jsonObject.getString("encryptedAesKeyStr");

            // 使用自己私钥对aes进行解密
            ECPrivateKey privateKey = ECCUtils.string2PrivateKey(clientInfo.getEccPrivateKeyStr());
            byte[] decryptedAesKey = ECCUtils.privateDecrypt(AESUtils.base642Byte(encryptedAesKeyStr), privateKey);
            String decryptedAesKeyStr = new String(decryptedAesKey);
            Client.promptMessages.add("Encrypted AES Key has been decrypted.");

            // 使用解密后的aes对加密文件进行解密
            File decryptedFile = new File(savePath);
            AESUtils.decryptFile(decryptedAesKeyStr, clientInfo.getEncryption(), decryptedFile);
            Client.promptMessages.add("File has been decrypted.");
        } catch (Exception e) {
            Client.promptMessages.add("Not send to me. Can not decrypt the key.");
//          e.printStackTrace();
        }
    }

    private static void broadcastClientInitInfo(Socket client, ClientInfo clientInfo) throws IOException {
        // 得到Socket输出流，并转换为打印流
        PrintStream socketPrintStream = new PrintStream(client.getOutputStream());

        // 发送初始化信息
        StringWriter stringWriter = new StringWriter();
        System.out.println("Broadcast public information.");
        Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                .add("name", clientInfo.getName())
                .add("eccPublicKeyStr", clientInfo.getEccPublicKeyStr())
                .build());
        socketPrintStream.println(stringWriter);

        if (clientInfo.getOtherPartyNames().isEmpty()) {
            System.out.println("Has not receive other client's info, please wait.");
        }
        // 资源释放
        //socketPrintStream.close();
    }

    static class ReadHandler extends Thread {
        public static Queue<JsonObject> encryptedMessagesList;
        private final InputStream inputStream;
        private boolean done = false;
        private ClientInfo clientInfo;

        ReadHandler(InputStream inputStream, ClientInfo clientInfo) {
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
}

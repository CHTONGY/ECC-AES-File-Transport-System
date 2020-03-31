package com.yantong.filesys.client;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.client.bean.ServerInfo;
import com.yantong.filesys.lib.utils.AESUtils;
import com.yantong.filesys.lib.utils.CloseUtils;
import com.yantong.filesys.lib.utils.ECCUtils;
import com.yantong.filesys.lib.utils.PrintUtils;

import javax.json.*;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class TCPClient {
    public static void linkWith(ServerInfo serverInfo, ClientInfo clientInfo) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);

        System.out.println("已发起服务器TCP连接.");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " P:" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + " P:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream(), clientInfo);
            readHandler.start();

            // 发送接收数据
            write(socket, clientInfo);

            // 退出操作
            readHandler.exit();
        } catch (Exception e) {
            System.out.println("异常关闭");
            e.printStackTrace();
        }


//         释放资源
        socket.close();
        System.out.println("客户端已退出～");
    }

    private static void write(Socket client, ClientInfo clientInfo) throws IOException {
        // 构建键盘输入流
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

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

        while (true) {
            if (!clientInfo.getOtherPartyNames().isEmpty()) {
                System.out.println("You can send files t neighbor(s), please choose one:");
//                PrintUtils.printList(clientInfo.getOtherPartyNames());
                String name = bufferedReader.readLine();

                if(name.equals(clientInfo.getName())) {
                    System.out.println("Can not choose yourself.Please choose other party.");
                    continue;
                }
                String otherPartyEccPublicKeyStr = clientInfo.getOtherPartyEccPublicKeyStr(name);
                if (otherPartyEccPublicKeyStr == null) {
                    System.out.println("The name is not in the list. Please try again.");
                    continue;
                }

                try {
                    System.out.println("Successfully choose a client. Shall we encrypt file now? Y:es or N:o");
                    String[] values = bufferedReader.readLine().split(" ");
                    if (values[0].equalsIgnoreCase("y")) {
                        // 1. Create AES Key and encrypt file
                        String aesKey = AESUtils.genKeyAES();
                        AESUtils.encryptFile(aesKey, clientInfo.getOrigin(), clientInfo.getEncryption());
                        System.out.println("File has been encrypted.");
                        // 2. 使用对方公钥对密钥进行加密
                        ECPublicKey otherPartyEccPublicKey = ECCUtils.string2PublicKey(otherPartyEccPublicKeyStr);
                        byte[] encryptedAesKey = ECCUtils.publicEncrypt(aesKey.getBytes(), otherPartyEccPublicKey);
                        String encryptedAesKeyStr = AESUtils.byte2Base64(encryptedAesKey);
                        System.out.println("AES Key has been encrypted.");
                        // 3. 发送加密后的AES
                        stringWriter = new StringWriter();
                        Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                                .add("name", clientInfo.getName())
                                .add("encryptedAesKeyStr", encryptedAesKeyStr)
                                .build());
                        System.out.println("[" + clientInfo.getName() + "] send to [" + name + "]: " + stringWriter.toString());
                        socketPrintStream.println(stringWriter);
                    } else if (values[0].equalsIgnoreCase("n")) {
                        System.exit(0);
                    } else {
                        System.out.println("Invalid input.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if ("00bye00".equalsIgnoreCase(name)) {
                    break;
                }
            }
        }

        // 资源释放
        socketPrintStream.close();
    }

    static class ReadHandler extends Thread {
        private final InputStream inputStream;
        private boolean done = false;
        private ClientInfo clientInfo;

        ReadHandler(InputStream inputStream, ClientInfo clientInfo) {
            this.inputStream = inputStream;
            this.clientInfo = clientInfo;
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
                        } else if(jsonObject.containsKey("BroadCastAllClientsInfo")){
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
                                System.out.println("Update neighbors:");
                                PrintUtils.printList(clientInfo.getOtherPartyNames());
                            }
                        } else if (jsonObject.containsKey("encryptedAesKeyStr")) {
                            try {
                                System.out.println("[system]: receive encrypted message ==> " + jsonObject.toString());
                                String encryptedAesKeyStr = jsonObject.getString("encryptedAesKeyStr");

                                // 使用自己私钥对aes进行解密
                                ECPrivateKey privateKey = ECCUtils.string2PrivateKey(clientInfo.getEccPrivateKeyStr());
                                byte[] decryptedAesKey = ECCUtils.privateDecrypt(AESUtils.base642Byte(encryptedAesKeyStr), privateKey);
                                String decryptedAesKeyStr = new String(decryptedAesKey);
                                System.out.println("Encrypted AES Key has been decrypted.");

                                // 使用解密后的aes对加密文件进行解密
                                AESUtils.decryptFile(decryptedAesKeyStr, clientInfo.getEncryption(), clientInfo.getDecryption());
                                System.out.println("File has been decrypted.");
                                System.out.println("You can send files t neighbor(s), please choose one:");
                            } catch (Exception e) {
                                System.out.println("Not send to me. Can not decrypt the key.");
                                System.out.println("You can send files t neighbor(s), please choose one:");
//                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
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

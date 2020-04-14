package com.yantong.filesys.client;

import com.yantong.filesys.client.bean.ClientInfo;
import com.yantong.filesys.client.bean.ServerInfo;
import com.yantong.filesys.client.handler.FileReadHandler;
import com.yantong.filesys.client.handler.ReadHandler;
import com.yantong.filesys.foo.constants.TCPConstants;
import com.yantong.filesys.lib.utils.AESUtils;
import com.yantong.filesys.lib.utils.ECCUtils;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class TCPClient {
    public static Socket socket;
    public static Socket fileSocket;

    public static void linkWith(ServerInfo serverInfo, ClientInfo clientInfo) throws IOException {
        socket = new Socket();
        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);
        Client.promptMessages.add("已发起服务器TCP连接.");
        Client.promptMessages.add("客户端信息：" + socket.getLocalAddress() + " P:" + socket.getLocalPort());
        Client.promptMessages.add("服务器信息：" + socket.getInetAddress() + " P:" + socket.getPort());

        fileSocket = new Socket();
//        fileSocket.setSoTimeout(3000);
        fileSocket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), TCPConstants.PORT_FILE_SERVER), 3000);
        Client.promptMessages.add("已发起文件服务器TCP连接.");
        Client.promptMessages.add("文件服务器信息：" + fileSocket.getInetAddress() + " P:" + fileSocket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream(), clientInfo);
            readHandler.start();
            FileReadHandler fileReadHandler = new FileReadHandler(fileSocket.getInputStream(), Client.receiveTempFilePath);
            fileReadHandler.start();
            // 发送初始化信息
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
                File encryptedFile = new File(Client.sendTempFilePath);
                AESUtils.encryptFile(aesKey, originFile, encryptedFile);
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
                Client.promptMessages.add("Successfully send Keys!");
                Client.promptMessages.add("[" + clientInfo.getName() + "] send to [" + name + "]: " + stringWriter.toString());
                socketPrintStream.println(stringWriter);
                sendFile(Client.sendTempFilePath);
                Client.promptMessages.add("The original file is " + originFile.length() + "byte");
                Client.promptMessages.add("The encrypted file is " + encryptedFile.length() + "byte");
                Client.promptMessages.add("Successfully send encrypted file!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFile(String sendTempFilePath) {
        try {
            InputStream fileInputStream = new FileInputStream(sendTempFilePath);
            DataInputStream dis = new DataInputStream(fileInputStream);
            DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
            byte[] buf = new byte[1024 * 9];
            int len = 0;
//            while((len = dis.read(buf)) != -1) {
//                dos.write(buf, 0, len);
//            }
            if((len = dis.read(buf)) != -1) {
                dos.write(buf, 0, len);
            }
            dos.flush();
//            fileInputStream.reset();
//            dis.close();
//            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
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
            File tempFile = new File(Client.receiveTempFilePath);
            AESUtils.decryptFile(decryptedAesKeyStr, tempFile, decryptedFile);
            Client.promptMessages.add("File has been decrypted.");
            Client.promptMessages.add("Received encrypted file is " + tempFile.length() + "byte");
            Client.promptMessages.add("Decrypted file is " + decryptedFile.length() + "byte");
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
}

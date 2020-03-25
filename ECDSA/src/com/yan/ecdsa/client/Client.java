package com.yan.ecdsa.client;

import com.yan.aes.AESUtil;
import com.yan.ecc.ECCUtil;

import javax.json.Json;
import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

public class Client {
    File origin = new File("/Users/yantong/Desktop/plaintext.txt");     // origin file
    File encryption = new File("/Users/yantong/Desktop/encrypt.txt");   // encryption file
    File decryption = new File("/Users/yantong/Desktop/decrypt.txt");   // decryption file
    private String name;    // this user's name
    private String otherPartyName;  // other user's name
    private PrintWriter printWriter;
    private String prompt;  // prompt message
    private String aesKey;  // This user's AES Key to encrypt a file
    private String eccPublicKeyStr; // This user's ECC Public Key in String format
    private String eccPrivateKeyStr;    // This user's ECC Private Key in String format
    private String otherPartyEccPublicKeyStr;   // Other user's ECC Public Key in String format
    private boolean readyFlag;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        Socket socket = new Socket("localhost", 4444);
//        Socket socket = new Socket();
//        socket.setSoTimeout(3000);
//        socket.connect(new InetSocketAddress(socket.getLocalAddress(), socket.getPort()), 3000);
        new ClientThread(socket, client).start();
        client.printWriter = new PrintWriter(socket.getOutputStream(), true);
        client.initInfo();
        client.encryptAndSendMessage();
    }

    private void initInfo() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        // Get user's name
        prompt = "[system]: enter name";
        System.out.println(prompt);
        String values = bufferedReader.readLine();
        this.name = values;

        // Generate ECC Key
        try {
            KeyPair keyPair = ECCUtil.getKeyPair();
            this.eccPublicKeyStr = ECCUtil.getPublicKey(keyPair);
            this.eccPrivateKeyStr = ECCUtil.getPrivateKey(keyPair);
            System.out.println("Generate ECC Key Pair successfully.");
        } catch (Exception e) {
            System.out.println("Fail to generate ECC Key Pair.");
            e.printStackTrace();
        }

        // Broadcast public information
        StringWriter stringWriter = new StringWriter();
        System.out.println("Broadcast public information.");
        Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                .add("name", this.name)
                .add("eccPublicKeyStr", this.eccPublicKeyStr)
                .build());
        printWriter.println(stringWriter);
    }

    private void encryptAndSendMessage() {
        if (!readyFlag) {
            prompt = "[system]: wait for other party's public key";
            System.out.println(prompt);
        }
        while (true) {
            if (readyFlag) {
                try {
                    System.out.println("Shall we encrypt file now? Y:es or N:o");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                    String[] values = bufferedReader.readLine().split(" ");
                    if (values[0].equalsIgnoreCase("y")) {
                        // 1. Create AES Key and encrypt file
                        this.aesKey = AESUtil.genKeyAES();
                        AESUtil.encryptFile(this.aesKey, origin, encryption);
                        System.out.println("File has been encrypted.");
                        // 2. 使用对方公钥对密钥进行加密
                        ECPublicKey otherPartyEccPublicKey = ECCUtil.string2PublicKey(otherPartyEccPublicKeyStr);
                        byte[] encryptedAesKey = ECCUtil.publicEncrypt(this.aesKey.getBytes(), otherPartyEccPublicKey);
                        String encryptedAesKeyStr = AESUtil.byte2Base64(encryptedAesKey);
                        System.out.println("AES Key has been encrypted.");
                        // 3. 发送加密后的AES
                        StringWriter stringWriter = new StringWriter();
                        Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                                .add("name", this.name)
                                .add("encryptedAesKeyStr", encryptedAesKeyStr)
                                .build());
                        System.out.println("[" + name + "]: send " + stringWriter.toString());
                        printWriter.println(stringWriter);
                    } else if (values[0].equalsIgnoreCase("n")) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /********** Getter & Setter***********/
    public String getPrompt() {
        return prompt;
    }

    public String getName() {
        return name;
    }

    public String getOtherPartyName() {
        return otherPartyName;
    }

    public void setOtherPartyName(String otherPartyName) {
        this.otherPartyName = otherPartyName;
    }

    public String getEccPublicKeyStr() {
        return eccPublicKeyStr;
    }

    public String getOtherPartyEccPublicKeyStr() {
        return otherPartyEccPublicKeyStr;
    }

    public void setOtherPartyEccPublicKeyStr(String otherPartyEccPublicKeyStr) {
        this.otherPartyEccPublicKeyStr = otherPartyEccPublicKeyStr;
    }

    public String getEccPrivateKeyStr() {
        return eccPrivateKeyStr;
    }

    public void setReadyFlag(boolean readyFlag) {
        this.readyFlag = readyFlag;
    }

    /**************************************/


}

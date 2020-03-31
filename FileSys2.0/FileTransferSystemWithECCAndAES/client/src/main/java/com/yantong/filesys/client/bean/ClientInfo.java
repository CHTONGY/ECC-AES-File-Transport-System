package com.yantong.filesys.client.bean;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientInfo {
    private final String name;    // this user's name
    private final String eccPublicKeyStr; // This user's ECC Public Key in String format
    private final String eccPrivateKeyStr;    // This user's ECC Private Key in String format
    File origin = new File("/Users/yantong/Desktop/plaintext.txt");     // origin file
    File encryption = new File("/Users/yantong/Desktop/encrypt.txt");   // encryption file
    File decryption = new File("/Users/yantong/Desktop/decrypt.txt");   // decryption file
    private List<String> otherPartyNames;  // other user's name
    private Map<String, String> otherPartyEccPublicKeyStrs;   // <Other's Name, Other's EccPublicKeyStr>

    public ClientInfo(String name, String eccPublicKeyStr, String eccPrivateKeyStr) {
        this.name = name;
        this.eccPublicKeyStr = eccPublicKeyStr;
        this.eccPrivateKeyStr = eccPrivateKeyStr;
        otherPartyNames = new ArrayList<>();
        otherPartyEccPublicKeyStrs = new HashMap<>();
    }

    public File getOrigin() {
        return origin;
    }

    public File getEncryption() {
        return encryption;
    }

    public File getDecryption() {
        return decryption;
    }

    public void setOtherPartyEccPublicKeyStrs(String name, String otherPartyEccPublicKeyStr) {
        synchronized (otherPartyEccPublicKeyStrs) {
            this.otherPartyEccPublicKeyStrs.put(name, otherPartyEccPublicKeyStr);
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getOtherPartyNames() {
        synchronized (otherPartyNames) {
            return otherPartyNames;
        }
    }

    public void setOtherPartyNames(String otherPartyName) {
        synchronized (otherPartyName) {
            this.otherPartyNames.add(otherPartyName);
        }
    }

    public String getEccPublicKeyStr() {
        return eccPublicKeyStr;
    }

    public String getEccPrivateKeyStr() {
        return eccPrivateKeyStr;
    }

    public String getOtherPartyEccPublicKeyStr(String otherPartyName) {
        synchronized (otherPartyEccPublicKeyStrs) {
            return otherPartyEccPublicKeyStrs.get(otherPartyName);
        }
    }
}

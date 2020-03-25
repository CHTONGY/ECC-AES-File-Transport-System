package com.yan.ecdsa.server;


import javax.json.Json;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {
    private Server server;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    public ServerThread(Socket socket, Server server) throws IOException {
        this.server = server;
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        JsonObject jsonObject;
        try {
            while (true) {
                jsonObject = Json.createReader(bufferedReader).readObject();
                System.out.println("\n[System]: " + jsonObject.toString());
                server.forwardMessage(jsonObject.toString(), this);
                if (jsonObject.containsKey("eccPublicKeyStr")) {
                    System.out.println("[Eve]: have ECC Public Key for " + jsonObject.getString("name"));
                } else if (jsonObject.containsKey("encryptedAesKeyStr")) {
                    System.out.println("[Eve]: get encrypted AES Key from" + jsonObject.getString("name"));
                    System.out.println("Cannot decrypt it because do not have the private key");
                }
            }
        } catch (Exception e) {
            server.getServerThreads().remove(this);
        }
    }

    void forwardMessage(String message) {
        printWriter.println(message);
    }
}

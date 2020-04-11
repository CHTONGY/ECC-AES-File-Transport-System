package com.yantong.filesys.server.handle;

import com.yantong.filesys.lib.utils.CloseUtils;
import com.yantong.filesys.server.BroadcastMessage;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfoPrompt;
    private BroadcastMessage clientInfoMessage;

    public ClientHandler(Socket socket, ClientHandlerCallback clientHandlerCallback, BroadcastMessage clientInfoMessage) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream(), clientInfoMessage);
        this.writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfoMessage = clientInfoMessage;
        this.clientInfoPrompt = "A[" + socket.getInetAddress().getHostAddress() + "] P[" + socket.getPort() + "]";
        System.out.println("新客户端连接：" + clientInfoPrompt);
    }

    public String getClientInfoPrompt() {
        return clientInfoPrompt;
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socket);
        System.out.println("客户端已退出：" + socket.getInetAddress() +
                " P:" + socket.getPort());
    }

    public void send(Object str) {
        writeHandler.send(str);
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        void onSelfClosed(ClientHandler handler);

        void onNewMessageArrived(ClientHandler handler, String msg);
        void onNewMessageArrived(ClientHandler handler, JsonObject jsonObject);

        void onNewClientArrived(ClientHandler handler, BroadcastMessage clientInfoMessage);
    }

    class ClientReadHandler extends Thread {
        private final InputStream inputStream;
        private BroadcastMessage clientInfoMessage;
        private boolean done = false;

        ClientReadHandler(InputStream inputStream, BroadcastMessage clientInfoMessage) {
            this.inputStream = inputStream;
            this.clientInfoMessage = clientInfoMessage;
        }

        @Override
        public void run() {
            super.run();
            try {
                // 得到输入流，用于接收数据
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                JsonObject jsonObject;
                do {
                    // 客户端拿到一条数据
                    jsonObject = Json.createReader(bufferedReader).readObject();
                    if (jsonObject == null) {
                        System.out.println("客户端已无法读取数据！");
                        // 退出当前客户端
                        ClientHandler.this.exitBySelf();
                        break;
                    }
                    System.out.println("Receive message from client.");
                    if(jsonObject.containsKey("eccPublicKeyStr")) {
                        synchronized (clientInfoMessage) {
                            clientInfoMessage.addClientPublicInfo(jsonObject);
                            clientHandlerCallback.onNewClientArrived(ClientHandler.this, clientInfoMessage);
                        }
                    } else {
                        clientHandlerCallback.onNewMessageArrived(ClientHandler.this, jsonObject);
                    }
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开");
                    ClientHandler.this.exitBySelf();
                    e.printStackTrace();
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

    class ClientWriteHandler {
        private final PrintStream printStream;
        private final ExecutorService executorService;
        private boolean done = false;

        ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        void send(Object str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final Object msg;

            WriteRunnable(Object msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

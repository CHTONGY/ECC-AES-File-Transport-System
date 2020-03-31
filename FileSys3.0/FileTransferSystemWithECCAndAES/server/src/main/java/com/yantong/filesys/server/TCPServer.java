package com.yantong.filesys.server;

import com.yantong.filesys.server.handle.ClientHandler;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private final ExecutorService forwardingThreadPoolExecutor;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private BroadcastMessage clientInfoMessage;

    public TCPServer(int port, BroadcastMessage clientInfoMessage) {
        this.port = port;
        this.clientInfoMessage = clientInfoMessage;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port, clientInfoMessage);
            mListener = listener;
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }

            clientHandlerList.clear();
        }

        forwardingThreadPoolExecutor.shutdownNow();
    }

    synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, String msg) {
        System.out.println("Received-:" + handler.getClientInfoPrompt() + ": " + msg);

        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler.equals(handler)) {
                        continue;
                    }

                    clientHandler.send(msg);
                }
            }
        });
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, JsonObject jsonObject) {
        System.out.println("Received-:" + handler.getClientInfoPrompt() + ": " + jsonObject.toString());

        StringWriter stringWriter = new StringWriter();
        Json.createWriter(stringWriter).writeObject(jsonObject);

        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler.equals(handler)) {
                        continue;
                    }

                    clientHandler.send(stringWriter);
                }
            }
        });
    }

    @Override
    public void onNewClientArrived(ClientHandler handler, BroadcastMessage clientInfoMessage) {
        System.out.println("Received new client.");
        System.out.println("All clients message:");
        for (JsonObject jsonObject : clientInfoMessage.getClientPublicInfoList()) {
            System.out.println("\t" + handler.getClientInfoPrompt() + ": " + jsonObject.toString());
        }

        StringWriter stringWriter = new StringWriter();
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
//                    if (clientHandler.equals(handler)) {
//                        continue;
//                    }

                    Json.createWriter(stringWriter).writeObject(Json.createObjectBuilder()
                            .add("BroadCastAllClientsInfo", clientInfoMessage.toJsonArrayBuilder())
                            .build());

                    clientHandler.send(stringWriter);
                }
            }
        });


    }

    private class ClientListener extends Thread {
        private ServerSocket server;
        private BroadcastMessage clientInfoMessage;
        private boolean done = false;

        private ClientListener(int port, BroadcastMessage clientInfoMessage) throws IOException {
            server = new ServerSocket(port);
            this.clientInfoMessage = clientInfoMessage;
            System.out.println("服务器信息：" + server.getInetAddress() + " P:" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                Socket client;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    continue;
                }
                try {
                    // 客户端构建异步线程
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this, clientInfoMessage);
                    // 读取数据并打印
                    clientHandler.readToPrint();
                    synchronized (TCPServer.this) {
                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常：" + e.getMessage());
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

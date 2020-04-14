package com.yantong.filesys.server;

import com.yantong.filesys.server.handle.FileHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPFileServer implements FileHandler.FileHandlerCallback {
    private final int port;
    private final ExecutorService forwardingThreadPoolExecutor;
    private List<FileHandler> fileHandlerList;

    public TCPFileServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
        fileHandlerList = new ArrayList<>();
    }

    public boolean start() {
        try {
            FileListener listener = new FileListener(port);
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onNewFileArrived(FileHandler handler, String tempFilePath) {
        System.out.println("Received file!");

        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPFileServer.this) {
                for(FileHandler fileHandler : fileHandlerList) {
                    if(fileHandler.equals(handler)) {
                        continue;
                    }
                    fileHandler.sendFile(tempFilePath);
                }
            }
        });
    }

    private class FileListener extends Thread {
        private ServerSocket serverSocket;

        private FileListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            Server.serverPromptMessages.add("文件服务器信息：" + serverSocket.getInetAddress() + " P:" + serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            Server.serverPromptMessages.add("文件服务器准备就绪～");
            while (true) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    continue;
                }

                // 构建异步线程
                try {
                    FileHandler fileHandler = new FileHandler(socket, TCPFileServer.this, Server.tempFilePath);
                    fileHandler.receive();
                    synchronized (TCPFileServer.this) {
                        fileHandlerList.add(fileHandler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

package com.yantong.filesys.server.handle;

import java.io.*;
import java.net.Socket;

public class FileHandler {
    private final Socket socket;
    private final FileHandlerCallback fileHandlerCallback;
    private final FileReadHandler fileReadHandler;
    private final FileWriteHandler fileWriteHandler;
    private final String tempFilePath;

    public FileHandler(Socket socket, FileHandlerCallback fileHandlerCallback, String tempFilePath) throws IOException {
        this.socket = socket;
        this.fileHandlerCallback = fileHandlerCallback;
        this.tempFilePath = tempFilePath;
        this.fileReadHandler = new FileReadHandler(socket.getInputStream(), this.tempFilePath);
        this.fileWriteHandler = new FileWriteHandler(socket.getOutputStream());
    }

    public void receive() {
        fileReadHandler.start();
    }

    public void sendFile(String tempFilePath) {
        fileWriteHandler.send(tempFilePath);
    }

    public interface FileHandlerCallback {
        void onNewFileArrived(FileHandler handler, String tempFilePath);
    }

    class FileReadHandler extends Thread {
        private final InputStream inputStream;
        private final String tempFilePath;
        private DataInputStream dis;
        private DataOutputStream dos;

        FileReadHandler(InputStream inputStream, String tempFilePath) throws FileNotFoundException {
            this.inputStream = inputStream;
            this.tempFilePath = tempFilePath;
            this.dis = new DataInputStream(inputStream);
            this.dos = new DataOutputStream(new FileOutputStream(tempFilePath));
        }

        @Override
        public void run() {
            super.run();
            while(true) {
                try {
//                    DataInputStream dis = new DataInputStream(inputStream);
//                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFilePath));
                    byte[] buf = new byte[1024 * 9];
                    int len = 0;
//                    while((len = dis.read(buf)) != -1) {
//                        dos.write(buf, 0, len);
//                    }
                    if ((len = dis.read(buf)) != -1) {
                        dos.write(buf, 0, len);
                    }
                    dos.flush();
                    fileHandlerCallback.onNewFileArrived(FileHandler.this, tempFilePath);
//                    dis.close();
//                    dos.close();
//                    inputStream.reset();

                } catch (Exception e) {
                    e.printStackTrace();
//                    continue;
                }
            }
        }
    }

    class FileWriteHandler extends Thread {
        private final OutputStream outputStream;
//        private final ExecutorService executorService;
        private DataInputStream dis;
        private DataOutputStream dos;

        FileWriteHandler(OutputStream outputStream) {
            this.outputStream = outputStream;
//            this.executorService = Executors.newSingleThreadExecutor();
        }

        void send(String tempFilePath) {
            try {
                DataInputStream dis = new DataInputStream(new FileInputStream(tempFilePath));
                DataOutputStream dos = new DataOutputStream(outputStream);
                byte[] buf = new byte[1024 * 9];
                int len = 0;
//                while((len = dis.read(buf)) != -1) {
//                    dos.write(buf, 0, len);
//                }
                if((len = dis.read(buf)) != -1) {
                    dos.write(buf, 0, len);
                }
                dos.flush();
//                dis.close();
//                dos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

package com.yantong.filesys.client.handler;

import java.io.*;

public class FileReadHandler extends Thread {
    private final InputStream inputStream;
    private String fileTempPath;
    private DataInputStream dis;
    private DataOutputStream dos;

    public FileReadHandler(InputStream inputStream, String fileTempPath) throws FileNotFoundException {
        this.inputStream = inputStream;
        this.fileTempPath = fileTempPath;
        this.dis = new DataInputStream(inputStream);
        this.dos = new DataOutputStream(new FileOutputStream(fileTempPath));
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            try {
//                DataInputStream dis = new DataInputStream(inputStream);
//                DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileTempPath));
                byte[] buf = new byte[1024 * 9];
                int len = 0;
                if ((len = dis.read(buf)) != -1) {
                    dos.write(buf, 0, len);
                }
                dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
//                continue;
            }
        }
    }
}

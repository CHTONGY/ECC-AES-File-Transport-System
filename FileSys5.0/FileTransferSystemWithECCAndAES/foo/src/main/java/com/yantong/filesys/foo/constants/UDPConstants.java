package com.yantong.filesys.foo.constants;

public class UDPConstants {
    // 公用头部
    public static final byte[] HEADER = new byte[]{7,7,7,7,9,9,9,9};
    // Server端UDP接收端口
    public static final int PORT_SERVER = 30201;
    // Client端UDP接收端口
    public static final int PORT_CLIENT_RESPONSE = 30202;
}

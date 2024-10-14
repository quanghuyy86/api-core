package vn.vnpay.netty;

public class NettyConfig {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        new NettyServer(port).start();
    }
}


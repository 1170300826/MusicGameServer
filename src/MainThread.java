import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainThread extends Thread{
    public static final int CLIENT_PORT = 9999;
    public static final Object lock = new Object(); //主线程访问的锁

    public static MainThread mainThread ;
    boolean flag = true;
    ServerSocket ss;

    public int clientCount = 0;
    public ArrayList<ServerAgent> listClient = new ArrayList<>();

    @Override
    public void run() {
        try {
            ss = new ServerSocket(CLIENT_PORT);
            System.out.println("监听到9999端口....");

        } catch(Exception e) {
            e.printStackTrace();
        }
        while(flag) {
            try {
                Socket sc = ss.accept();
                System.out.println(sc.getInetAddress()+" 已经连接");
                clientCount += 1;
                //建立新的serveAgent进行处理用户相关的处理
                ServerAgent serverAgent = new ServerAgent(sc);
                serverAgent.start();
                listClient.add(serverAgent);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if(mainThread==null) {
            mainThread = new MainThread();
            mainThread.start();       //开启总线程MainThread
        }
    }

}

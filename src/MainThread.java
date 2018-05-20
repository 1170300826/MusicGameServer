
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainThread extends Thread{
    public static final int CLIENT_PORT = 9999;
    public static final Object lock = new Object(); //主线程访问的锁

    public static MainThread mainThread ;
    //public static int clientCount = 0;
    public static int clientGlobalClock = 0;        //产生标识client的时间签生成器
    public static int sessionGlobalClock = 0;       //产生标识房间号的时间签生成器
    //public static Map<String,Integer> sessionPWDtoID = new HashMap<String,Integer>();   //建立房间密码到房间标识号的映射
    public static Map<String,ArrayList<ServerAgent>> SSIDtoCLIENTSA = new HashMap<>();    //建立由sessionID到一个房间内部所有管理线程的映射
    //public static Map<Integer,Integer> CIDtoSSID = new HashMap<>();    //建立由clockID到sessinID的映射

    Random random = new Random();

    boolean flag = true;
    ServerSocket ss;

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
                //建立新的serveAgent进行处理用户相关的处理
                ServerAgent serverAgent = new ServerAgent(sc);
                serverAgent.start();
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

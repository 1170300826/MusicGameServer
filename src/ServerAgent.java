import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

//单独一个client的thread用来处理相关的处理操作
public class ServerAgent extends Thread{
    boolean flag = true;
    Socket sc;
    DataInputStream din;
    DataOutputStream dout;

    public ServerAgent (Socket sc) {
        this.sc = sc;
        try{
            din = new DataInputStream(sc.getInputStream());
            dout = new DataOutputStream(sc.getOutputStream());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        while(flag) {
            try {
                System.out.println("1");
                String msg = din.readUTF();
                System.out.println("2");
                System.out.println(msg);
            } catch(Exception e) {
                //已经退出了当前的serverAgent
                System.out.println("客户端已经退出");
                // TODO: 2018/5/16 需要在这里进行广播
                //e.printStackTrace();
                flag = false;
            }
        }
    }
}

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

//单独一个client的thread用来处理相关的处理操作
//这里对于每一个与server连接的client分配一个serverAgent处理线程
// ，在线程中的din与dout是建立两者之间独特通信的标识
//返回错误记录表：
// type1代表leader的密码与之前的密码产生重复
// type2代表teamate的密码不能与正在创建的房间所匹配

public class ServerAgent extends Thread{
    public static final int CLIENTYPE_LEANDER = 111;
    public static final int CLIENTYPE_NOARMAL = 222;
    public int isLeader;   //是否是房主
    public int clockID;    //具体的clock标识 用来返回给客户端 用来标识的最重要依据
    public int sessionID;

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
                String msg = din.readUTF();
                System.out.println("这里收到了msg"+msg);
                if(msg.startsWith("<#CONNECT#>")) {
                    if(!addAClient(msg)) {
                        setFlag(false);
                        return ;
                    }
                } else if(msg.startsWith("<#EXIT#>")) {
                    removeAClient(msg);
                }
            } catch(Exception e) {
                //客户端退出需要对该客户端的所有相关信息进行清空
                System.out.println("客户端已经退出");
                removeAClient("<#EXIT#>"+this.sessionID);
                flag = false;
            }
        }
    }
    //typical addAClient.msg   <#CONNECT#>LEADER#PWD | <#CONNECT#>LEADER#SESSIONID
    public boolean addAClient(String msg) {
        //新建立一个client
        msg = msg.substring(11);
        String[] msgSplits = msg.split("#");
        int clientType = ServerAgent.CLIENTYPE_NOARMAL;
        int sessionID = 0;
        if(msgSplits[0].equals("LEADER")) {
            if(MainThread.sessionPWDtoID.get(msgSplits[1])!=null) {
                sendMsgtoClient("<#CONNECT#>ERROR1");
                return false;
            }
            clientType = ServerAgent.CLIENTYPE_LEANDER;
            sessionID = MainThread.sessionGlobalClock++;
            synchronized (MainThread.lock) {
                MainThread.sessionPWDtoID.put(msgSplits[1], sessionID);
            }
        } else {
            if(MainThread.sessionPWDtoID.get(msgSplits[1])==null) {
                sendMsgtoClient("<#CONNECT#>ERROR2");
                return false;
            }else {
                sessionID = MainThread.sessionPWDtoID.get(msgSplits[1]);    //2
            }
        }
        this.isLeader = clientType;
        this.sessionID = sessionID;
        this.clockID = MainThread.clientGlobalClock++;
        synchronized(MainThread.lock) {
            if(clientType==CLIENTYPE_LEANDER) {
                ArrayList<ServerAgent> listSA = new ArrayList<>();
                listSA.add(this);
                MainThread.SSIDtoCLIENTSA.put(sessionID, listSA);           //1
            }else {
                MainThread.SSIDtoCLIENTSA.get(sessionID).add(this);
            }
            MainThread.CIDtoSSID.put(clockID,sessionID);                    //3
            MainThread.clientCount += 1;
        }
        String successMsg = "<#CONNECT#>"+Integer.toString(clockID)+"#"+Integer.toString(sessionID);
        sendMsgtoClient(successMsg);
        return true;
    }
    //任何成员退出导致游戏结束
    //remove需要关闭线程 删除sesssion相关的数据
    public void removeAClient(String msg) {
        msg.substring(8);
        int rmSessionID=MainThread.CIDtoSSID.get(msg);
        ArrayList<ServerAgent> listSA = MainThread.SSIDtoCLIENTSA.get(rmSessionID);
        //结束线程
        for(int i=0;i<listSA.size();i++) {
            ServerAgent thisclient = listSA.get(i);
            if(thisclient.sessionID==rmSessionID) {
                //向客户端发送消息 是客户端进行销毁反馈信息
                String result = "<#DESTROY#>";
                thisclient.sendMsgtoClient(result);
                thisclient.interrupt();
                thisclient.setFlag(false);
                synchronized (MainThread.lock) {
                    MainThread.clientCount--;
                    MainThread.CIDtoSSID.remove(thisclient.clockID);        //1
                }
            }
        }
        //删除在PWDtoSSID中的所有相关线程数据
        String rmPWD = "";
        for(Map.Entry<String,Integer> entry:MainThread.sessionPWDtoID.entrySet()) {
            if(entry.getValue().equals(rmSessionID)) {
                rmPWD = entry.getKey();
                break;
            }
        }
        synchronized (MainThread.lock) {
            MainThread.sessionPWDtoID.remove(rmPWD);                        //2
            MainThread.SSIDtoCLIENTSA.remove(rmSessionID);                  //3
        }
    }
    public void activateERROR(final String ErrorType) {
        sendMsgtoClient("<#ERROR#>"+ ErrorType);
        this.setFlag(false);
    }
    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    public void sendMsgtoClient(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(msg);
                    ServerAgent.this.dout.writeUTF(msg);
                } catch(Exception e) {
                    System.out.println("发送出现了错误");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

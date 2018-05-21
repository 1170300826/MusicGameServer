import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

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
    public String sessionID;

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
                System.out.println(String.format("IP:%s  MSG:%s",sc.getInetAddress(),msg));
                if(msg.startsWith("<#CONNECT#>")) {
                    if(!addAClient(msg)) {
                    }
                } else if(msg.startsWith("<#EXIT#>")) {
                    removeAClient(msg);
                } else if(msg.startsWith("<#DANMAKU#>")) {
                    broadDanmaku(msg);
                } else if(msg.startsWith("<DESTROYTHREAD>")) {
                    //与客户端的长连接关闭 此时切断当前client的处理线程
                    this.onThreadDestroy();
                    this.setFlag(false);
                }
            } catch(Exception e) {
                //客户端退出需要对该客户端的所有相关信息进行清空
                System.out.println("ERROR IN CLIENT\'S EXIT");
                removeAClient("<#EXIT#>");
                flag = false;
            }
        }
    }
    //在线程关闭之前需要进行处理的事项
    public void onThreadDestroy() {
    }

    //typical addAClient.msg   <#CONNECT#>LEADER#PWD | <#CONNECT#>LEADER#SESSIONID
    public boolean addAClient(String msg) {
        //新建立一个client
        msg = msg.substring(11);
        String[] msgSplits = msg.split("#");
        int clientType = ServerAgent.CLIENTYPE_NOARMAL;
        if(msgSplits[0].equals("LEADER")) {
            if(MainThread.SSIDtoCLIENTSA.get(msgSplits[1])!=null) {
                sendMsgtoClient("<#CONNECT#>ERROR1");
                return false;
            }
            clientType = ServerAgent.CLIENTYPE_LEANDER;
        } else {
            clientType=ServerAgent.CLIENTYPE_NOARMAL;
            if(MainThread.SSIDtoCLIENTSA.get(msgSplits[1])==null) {
                sendMsgtoClient("<#CONNECT#>ERROR2");
                return false;
            }
        }
        this.isLeader = clientType;
        this.sessionID = msgSplits[1];
        this.clockID = MainThread.clientGlobalClock++;
        synchronized(MainThread.lock) {
            if(clientType==CLIENTYPE_LEANDER) {
                ArrayList<ServerAgent> listSA = new ArrayList<>();
                listSA.add(this);
                MainThread.SSIDtoCLIENTSA.put(this.sessionID,listSA);           //1
            }else {
                MainThread.SSIDtoCLIENTSA.get(sessionID).add(this);
            }
        }
        String successMsg = "<#CONNECT#>"+Integer.toString(clockID)+"#"+Integer.toString(clockID);
        sendMsgtoClient(successMsg);

        try {
            Thread.sleep(500);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //通知所有的同组成员改变显示人数
        successMsg = "<#SHOWNUMBER#>"+Integer.toString(MainThread.SSIDtoCLIENTSA.get(sessionID).size());
        ArrayList<ServerAgent> listsa = MainThread.SSIDtoCLIENTSA.get(sessionID);
        for(ServerAgent sa:listsa) {
            sa.sendMsgtoClient(successMsg);
        }
        return true;
    }
    //任何成员退出导致游戏结束
    //remove需要关闭线程 删除sesssion相关的数据
    public void removeAClient(String msg) {
        ArrayList<ServerAgent> listSA = MainThread.SSIDtoCLIENTSA.get(sessionID);
        //结束线程
        if(isLeader==CLIENTYPE_LEANDER) {
            //需要结束所有房间成员的线程
            for (int i = 0; i < listSA.size(); i++) {
                ServerAgent thisclient = listSA.get(i);
                //不向leader发送该信息 该信息意味着client需要回退acitivity重新选择房间进入
                if (thisclient.clockID == this.clockID) continue;
                String result = "<#DESTROY#>";
                thisclient.sendMsgtoClient(result);
            }
            synchronized(MainThread.lock) {
                MainThread.SSIDtoCLIENTSA.remove(sessionID);
            }
        } else {
            //清空个人信息，向其他成员发送人数信息
            String Msg = "<#SHOWNUMBER#>"+Integer.toString((MainThread.SSIDtoCLIENTSA.get(sessionID).size())-1);
            int rmi=0;
            for(int i=0;i<listSA.size();i++) {
                ServerAgent thisclient = listSA.get(i);
                if(thisclient.clockID==this.clockID) {
                    rmi = i;
                } else {
                    thisclient.sendMsgtoClient(Msg);
                }
            }
            synchronized (MainThread.lock) {
                listSA.remove(rmi);
            }
        }
    }
    public void broadDanmaku(String msg) {
        msg = msg.substring(11);
        String[] msgSplits =  msg.split("#");
        ArrayList<ServerAgent> listsa = MainThread.SSIDtoCLIENTSA.get(sessionID);
        for(ServerAgent sa:listsa) {
            sa.sendMsgtoClient("<#DANMAKU#>"+msg);
        }
    }
    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    public void sendMsgtoClient(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerAgent.this.dout.writeUTF(msg);
                    System.out.println("MSG SENDED : "+msg);
                } catch(Exception e) {
                    System.out.println("ERROR IN SENDING MSG");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

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
    //注意一个sa用来标识一个client 因此可以直接通过本地变量获得client的相关信息

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
                System.out.println("这里收到了msg: "+msg);
                if(msg.startsWith("<#CONNECT#>")) {
                    if(!addAClient(msg)) {
                    }
                } else if(msg.startsWith("<#EXIT#>")) {
                    removeAClient(msg,0);
                } else if(msg.startsWith("<#DANMAKU#>")) {
                    broadDanmaku(msg);
                }
            } catch(Exception e) {
                //客户端退出需要对该客户端的所有相关信息进行清空
                System.out.println("客户端已经退出");
                removeAClient("<#EXIT#>"+Integer.toString(this.clockID),0);
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
            clientType=ServerAgent.CLIENTYPE_NOARMAL;
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
        this.homePWD = msgSplits[1];
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
        String successMsg = "<#CONNECT#>"+Integer.toString(clockID)+"#"+Integer.toString(clockID);
        sendMsgtoClient(successMsg);

        try {
            Thread.sleep(100);
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
    public void removeAClient(String msg,int type) {
        //msg = msg.substring(8);
        int rmSessionID=sessionID//MainThread.CIDtoSSID.getOrDefault(Integer.parseInt(msg),-1);
        //System.out.println("对比："+Integer.toString(rmSessionID)+" "+Integer.toString(sessionID));
        ///if(rmSessionID==-1){
        //    System.out.println("删除失败");
        //    return ;
        //}
        /*
        synchronized (MainThread.lock) {
            MainThread.CIDtoSSID.remove(msg);
        }
        */
        ArrayList<ServerAgent> listSA = MainThread.SSIDtoCLIENTSA.get(rmSessionID);
        //结束线程
        if(isLeader==CLIENTYPE_LEANDER) {
            //需要结束所有房间成员的线程
            for (int i = 0; i < listSA.size(); i++) {
                ServerAgent thisclient = listSA.get(i);
                //不向leader发送该信息
                if (thisclient.clockID == this.clockID) continue;
                //向其他客户端发送消息 是客户端进行销毁反馈信息
                //synchronized (MainThread.lock) {
                //    MainThread.clientCount--;
                //    MainThread.CIDtoSSID.remove(thisclient.clockID);        //1
                //}
                String result = "<#DESTROY#>";
                thisclient.sendMsgtoClient(result);
                thisclient.interrupt();
                thisclient.setFlag(false);
            }

        } else {
            //清空个人信息，向其他成员发送人数信息

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
        //向其他client进行人数的广播
        String successMsg = "<#SHOWNUMBER#>"+Integer.toString(MainThread.SSIDtoCLIENTSA.get(rmSessionID).size());
        ArrayList<ServerAgent> listsa = MainThread.SSIDtoCLIENTSA.get(sessionID);
        int rmi=0;
        for(int i=0;i<listsa.size();i++) {
            ServerAgent sa = listsa.get(i);
            if(sa.clockID == Integer.parseInt(msg)) {
                rmi = i;
            } else {
                sa.sendMsgtoClient(successMsg);
            }
        }
        listsa.remove(rmi);
        System.out.println("现在的容量是："+MainThread.SSIDtoCLIENTSA.get(sessionID).size());
    }
    public void broadDanmaku(String msg) {
        msg = msg.substring(11);
        String[] msgSplits =  msg.split("#");
        ArrayList<ServerAgent> listsa = MainThread.SSIDtoCLIENTSA.get(
                MainThread.CIDtoSSID.get(Integer.parseInt(msgSplits[0]))
        );
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
                    System.out.println("这里发布了msg: "+msg);
                    ServerAgent.this.dout.writeUTF(msg);
                } catch(Exception e) {
                    System.out.println("发送出现了错误");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

package Thread;

import data.ClientTeamData;
import data.MusicManager;
import fileIO.IOManager;
import sun.applet.Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//单独一个client的thread用来处理相关的处理操作
//这里对于每一个与server连接的client分配一个serverAgent处理线程
// ，在线程中的din与dout是建立两者之间独特通信的标识
//返回错误记录表：
// type1代表leader的密码与之前的密码产生重复
// type2代表teamate的密码不能与正在创建的房间所匹配

public class ServerAgent extends Thread {
    public static final int CLIENTYPE_LEANDER = 111;
    public static final int CLIENTYPE_NOARMAL = 222;
    public int isLeader;   //是否是房主
    public int clockID;    //具体的clock标识 用来返回给客户端 用来标识的最重要依据
    public String sessionID;

    boolean flag = true;
    Socket sc;
    DataInputStream din;
    DataOutputStream dout;

    public ServerAgent(Socket sc) {
        this.sc = sc;
        try {
            din = new DataInputStream(sc.getInputStream());
            dout = new DataOutputStream(sc.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (flag) {
            try {
                String msg = din.readUTF();
                String[] msgSplits;
                if(!msg.equals("HEART_BEAT_MSG"))
                    System.out.println(String.format("IP:%s  MSG:%s", sc.getInetAddress(), msg));
                if (msg.startsWith("<#CONNECT#>")) {
                    if (!addAClient(msg)) {
                    }
                } else if (msg.startsWith("<#EXIT#>")) {
                    removeAClient(msg);
                } else if (msg.startsWith("<#DANMAKU#>")) {
                    broadDanmaku(msg);
                } else if (msg.startsWith("<#DESTROYTHREAD#>")) {
                    //与客户端的长连接关闭 此时切断当前client的处理线程
                    this.onThreadDestroy();
                    this.setFlag(false);
                } else if (msg.startsWith("<#BUTTONPRESSED#>")) {    //按下开始游戏按钮 传递更换activity的信息
                    onButtonPressed(msg);
                }
                else if(msg.startsWith("<#WAITVIEW#>")) {
                    msgSplits = msg.substring(12).split("#");
                    onWaitView(msg,msgSplits);
                } else if(msg.startsWith("<#MUSICOVERVIEW#>")) {
                    msg = msg.substring(17);
                    if (msg.startsWith("MUSICSENDED")) {
                        msg = msg.substring(12);
                        onMusicReceived(msg);
                    }
                } else if(msg.startsWith("<#CHOOSEVIEW#>")) {
                    msg = msg.substring(14);
                    onChooseView(msg);
                } else if(msg.startsWith("<#CREATEVIEW#>")) {
                    msg = msg.substring(14);
                    onCreateView(msg);
                } else if(msg.startsWith("<#MUTIPLAYVIEW#>")) {
                    msg = msg.substring(16);
                    onMutiPlayView(msg);
                }
            } catch (Exception e) {
                //客户端退出需要对该客户端的所有相关信息进行清空
                System.out.println("ERROR IN CLIENT\'S EXIT");
                removeAClient("<#EXIT#>");
                flag = false;

            }
        }
    }
        //在线程关闭之前需要进行处理的事项
        public void onThreadDestroy () {
            try {
                removeAClient("<EXIT>");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        //typical addAClient.msg   <#CONNECT#>LEADER#PWD | <#CONNECT#>LEADER#SESSIONID
        public boolean addAClient (String msg) {
            //新建立一个client
            msg = msg.substring(11);
            String[] msgSplits = msg.split("#");
            int clientType;
            if (msgSplits[0].equals("LEADER")) {
                if (MainThread.SSIDtoCLIENTSA.get(msgSplits[1]) != null) {
                    sendMsgtoClient("<#CONNECT#>ERROR1");
                    return false;
                }
                clientType = ServerAgent.CLIENTYPE_LEANDER;
            } else {
                //需要对新加入的client进行检查 如果team已经在游戏 则不能加入
                clientType = ServerAgent.CLIENTYPE_NOARMAL;
                ClientTeamData data = MainThread.SSIDtoCLIENTSA.get(msgSplits[1]);
                //加入房间的错误处理
                if (data == null) { //没有房间
                    sendMsgtoClient("<#CONNECT#>ERROR1");
                    return false;
                }else if(MainThread.SSIDtoCLIENTSA.get(msgSplits[1]).teamState==1) {    //已经刚开始游戏
                    sendMsgtoClient("<#CONNECT#>ERROR3");
                    return false;
                } else if(MainThread.SSIDtoCLIENTSA.get(msgSplits[1]).teamState==-1) {  //房主仍然在选择
                    sendMsgtoClient("<#CONNECT#>ERROR4");
                    return false;
                } else if(data.teamNumberLimit <= data.listsa.size()) { //房间人数已经满
                        sendMsgtoClient("<#CONNECT#>ERROR2");
                        return false;
                }

            }
            this.isLeader = clientType;
            this.sessionID = msgSplits[1];
            this.clockID = MainThread.clientGlobalClock++;
            synchronized (MainThread.lock) {
                if (clientType == CLIENTYPE_LEANDER) {
                    ClientTeamData data = new ClientTeamData();
                    data.listsa.add(this);
                    MainThread.SSIDtoCLIENTSA.put(this.sessionID, data);           //1
                } else {
                    MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.add(this);
                }
            }
            if(isLeader == CLIENTYPE_LEANDER) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                MainThread.SSIDtoCLIENTSA.get(sessionID).date =  df.format(new Date());
            }
            //在msg中尝试加入更多与 isntru显示相关的数据
            String successMsg = "<#CONNECT#>" + Integer.toString(clockID) + "#" + sessionID+"#";
            if(isLeader==CLIENTYPE_NOARMAL) {
                successMsg += MainThread.SSIDtoCLIENTSA.get(sessionID).strInstruNum;
            }
            sendMsgtoClient(successMsg);
            updateTeamInfo(sessionID);
            return true;
        }
        //任何成员退出导致游戏结束
        //remove需要关闭线程 删除sesssion相关的数据
        public void removeAClient (String msg) {
            if(MainThread.SSIDtoCLIENTSA.get(sessionID)==null) return ;
            ArrayList<ServerAgent> listSA = MainThread.SSIDtoCLIENTSA.get(sessionID).listsa;
            if(listSA==null || listSA.size()==0) return ;
            if(!listSA.contains(this)) return ;
            //结束线程
            if (isLeader == CLIENTYPE_LEANDER) {
                for (int i = 0; i < listSA.size(); i++) {
                    ServerAgent thisclient = listSA.get(i);
                    //不向leader发送该信息 该信息意味着client需要回退acitivity重新选择房间进入
                    if (thisclient.clockID == this.clockID) continue;
                    String result = "<#DESTROY#>";
                    thisclient.sendMsgtoClient(result);
                }
                synchronized (MainThread.lock) {
                    MainThread.SSIDtoCLIENTSA.remove(sessionID);
                }
            } else {
                //清空个人信息，向其他成员发送人数信息
                String Msg = "<#SHOWNUMBER#>" + Integer.toString((MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.size()) - 1);
                int rmi = 0;
                for (int i = 0; i < listSA.size(); i++) {
                    ServerAgent thisclient = listSA.get(i);
                    if (thisclient.clockID == this.clockID) {
                        rmi = i;
                    } else {
                        thisclient.sendMsgtoClient(Msg);
                    }
                }
                synchronized (MainThread.lock) {
                    listSA.remove(rmi);
                }
                //全部的成员已经退出：需要清理team数据 同时保存team信息
                if(MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.size()==0) {
                    //保存team数据
                    MusicManager manager = MainThread.SSIDtoCLIENTSA.get(sessionID).music;
                    manager.onMusicOver(MainThread.SSIDtoCLIENTSA.get(sessionID));
                    MainThread.SSIDtoCLIENTSA.remove(sessionID);
                }
            }
        }
        public void broadDanmaku (String msg){
            msg = msg.substring(11);
            String[] msgSplits = msg.split("#");
            sendMsgtoTeam(sessionID,"<#DANMAKU#>" + msg);
        }
        public void setFlag ( boolean flag){
            this.flag = flag;
        }
        public void onButtonPressed(String msg) {
            //向组内服务器传递ButtonPressed的信息
            ClientTeamData data = MainThread.SSIDtoCLIENTSA.get(sessionID);
            for (ServerAgent sa: data.listsa) {
                sa.sendMsgtoClient(msg);
            }
        }
        public void onWaitView(String msg,String[] msgSplits) {
            String text;
            ClientTeamData data = MainThread.SSIDtoCLIENTSA.get(sessionID);
            int[] initFlag = MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag;
                if (msgSplits[1].startsWith("INSTRU")) {
                    int rk = msgSplits[1].charAt(6) - '0';
                    if (msgSplits[2].equals("SELECT")) { //选择当前的按钮
                        if(initFlag[rk]==clockID) {
                            MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag[rk]=-1;
                            sendMsgtoTeam(sessionID,"<#WAITVIEW#>"+msgSplits[0]+"#"+msgSplits[1]+"#UNSELECT");
                        } else if (initFlag[rk] == -1) {    //可以进行选择
                            for (int i = 0; i < 4; i++)
                                if (initFlag[i] == clockID) {
                                    MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag[rk]=-1;
                                    //System.out.println(MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag[i]);
                                    MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag[i]=-1;
                                    sendMsgtoTeam(sessionID, String.format("<#WAITVIEW#>%d#INSTRU%d#UNSELECT", clockID, i));
                                }
                            initFlag[rk] = clockID;
                            text = msg;
                            sendMsgtoTeam(sessionID, text);
                        } else {            //不允许选择
                            text = String.format("<#WAITVIEW#>%d#INSTRUFALSE", clockID);
                            sendMsgtoClient(text);
                        }

                    }
                } else if(msgSplits[0].equals("STARTGAME")||msgSplits[0].startsWith("STARTGAME&MUSIC")) {
                int[]  instruFlag = data.instruFlag;
                MainThread.SSIDtoCLIENTSA.get(sessionID).teamState = 1;
                int cnt = 0;
                for(int i=0;i<4;i++) if(instruFlag[i]!=-1) cnt++;
                if(cnt==data.listsa.size()) {
                    if(msgSplits[0].equals("STARTGAME")) {
                        sendMsgtoTeam(sessionID,msg);
                    } else if(msgSplits[0].startsWith("STARTGAME&MUSIC")) {
                        int instruType = Integer.parseInt(msgSplits[1]);
                        String fname = data.chooseMusicName;
                        int x = fname.indexOf("-");
                        fname = fname.substring(0,x)+Integer.toString(instruType)+fname.substring(x);
                        IOManager io = new IOManager(fname,IOManager.FILE_READ,false);
                        String ans = io.read();
                        io.onDestroy();
                        sendMsgtoClient("<#WAITVIEW#>STARTGAME&MUSIC#"+ans);
                    }
                } else if(cnt<data.listsa.size()) {
                    sendMsgtoClient("<#WAITVIEW#>STARTFALSE1");
                }
            } else if(msgSplits[0].startsWith("STARTGAME&MUSIC")) {

            } else if(msgSplits[0].startsWith("INSTRUNUMLIMIT")) {
                    MainThread.SSIDtoCLIENTSA.get(sessionID).teamNumberLimit = Integer.parseInt(msgSplits[1].trim());
                }
        }
        public void onMusicReceived(String msg) {
            String[] msgSplits = msg.split("#");
            MusicManager manager = MainThread.SSIDtoCLIENTSA.get(sessionID).music;
            if(MainThread.SSIDtoCLIENTSA.get(sessionID).musicName=="") {
                MainThread.SSIDtoCLIENTSA.get(sessionID).musicName=msgSplits[0];
            }
            MainThread.SSIDtoCLIENTSA.get(sessionID).uploadFlag[Integer.parseInt(msgSplits[1])] = true;
            String date = MainThread.SSIDtoCLIENTSA.get(sessionID).date;
            manager.onMusicReceived(msgSplits[0]+msgSplits[1]+"-"+date,msgSplits[3]);
            sendMsgtoClient("<#MUSICOVERVIEW#>RECEIVED");
            //需要向leader发送 更新人数信息的msg
            if(isLeader==CLIENTYPE_NOARMAL) MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.remove(this);
            int size = MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.size();
            sendMsgtoTeam(sessionID,String.format("<#MUSICOVERVIEW#>NUMBER#%d",size));
        }
        public void onChooseView(String msg) {
            String[] msgSplits = msg.split("#");
            if(msg.startsWith("MUSICSELECT")) {
                String filename = msgSplits[1];
                IOManager reader = new IOManager(filename, IOManager.FILE_READ, false);
                String ans = reader.read();
                reader.onDestroy();
                MainThread.SSIDtoCLIENTSA.get(sessionID).chooseMusicName = filename;
                MainThread.SSIDtoCLIENTSA.get(sessionID).strInstruNum = ans;            //记录小组中的乐器可使用情况
                sendMsgtoClient("<#CHOOSEVIEW#>INSTRUNUM#"+ans);
                updateTeamInfo(sessionID);
            }
        }
        public void onCreateView(String msg) {
            String[] msgSplits = msg.split("#");
            //进行游戏状态的修改
            if(msgSplits[0].equals("teamstate")) {
                MainThread.SSIDtoCLIENTSA.get(sessionID).teamState = Integer.parseInt(msgSplits[1]);
            } else
            if(msgSplits[0].equals("MusicList")) {   //需要提供附属乐器的信息
                IOManager io = new IOManager("MusicList",IOManager.FILE_READ,false);
                String ans = io.read();
                io.onDestroy();
                StringBuffer builder = new StringBuffer();
                String[] nameList = ans.split("\\$\\$");
                for(int i=0;i<nameList.length;i++) {
                    IOManager info = new IOManager(nameList[i],IOManager.FILE_READ,false);
                    String instruInfo = info.read();
                    builder.append(instruInfo+"#");
                }
                sendMsgtoClient("<#CREATEVIEW#>"+ans+"#"+builder.toString());
            }
        }
        public void onMutiPlayView(String msg) {
            if(msg.startsWith("GAMEOVER")) {
                ClientTeamData data = MainThread.SSIDtoCLIENTSA.get(sessionID);
                msg = msg.substring(9);
                //需要向所有的已有MUTIPLAYVIEW发送信息
                String finalMsg = msg;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(300);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        int x=0;
                        for(int i=0;i<4;i++) if(data.instruFlag[i]==clockID) break;
                        sendMsgtoTeam(sessionID, String.format("<#MUTIRES#>ADDRES#%d#",x) + finalMsg);
                        for (int i = 0; i < 4; i++) {
                            if (data.mateFinalScore[i] == 0) {
                                data.mateFinalScore[i] = Integer.parseInt(finalMsg.split("#")[1].trim());
                                break;
                            }
                        }
                        if ((++data.mateGameOver) >= data.listsa.size()) {
                            //需要计算总得分
                            try {
                                Thread.sleep(300);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int ans = 0;
                            for (int i = 0; i < 4; i++) ans += data.mateFinalScore[i];
                            sendMsgtoTeam(sessionID, "<#MUTIRES#>GAMEOVER#" + ans);
                        }
                    }
                }).start();
            }
        }

        //------------Util Module------------------------
        public void sendMsgtoClient (final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerAgent.this.dout.writeUTF(msg);
                    System.out.println("MSG SENDED : " + msg);
                } catch (Exception e) {
                    System.out.println("ERROR IN SENDING MSG");
                    e.printStackTrace();
                }
            }
        }).start();
    }
        public void sendMsgtoTeam(String sessionID,String msg) {
        //向sessionID的小组内发送msg信息
            ArrayList<ServerAgent> listsa = MainThread.SSIDtoCLIENTSA.get(sessionID).listsa;
            for (ServerAgent sa:listsa){
                sa.sendMsgtoClient(msg);
            }
        }
        public void updateTeamInfo(String sessionID) {
            //等待activity的跳转时间
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //通知所有的同组成员改变显示人数
            String successMsg = "<#SHOWNUMBER#>" + Integer.toString(MainThread.SSIDtoCLIENTSA.get(sessionID).listsa.size());
            sendMsgtoTeam(sessionID,successMsg);
            //向该client传递更新view信息的msg
            int[] instruFlag = MainThread.SSIDtoCLIENTSA.get(sessionID).instruFlag;
            for(int i=0;i<4;i++) if(instruFlag[i]!=-1) {
                sendMsgtoClient(String.format("<#WAITVIEW#>%d#INSTRU%d#SELECT",instruFlag[i],i));
            }
        }
}
package data;
import Thread.ServerAgent;

import java.util.ArrayList;
//一个client组内涉及的Data
// 在MainThread中需要存储的所有类型

public class ClientTeamData {
    public int connectType;     //标志着连接的类型 0-编曲activity 1-play流程
    public String chooseMusicName;
    public int teamState;               //0-未开始游戏 1-已开始游戏
    public int teamNumberLimit;       //队伍中队员数量的限制
    public ArrayList<ServerAgent> listsa; //所有serverAgent的句柄
    public int[] instruFlag;            //四个乐器的标志
    public boolean[] uploadFlag = new boolean[4];        //上传乐谱的标志
    public String musicName;
    public String strInstruNum;
    public MusicManager music;
    public String date;

    public ClientTeamData() {
        connectType = 0;
        date = "";
        teamNumberLimit = 4;
        teamState=-1;
        musicName = "";
        chooseMusicName = "";
        strInstruNum="";
        listsa = new ArrayList<>();
        instruFlag = new int[4];
        for(int i=0;i<4;i++) uploadFlag[i]=false;
        for(int i=0;i<4;i++) instruFlag[i]=-1;
        music = new MusicManager();
    }
}

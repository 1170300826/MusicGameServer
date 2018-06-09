package data;
import Thread.ServerAgent;

import java.util.ArrayList;
//一个client组内涉及的Data
// 在MainThread中需要存储的所有类型

public class ClientTeamData {
    public int teamState;       //0-选择等待界面 1-游戏进行
    public int mateNumber;
    public ArrayList<ServerAgent> listsa; //所有serverAgent的句柄
    public int[] instruFlag;            //四个乐器的标志
    public boolean[] uploadFlag = new boolean[4];        //上传乐谱的标志
    public String musicName;
    public MusicManager music;
    public String date;
    public ClientTeamData() {
        date = "";
        teamState = 0;
        mateNumber = 0;
        musicName = "";
        listsa = new ArrayList<>();
        instruFlag = new int[4];
        for(int i=0;i<4;i++) uploadFlag[i]=false;
        for(int i=0;i<4;i++) instruFlag[i]=-1;
        music = new MusicManager();
    }
}

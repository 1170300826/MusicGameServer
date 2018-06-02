package data;
import Thread.ServerAgent;

import java.util.ArrayList;
//一个client组内涉及的Data
// 在MainThread中需要存储的所有类型

public class ClientTeamData {
    public ArrayList<ServerAgent> listsa; //所有serverAgent的句柄
    public int[] instruFlag;            //四个乐器的标志位
    public ClientTeamData() {
        listsa = new ArrayList<>();
        instruFlag = new int[4];
        for(int i=0;i<4;i++) instruFlag[i]=-1;
    }
}

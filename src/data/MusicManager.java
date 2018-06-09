package data;

import fileIO.IOManager;

//对于一个team的所有乐谱进行统一的管理
//一个team对应一个manager

public class MusicManager {
    int clientNum = 0;
    IOManager io;
    public MusicManager() {
    }
    private void addMusicInstru(String name,int type,String msg) {
        io = new IOManager(String.format("%s%d",name,type) , IOManager.FILE_WRITE , false);
        io.write(msg);
        io.onDestroy();
    }
    public void onMusicReceived(String name,String type,String time,String msg) {
        if(clientNum==0) {
            IOManager ls = new IOManager("MusicList",IOManager.FILE_WRITE,true);
            ls.write(name+'\n');
            ls.onDestroy();
        }
        clientNum++;
        addMusicInstru(name,Integer.parseInt(type),msg);
    }
    public void onMusicOver(ClientTeamData data)  {    //小组内的所有成员都已经退出
        IOManager io = new IOManager("serverdata/"+data.musicName+".txt",IOManager.FILE_WRITE,false);
        StringBuffer builder = new StringBuffer();
        for(int i=0;i<4;i++)
            if(data.instruFlag[i]!=-1) {
                builder.append(String.format("%d ",i));
            }
        io.write(builder.toString());
        io.onDestroy();
    }

}

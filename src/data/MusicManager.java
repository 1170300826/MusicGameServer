package data;

import fileIO.IOManager;

//对于一个team的所有乐谱进行统一的管理
//一个team对应一个manager

public class MusicManager {
    int clientNum = 0;
    IOManager io;
    public MusicManager() {
    }
    private void addMusicInstru(String name,String msg) {
        io = new IOManager(name , IOManager.FILE_WRITE , false);
        io.write(msg);
        io.onDestroy();
    }
    public void onMusicReceived(String name,String msg) {
        if(clientNum==0) {
            IOManager ls = new IOManager("MusicList",IOManager.FILE_WRITE,true);
            ls.write(name+'\n');
            ls.onDestroy();
        }
        clientNum++;
        //记录该组内的乐器组成
        String[] msgSplits = msg.split("-");
        char type = msgSplits[0].charAt(msgSplits[0].length()-1);
        IOManager info = new IOManager(msgSplits[0].substring(0,msgSplits[0].length()-1),IOManager.FILE_WRITE,true);
        info.write(String.format("%c\n",type));
        info.onDestroy();

        addMusicInstru(name,msg);
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

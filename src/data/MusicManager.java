package data;

import fileIO.IOManager;

import static java.lang.Math.max;

//对于一个team的所有乐谱进行统一的管理
//一个team对应一个manager

public class MusicManager {
    int instruNum = 0;
    long maxTime = 0;
    IOManager io;
    public MusicManager() {
    }
    private void addMusicInstru(String name,int type,String msg) {
        io = new IOManager(String.format("%s%d",name,type),IOManager.FILE_WRITE);
        io.write(msg);
        io.onDestroy();
    }
    public void onMusicReceived(String name,String type,String time,String msg) {
        addMusicInstru(name,Integer.parseInt(type),msg);
        maxTime = max(maxTime,Long.parseLong(time));
        if((++instruNum)==4) {
            io = new IOManager(name,IOManager.FILE_WRITE);
            io.write(String.format("%s %d",name+type,maxTime));
            io.onDestroy();
        }
    }

}

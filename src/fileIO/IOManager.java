package fileIO;

import java.io.*;

public class IOManager {
    public static final int FILE_WRITE = 1111;
    public static final int FILE_READ = 1112;
    FileInputStream in;
    FileOutputStream out;
    BufferedReader reader;
    BufferedWriter writer;
    int ioType;

    public IOManager(String filename,int ioType,boolean flag) {
        filename = "serverdata/"+filename+".txt";
        this.ioType = ioType;
        try {
            if (ioType == FILE_WRITE) {
                out = new FileOutputStream(filename,flag);
                writer = new BufferedWriter(new OutputStreamWriter(out));
            } else {
                in = new FileInputStream(filename);
                reader = new BufferedReader(new InputStreamReader(in));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public void write(String msg) {
        try {
            writer.write(msg);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public String read() {
        StringBuffer builder = new StringBuffer();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
    public void onDestroy() {
        try {
            if (ioType == FILE_WRITE) {
                writer.close();
            } else {
                reader.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

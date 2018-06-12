package test;

import java.io.*;

public class test {
    public static void main(String[] args) {
        FileInputStream in = null;
        BufferedReader reader = null;
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try {
            in = new FileInputStream("musicscore.txt");
            reader = new BufferedReader(new InputStreamReader(in));
            out = new FileOutputStream("musicscore2.txt");
            writer = new BufferedWriter(new OutputStreamWriter(out));
        } catch(Exception e) {
            e.printStackTrace();
        }
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] strs = line.trim().split(" ");
                int t1 = Integer.parseInt(strs[1].trim());
                int t2 = Integer.parseInt(strs[2].trim());
                String ans = String.format("%d %d %s", t1 * 400, (t1 + t2) * 400,strs[0]);
                writer.write(ans+"\n");
            }
            writer.flush();
            writer.close();
            reader.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}

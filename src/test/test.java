package test;

import java.io.*;

public class test {
    public static void main(String[] args) {
        int[] key = {
                0,1,3,5,6,8,10,12,13,2,4,7,9,11
        };
        FileInputStream in = null;
        BufferedReader reader = null;
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try {
            in = new FileInputStream("小白兔乖乖.txt");
            reader = new BufferedReader(new InputStreamReader(in));
            out = new FileOutputStream("小白兔乖乖2.txt");
            writer = new BufferedWriter(new OutputStreamWriter(out));
        } catch(Exception e) {
            e.printStackTrace();
        }
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] strs = line.trim().split(" ");
                System.out.println(line);
                int t1 = Integer.parseInt(strs[1].trim());
                int t2 = Integer.parseInt(strs[2].trim());
                int k = Integer.parseInt(strs[0].trim());
                String ans = String.format("%d %d %d", t1 * 400, ( t2) * 400,key[k]);
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

package test;

import java.util.ArrayList;

public class test {
    public static void main(String[] args) {
        class phone {
            public int[] ls = new int[4];
            public int rk = 0;
            public ArrayList<Integer> list = new ArrayList<>();
            public phone() {

            }
        }
        phone c = new phone();
        ArrayList<Integer> list = c.list;
        int x = c.rk;
        c.rk=12;
        list.add(12);
        list.add(23123);
        int[] z = c.ls;
        z[0]=12;
        System.out.println(c.ls[0]);
    }
}

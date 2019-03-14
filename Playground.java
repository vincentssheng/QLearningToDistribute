import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.*;
import java.util.Random;
import java.lang.Math;


public class Playground {

    static int count = 0;

    public static void print_2d_array(int[][] a){
        for(int[] row : a){
            System.out.println(Arrays.toString(row));
        }
    }

//    QLearnerAI q = new QLearnerAI("p1", 0.15);
    public static void main(String[] args){
        try{
            File folder = new File("qtables/");
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("qtable.txt")));
            File[] listOfFiles = folder.listFiles();
            for (File f : listOfFiles){
                if (f.exists()){
                    System.out.println("Loading Q Table " + f);
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line;
                    while ((line = br.readLine()) != null){
                        line = line.substring(0, line.length()-1);
                        bw.write(line);
                    }
                }
            }
            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

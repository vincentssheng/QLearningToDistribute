import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.io.*;


public class QLearnerAI extends AIModule {

    private static double gamma = 0.99;
    private double epsilon = 0.99;
    public static HashMap<String, int[]> state_action_count = new HashMap<String, int[]>();
    public static HashMap<String, String[]> state_action_values = new HashMap<String, String[]>();
    int is_training;

    public QLearnerAI(int is_training) {
        this.is_training = is_training;
    }

    class Board {
        String state;
        ArrayList<Integer> legalActions;
        String[] q_values;

        public Board(ArrayList<Integer> legalActions, String state, String[] q_values) {
            this.legalActions = legalActions;
            this.state = state;
            this.q_values = q_values;
        }

    }


    @Override
    public void getNextMove(GameStateModule game) {
        if (is_training == 1) {
            Board curr_board = getStateActionValues(game);
            seedQTableAtTraining(game,curr_board);
            chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            updateQTable(game, curr_board);
        } else {
            try {
                Board curr_board = getStateActionValuesFromFile(game);
                curr_board.q_values = seedQTable(game,curr_board);
                chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private Board getStateActionValuesFromFile(GameStateModule game) throws Exception {
        String currState = "";
        int nonzeros = 0;
        ArrayList<Integer> legalActions = new ArrayList<Integer>();
        for (int i = 0; i < game.getWidth(); i++) {
            // legal actions
            if (game.canMakeMove(i)) {
                legalActions.add(i);
            }
            // current state
            for (int j = 0; j < game.getHeight(); j++) {
                currState += String.valueOf(game.getAt(i, j));
                if (game.getAt(i, j) != 0)
                    nonzeros += 1;
            }
        }
        File f = new File("qtables/" + nonzeros + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String[] q_values = new String[game.getWidth()];
        for (int i = 0; i < q_values.length; i++) {
            q_values[i] = "0";
        }
        String line;
        while ((line = br.readLine()) != null) {
            line = line.substring(0, line.length() - 1);
            String[] spl = line.split(":");
            if (spl[0].equals(currState)) {
                q_values = spl[1].split(" ");
                break;
            }
        }
        return new Board(legalActions, currState, q_values);
    }


    private Board getStateActionValues(GameStateModule game) {
        String currState = "";
        ArrayList<Integer> legalActions = new ArrayList<Integer>();
        for (int i = 0; i < game.getWidth(); i++) {

            if (game.canMakeMove(i)) {
                legalActions.add(i);
            }

            for (int j = 0; j < game.getHeight(); j++) {
                currState += String.valueOf(game.getAt(i, j));
            }
        }

        String[] q_values = state_action_values.get(currState);
        if (q_values == null) {
            String[] action_values = new String[game.getWidth()];
            for (int i = 0; i < game.getWidth(); i++)
                action_values[i] = "0";
            q_values = action_values;
            state_action_values.put(currState, q_values);
            state_action_count.put(currState, new int[game.getWidth()]);
        }
        return new Board(legalActions, currState, q_values);

    }


    private int selectMove(ArrayList<Integer> legalActions, String[] q_values) {
        if (is_training == 1) {
            if (Math.random() < epsilon) {
                Random rand = new Random();
                return legalActions.get(rand.nextInt(legalActions.size()));
            }
        }

        Random rand1 = new Random();
        int action = legalActions.get(rand1.nextInt(legalActions.size()));
        double max = Double.parseDouble(q_values[action]);
        for (int i = 0; i < legalActions.size(); i++) {
            if (Double.parseDouble(q_values[legalActions.get(i)]) > max) {
                max = Double.parseDouble(q_values[legalActions.get(i)]);
                action = legalActions.get(i);
            }
        }
        return action;
    }

    private void updateQTable(GameStateModule game, Board curr_board) {
        // update q(s, a) and count(s, a)

        game.makeMove(chosenMove); // **********************************************

        double reward = 0;
        double maxsa = 0;
        // direct win
        if (game.isGameOver()) {
            if (game.getWinner() != 0) //win
                reward = 1000;
            else //draw
                reward = 0;

            epsilon *= 0.999999;

        } else {
            Board next_board = getStateActionValues(game);
            int move = selectMove(next_board.legalActions, next_board.q_values);
            game.makeMove(move); // *************************
            if (game.isGameOver()) {
                if (game.getWinner() != 0) //win
                    reward = -1000;
                else //draw
                    reward = 0;
            }
            Board new_board = getStateActionValues(game);
            String sprime = new_board.state;
            for (int i = 0; i < new_board.legalActions.size(); i++) {
                if (Double.parseDouble(state_action_values.get(sprime)[i]) > maxsa) {
                    maxsa = Double.parseDouble(state_action_values.get(sprime)[i]);
                }
            }
            game.unMakeMove(); // ************************
        }


        double alpha = 1.0 / (double) (++state_action_count.get(curr_board.state)[chosenMove]);
        state_action_values.get(curr_board.state)[chosenMove] = Double.toString((alpha * (gamma * maxsa + reward) + (1 - alpha) * Double.parseDouble(state_action_values.get(curr_board.state)[chosenMove])));

        game.unMakeMove(); // **********************************************

    }

    //Part 2

    private void seedQTableAtTraining(GameStateModule game, Board curr_board) {
        
        if(curr_board.state.equals("00000000000000000000")){
            state_action_values.get(curr_board.state)[2] = Double.toString(1000.0);
        }
        
        for (int i = 0; i < curr_board.legalActions.size(); i++) {
            game.makeMove(curr_board.legalActions.get(i));
            if (game.isGameOver() && game.getWinner() != 0) {
                state_action_values.get(curr_board.state)[curr_board.legalActions.get(i)] = Double.toString(1000.0);//seed +1000
                game.unMakeMove();
                break;
            }
            Board next_board = getStateActionValues(game);
            for (int j = 0; j < next_board.legalActions.size(); j++) {
                game.makeMove(next_board.legalActions.get(j));
                if (game.isGameOver() && game.getWinner() != 0) {
                    state_action_values.get(curr_board.state)[curr_board.legalActions.get(i)] = Double.toString(-1000.0);//seed -1000
                    game.unMakeMove();
                    break;
                }
                game.unMakeMove();
            }
            game.unMakeMove();
        }
    }
    
    private String[] seedQTable(GameStateModule game, Board curr_board) {
        if(curr_board.state.equals("00000000000000000000")){
            curr_board.q_values[2] = Double.toString(1000.0);
            System.out.println("Seed");
        }
        
        for (int i = 0; i < curr_board.legalActions.size(); i++) {
                game.makeMove(curr_board.legalActions.get(i));
                if (game.isGameOver() && game.getWinner() != 0) {
                    curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(1000.0);//seed +1000
                    game.unMakeMove();
                    break;
                }
                Board next_board = getStateActionValues(game);
                for (int j = 0; j < next_board.legalActions.size(); j++) {
                        game.makeMove(next_board.legalActions.get(j));
                        if (game.isGameOver() && game.getWinner() != 0) {
                            curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(-1000.0);//seed -1000
                            game.unMakeMove();
                            break;
                        }
                        game.unMakeMove();
                }
                game.unMakeMove();
        }
        return curr_board.q_values;
    }


}


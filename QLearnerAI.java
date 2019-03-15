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
            //seedQTableAtTraining(game,curr_board);
            chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            updateQTable(game, curr_board);
        } else {
            try {
                Board curr_board = getStateActionValuesFromFile(game);
                //curr_board.q_values = seedQTable(game,curr_board);
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
        /* Tianyang exploring his sexual orientation randomly 
		if (is_training == 1) {
            if (Math.random() < epsilon) {
                Random rand = new Random();
                return legalActions.get(rand.nextInt(legalActions.size()));
            }
        } */

		/* Vincent exploring his sexual orientation with probabilities and weights given states */
		if (is_training == 1) {
			double totalProb = 0;
			double k = Math.E; // k has to be strictly greater than 0
	    	for(int i = 0; i < legalActions.size(); i++) {
				totalProb += Math.pow(k, Double.parseDouble(q_values[legalActions.get(i)]));
			}
			int count = 0;
			double cummulativeProb = 0.0;
			double rand = Math.random(); //between [0, 1)
			while(count < legalActions.size()){ 
				cummulativeProb += (Math.pow(k, Double.parseDouble(q_values[legalActions.get(count)])) / totalProb);
            	if ((rand < cummulativeProb) || (count == legalActions.size()-1)) { //so we use <
                	return legalActions.get(count);
				}
				count++;
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
                reward = 300;

            epsilon *= 0.99999995; // 8 for 2M, 95 for 10M

        } else {
            Board next_board = getStateActionValues(game);
            int move = selectMove(next_board.legalActions, next_board.q_values);
            game.makeMove(move); // *************************
            if (game.isGameOver()) {
                if (game.getWinner() != 0) //win
                    reward = -1000;
                else //draw
                    reward = 300;
            }
            else {
                //reward = eval(game);
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
                //System.out.println("Seeded direct win");
                game.unMakeMove();
                break;
            }
            Board next_board = getStateActionValues(game);
            Boolean garuanteedWin = true;
            Boolean garuanteedLose = false;
            for (int j = 0; j < next_board.legalActions.size(); j++) {
                game.makeMove(next_board.legalActions.get(j));
                
                Boolean winExist = false;
                
                if (game.isGameOver() && game.getWinner() != 0) {
                    state_action_values.get(curr_board.state)[curr_board.legalActions.get(i)] = Double.toString(-1000.0);//seed -1000
                    //System.out.println("Seeded direct lose");
                    garuanteedWin = false;
                    garuanteedLose = false;
                    game.unMakeMove();
                    break;
                }else{
                    // ******************* Seeding Garuanteed Win *********************
                    Board board = getStateActionValues(game);
                    Boolean loseable = true;
                    for (int k = 0; k < board.legalActions.size(); k++) {
                        game.makeMove(board.legalActions.get(k));
                        if (game.isGameOver() && game.getWinner() != 0) {
                            winExist = true;
                            loseable = false;
                            game.unMakeMove();
                            break;
                        }
                        Board nboard = getStateActionValues(game);
                        Boolean loseExist = false;
                        for (int l = 0; l < nboard.legalActions.size(); l++) {
                            game.makeMove(nboard.legalActions.get(l));
                            if (game.isGameOver() && game.getWinner() != 0) {
                                loseExist = true;
                                game.unMakeMove();
                                break;
                            }
                            game.unMakeMove();
                        }
                        game.unMakeMove();
                        
                        if(!loseExist)
                            loseable = false;
                    }
                    
                    if(loseable){
                        garuanteedLose = true;
                    }
                    
                }
                game.unMakeMove();
                
                if(!winExist)
                    garuanteedWin = false;
                
                // *********************************************************************
            }
            game.unMakeMove();
            
            if(garuanteedWin){
                state_action_values.get(curr_board.state)[curr_board.legalActions.get(i)] = Double.toString(999.5);
                //System.out.println("Seeded garuanteed win");
            }
            
            if(garuanteedLose){
                state_action_values.get(curr_board.state)[curr_board.legalActions.get(i)] = Double.toString(-1000);
                //System.out.println("Seeded garuanteed lose");
            }
            
        }
    }
    
    private String[] seedQTable(GameStateModule game, Board curr_board) {
        if(curr_board.state.equals("00000000000000000000")){
            curr_board.q_values[2] = Double.toString(1000.0);
        }
        
        for (int i = 0; i < curr_board.legalActions.size(); i++) {
            game.makeMove(curr_board.legalActions.get(i));
            if (game.isGameOver() && game.getWinner() != 0) {
                curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(1000.0);//seed +1000
                System.out.println("Seeded direct win");
		System.out.println(curr_board.legalActions.get(i));
		System.out.println(Arrays.toString(curr_board.q_values));
                game.unMakeMove();
                break;
            }
            Board next_board = getStateActionValues(game);
            Boolean garuanteedWin = true;
            Boolean garuanteedLose = false;
            //Boolean garuanteedLose = true;
            for (int j = 0; j < next_board.legalActions.size(); j++) {
                game.makeMove(next_board.legalActions.get(j));
                
                Boolean winExist = false;
                
                if (game.isGameOver() && game.getWinner() != 0) {
                    curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(-1000.0);//seed -1000
                    System.out.println("Seeded direct lose");
		    System.out.println(curr_board.legalActions.get(i));
                    garuanteedWin = false;
                    garuanteedLose = false;
                    game.unMakeMove();
                    break;
                }else{
                    // ******************* Seeding Garuanteed Win *********************
                    Board board = getStateActionValues(game);
                    Boolean loseable = true;
                    for (int k = 0; k < board.legalActions.size(); k++) {
                        game.makeMove(board.legalActions.get(k));
                        if (game.isGameOver() && game.getWinner() != 0) {
                            winExist = true;
                            loseable = false;
                            game.unMakeMove();
                            break;
                        }
                        Board nboard = getStateActionValues(game);
                        Boolean loseExist = false;
                        for (int l = 0; l < nboard.legalActions.size(); l++) {
                            game.makeMove(nboard.legalActions.get(l));
                            if (game.isGameOver() && game.getWinner() != 0) {
                                loseExist = true;
                                game.unMakeMove();
                                break;
                            }
                            game.unMakeMove();
                        }
                        game.unMakeMove();
                        
                        if(!loseExist)
                            loseable = false;
                    }
                    
                    if(loseable){
                        garuanteedLose = true;
                    }
                    
                }
                game.unMakeMove();
                
                if(!winExist)
                    garuanteedWin = false;
                
                // *********************************************************************
            }
            game.unMakeMove();
            
            if(garuanteedWin){
                curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(999.5);
                System.out.println("Seeded garuanteed win");
		System.out.println(curr_board.legalActions.get(i));
            }
            
            if(garuanteedLose){
                curr_board.q_values[curr_board.legalActions.get(i)] = Double.toString(-1000);
                System.out.println("Seeded garuanteed lose at");
                System.out.println(curr_board.legalActions.get(i));
            }
            
        }
        return curr_board.q_values;
    }
    
    //Part 3
    //Evaluate the reward for the state before our action.
    
    private double eval(final GameStateModule game) {
        int player = game.getActivePlayer();
        int ourScore = 6*(threeInCol(game, player) + threeInRow(game, player) + threeInPos(game, player) + threeInNeg(game, player))
        + 3*(twoInCol(game, player) + twoInRow(game, player) + twoInPos(game, player) + twoInNeg(game, player));
        int theirScore = 7*(threeInCol(game, 2/player) + threeInRow(game, 2/player) + threeInPos(game, 2/player) + threeInNeg(game, 2/player))
        + 4*(twoInCol(game, 2/player) + twoInRow(game, 2/player) + twoInPos(game, 2/player) + twoInNeg(game, 2/player));
        return ourScore - theirScore;
    }
    
    private int twoInCol(final GameStateModule game, int player) {
        int count = 0;
        for(int i = 0; i < game.getWidth(); i++) {
            if(game.getHeight() == 4) {
                int j = 0; //Anything above this will not be able to make 4 in a column
                if((game.getAt(i, j) == player) && (game.getAt(i, j+1) == player) && (game.getAt(i, j+2) == 0))
                    count++;
            }
            else { //for 6x5
                for(int j = 0; j < 1; j++) {
                    if((game.getAt(i, j) == player) && (game.getAt(i, j+1) == player) && (game.getAt(i, j+2) == 0))
                        count++;
                }
            }
        }
        return count;
    }
    
    //Evaluate number of open three in a columns
    private int threeInCol(final GameStateModule game, int player) {
        int count = 0;
        for(int i = 0; i < game.getWidth(); i++) {
            if(game.getHeight() == 4) { //for 5x4
                int j = 0;
                if((game.getAt(i, j) == player) && (game.getAt(i, j+1) == player) && (game.getAt(i, j+2) == player) && game.getAt(i, j+3) == 0)
                    count++;
            }
            else { //for 6x5
                for(int j = 0; j < 2; j++) {
                    if((game.getAt(i, j) == player) && (game.getAt(i, j+1) == player) && (game.getAt(i, j+2) == player) && game.getAt(i, j+3) == 0)
                        count++;
                }
            }
        }
        return count;
    }
    
    //Evaluate number of open two in a rows
    private int twoInRow(final GameStateModule game, int player) {
        int count = 0;
        int maxHeight = 0;
        for(int i = 0; i < game.getWidth(); i++) {
            maxHeight = game.getHeightAt(i);
        }
        for(int i = 0; i < 3; i++) { //left to right
            for (int j = 0; j < maxHeight;j++){
                if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j) == player) && (game.getAt(i + 2, j) == 0))
                    count++;
            }
        }
        for(int i = game.getWidth()-1; i > 2; i--) { //right to left
            for(int j = 0; j < maxHeight; j++) {
                if((game.getAt(i,j) == player) && (game.getAt(i-1,j) == player) && (game.getAt(i-2,j) == 0))
                    count++;
            }
        }
        return count;
        
    }
    
    //Evaluate number of open three in a rows
    private int threeInRow(final GameStateModule game, int player) {
        int count = 0;
        int maxHeight = 0;
        for(int i = 0; i < game.getWidth(); i++){
            if(game.getHeightAt(i) > maxHeight)
                maxHeight = game.getHeightAt(i);
        }
        for(int i = 0; i < Math.floor(game.getWidth()/2); i++) { //left to right
            for(int j = 0; j < maxHeight; j++) {
                if((game.getAt(i,j) == player) && (game.getAt(i+1,j) == player) && (game.getAt(i+2,j) == player) &&(game.getAt(i+3,j) == 0))
                    count++;
            }
        }
        for(int i = game.getWidth()-1; i > Math.ceil(game.getWidth()/2); i--) { //right to left
            for(int j = 0; j < maxHeight; j++) {
                if((game.getAt(i,j) == player) && (game.getAt(i-1,j) == player) && (game.getAt(i-2,j) == player) &&(game.getAt(i-3,j) == 0))
                    count++;
            }
        }
        return count;
    }
    
    //Evaluate number of open two in a / diagonal
    private int twoInPos(final GameStateModule game, int player) {
        int count = 0;
        if(game.getHeight() == 4){ //if height is 4, no iterations needed
            //counting bottom up
            for(int i = 0; i < 3; i++) { //since rows is always of width 5
                for(int j = 0; j < 2; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j + 1) == player) && (game.getAt(i + 2, j + 2) == 0))
                        count++;
                }
            }
            //counting top bottom
            for(int i = 4; i > 1; i--) {
                for(int j = 3; j > 1; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j - 1) == player) && (game.getAt(i - 2, j - 2) == 0))
                        count++;
                }
            }
        }
        else { //if height is 5, iterations needed
            for(int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j + 1) == player) && (game.getAt(i + 2, j + 2) == 0))
                        count++;
                }
            }
            for(int i = 5; i > 1; i--) {
                for (int j = 4; j > 1; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j - 1) == player) && (game.getAt(i - 2, j - 2) == 0))
                        count++;
                }
            }
        }
        return count;
    }

    
    //Evaluate number of open diagonals in the positive slope /
    private int threeInPos(final GameStateModule game, int player) {
        int count = 0;
        if(game.getHeight() == 4){ //if height is 4, no iterations needed
            //counting bottom up
            for(int i = 0; i < 2; i++) {
                for(int j = 0; j < 1; j++) {
                    if((game.getAt(i,j) == player) && (game.getAt(i+1,j+1) == player) && (game.getAt(i+2,j+2) == player) && (game.getAt(i+3,j+3) == 0))
                        count++;
                }
            }
            //counting top bottom
            for(int i = 4; i > 2; i--) {
                for(int j = 3; j > 2; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j-1) == player) && (game.getAt(i - 2, j-2) == player) && (game.getAt(i - 3, j-3) == 0))
                        count++;
                }
            }
        }
        else { //if height is 5, iterations needed
            for(int i = 0; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j + 1) == player) && (game.getAt(i + 2, j + 2) == player) && (game.getAt(i+3, j+3) == 0))
                        count++;
                }
            }
            for(int i = 5; i > 2; i--) {
                for (int j = 4; j > 2; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j - 1) == player) && (game.getAt(i - 2, j - 2) == player) && (game.getAt(i-3,j-3) == 0))
                        count++;
                }
            }
        }
        return count;
    }
    
    //Evaluate two in an open negative slope
    private int twoInNeg (final GameStateModule game, int player) {
        int count = 0;
        if(game.getHeight() == 4){ //if height is 4, no iterations needed
            //counting bottom up
            for(int i = 0; i < 3; i++) { //since rows is always of width 5
                for(int j = 3; j > 1; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j - 1) == player) && (game.getAt(i + 2, j - 2) == 0))
                        count++;
                }
            }
            //counting top bottom
            for(int i = 4; i > 1; i--) {
                for(int j = 0; j < 2; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j + 1) == player) && (game.getAt(i - 2, j + 2) == 0))
                        count++;
                }
            }
        }
        else { //if height is 5, iterations needed
            for(int i = 0; i < 4; i++) {
                for (int j = 4; j > 1; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j - 1) == player) && (game.getAt(i + 2, j - 2) == 0))
                        count++;
                }
            }
            for(int i = 5; i > 1; i--) {
                for (int j = 0; j < 3; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j + 1) == player) && (game.getAt(i - 2, j + 2) == 0))
                        count++;
                }
            }
        }
        return count;
    }
    
    
    //Evaluate number of open diagonals in the negative slope
    private int threeInNeg(final GameStateModule game, int player) {
    int count = 0;
        if(game.getHeight() == 4){ //if height is 4, no iterations needed
            //counting bottom up
            for(int i = 0; i < 2; i++) { //since rows is always of width 5
                for(int j = 3; j > 2; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j - 1) == player) && (game.getAt(i + 2, j - 2) == player) && (game.getAt(i+3,j-3) == 0))
                        count++;
                }
            }
            //counting top bottom
            for(int i = 4; i > 2; i--) {
                for(int j = 0; j < 1; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j + 1) == player) && (game.getAt(i - 2, j + 2) == player) && (game.getAt(i-3, j+3) == 0))
                        count++;
                }
            }
        }
        else { //if height is 5, iterations needed
            for(int i = 0; i < 3; i++) {
                for (int j = 4; j > 1; j--) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i + 1, j - 1) == player) && (game.getAt(i + 2, j - 2) == player) && (game.getAt(i+3,j-3) == 0))
                        count++;
                }
            }
            for(int i = 5; i > 2; i--) {
                for (int j = 0; j < 2; j++) {
                    if ((game.getAt(i, j) == player) && (game.getAt(i - 1, j + 1) == player) && (game.getAt(i - 2, j + 2) == player) && (game.getAt(i-3, j+3) == 0))
                        count++;
                }
            }
        }
        return count;
    }
}

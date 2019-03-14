import java.util.Random;
import java.lang.Math;
/// A sample AI that uses a Monte Carlo approach to play Connect Four.

/**
 * A sampleAI that uses a Monte Carlo approach to play Connect Four.  Unlike the heuristic
 * searches and minimax approaches we've covered in class, the Monte Carlo player plays
 * Connect Four by simulating purely random games and choosing the move that has the highest
 * expected outcome.  Since the Monte Carlo player plays moves randomly,
 * it does not always play the optimal move (see if you can convince yourself about why this is),
 * but is good at strategic play and likes to make threats.
 * 
 * Unlike StupidAI and RandomAI, this AI player's getNextMove function will continues to play
 * random games indefinitely until the terminate flag is set.
 * 
 * @author Leonid Shamis
 */
public class alphabeta_magicint extends AIModule
{
	/// Random number generator to play random games.
	private final Random r = new Random(System.currentTimeMillis());
	/// Used as a helper when picking random moves.
	//private int[] moves;
	private int center_pref = 150;
	private boolean first = true;
	private int ourPlayer;
	private int lastMove;
	private int[] heights;
	private int heuristic = 0;
	private int [] weight = {0, 1, 50, 1000, Integer.MAX_VALUE/100};
	private int [] maxMoves;
	private int [] minMoves;
	private int temp;
	int weight_current = -Integer.MAX_VALUE;
	int move_current;	
	int target_depth = 1;
	int width;

	/// Simulates random games and chooses the move that leads to the highest expected value.
	@Override
	public void getNextMove(final GameStateModule state)
	{
		// Cache our index.
		ourPlayer = state.getActivePlayer();

		// Default to choosing the first column (should be fixed after a few rounds)
		chosenMove = 0;

		int AddorSubtract = 0;
		
		maxMoves = new int [state.getWidth()];
		minMoves = new int [state.getWidth()];
		for(int i = 0; i < state.getWidth(); i++)
		{
			if(i % 2 == 0)
				AddorSubtract = 1;
			else
				AddorSubtract = -1;
			
			maxMoves[i] = state.getWidth() / 2 + (i + 1) / 2 * AddorSubtract;
			// System.out.println("maxMoves[" + i + "] = " + maxMoves[i]);
		}
		for(int i = state.getWidth() - 1; i >= 0; i--)
		{
			minMoves[i] = maxMoves[state.getWidth() - i - 1];
			// System.out.println("minMoves[" + i + "] = " + minMoves[i]);
		}

		// Start simulating games! Continue until told to stop.
		
		int alpha = -Integer.MAX_VALUE;
		int beta = Integer.MAX_VALUE;
		target_depth = 1;
		int best = 0;
		width = state.getWidth();
	
		//printState(state);
			
		while(!terminate)
		{ 
			for(int i = 0; i < width; i++)
			{
				if(state.canMakeMove(maxMoves[i]))
				{
					state.makeMove(maxMoves[i]);
					temp = Min(state, 1, alpha, beta);			
					if(temp > alpha)
					{
						alpha = temp;
						best = maxMoves[i];
					}
					state.unMakeMove();
				}
			}
			if(!terminate)
			{
				chosenMove = best;
				//System.out.println("Move " + best + " at depth " + target_depth + " returned value of " + alpha);
			}
			target_depth++;
			alpha = -Integer.MAX_VALUE;
			beta = Integer.MAX_VALUE;
		}
	}

	private int Max(final GameStateModule state, int depth, int alpha, int beta)
	{
			if(terminate)
				return alpha;
			if(state.isGameOver())
			{
				if(state.getWinner() == ourPlayer)
					return Integer.MAX_VALUE/(depth * 2);
				else
					return -Integer.MAX_VALUE/(depth * 2);
			}
			if(depth < target_depth)
			{
				for(int i = 0; i < width; i++)
				{
					if(state.canMakeMove(maxMoves[i]))
					{
						state.makeMove(maxMoves[i]);					
			
						alpha = Math.max(alpha, Min(state, depth + 1, alpha, beta));
				
						if(beta <= alpha)
						{
							state.unMakeMove();
							return alpha;
						}

						state.unMakeMove();
	
					}
				}
				return alpha;
			}
			else 
				return heuristic(state);
	}
	
	private int Min(final GameStateModule state, int depth, int alpha, int beta)
	{
			if(terminate)
				return beta;
			if(state.isGameOver())
			{
				if(state.getWinner() == ourPlayer)
					return Integer.MAX_VALUE/(depth * 2);
				else
					return -Integer.MAX_VALUE/(depth * 2);
			}
			if(depth < target_depth)
			{
				for(int i = 0; i < width; i++)
				{
					if(state.canMakeMove(minMoves[i]))
					{
						state.makeMove(minMoves[i]);

						beta = Math.min(beta, Max(state, depth + 1, alpha, beta));
	
						if(beta <= alpha)
						{
							state.unMakeMove();
							return beta;
						}

						state.unMakeMove();
					}					
				}
				return beta;
			}
			else 
				return heuristic(state);
	}

	/// Returns a random legal move in a given state.
	/**
	 * Given a game state, returns the index of a column that is a legal move.
	 *
	 * @param state The state in which to get a legal move.
	 * @return A random legal column to drop a coin in.
	 */
	private int [] getMoves(final GameStateModule state)
	{
		int [] moves = new int[state.getWidth()]; 
		// Fill in what moves are legal.
		int numLegalMoves = 0;
		for(int i = 0; i < state.getWidth(); ++i)
			if(state.canMakeMove(i))
				moves[numLegalMoves++] = i;
		return moves;
	}

	// Given the result of the last game, update our chosen move.
	/**
	 * After simulating a game, updates the array containing all of the expected values
	 * and updates the chosen move to reflect the move with the highest positive expectation
	 * value.
	 *
	 * @param ourPlayer The index of the player representing us.
	 * @param result The result of the last game (0 for draw, 1 for player 1 win, etc.)
	 * @param values The array of expected values.
	 * @param move The move played that led to this outcome.
	 */
	private void updateGuess(final int result, int[] values, int move)
	{
		// On a draw, we can skip making changes.
		if(result == 0)
			return;

		// Update the expected value of this move depending on whether we win or lose.
		values[move] += (result == ourPlayer ? 1 : -1);

		// Update the move to be the best known move.  This is necessary since we need
		// to have the best move available at all times because we run forever.
		for(int i = 0; i < values.length; ++i)
			if(values[i] > values[chosenMove])
				chosenMove = i;
	}

	private int heuristic(final GameStateModule state)
	{
		if(state.isGameOver())
			{
				if(state.getWinner() == ourPlayer)
					return Integer.MAX_VALUE;   // removed / by 1000
				else
					return -Integer.MAX_VALUE;  // removed / by 1000
			}
		int ret = 0;
		int deltaret = 0;
		/*
		for(int i = 0; i < width - 3; i++)
			for(int j = 0; j < state.getHeight(); j++)
			{
				ret += checkhoriz(i, j, state);
			}
		deltaret = ret;
		for(int i = 0; i < width; i++)
			for(int j = 0; j < state.getHeight() - 3; j++)
				ret += checkvert(i, j, state);
		deltaret = ret - deltaret;
		deltaret = ret;
		for(int i = 0; i < width - 3; i++)
			for(int j = 0; j < state.getHeight() - 3; j++)
				ret += checkdowndiag(i, j, state); 
		deltaret = ret - deltaret;
		deltaret = ret;
		for(int i = 3; i < width; i++)
			for(int j = 0; j < state.getHeight() - 3; j++)
				ret += checkupdiag(i, j, state); 
		*/
		for(int i = 0; i < width - 3; i++)
			for(int j = 0; j < state.getHeight(); j++)
			{
				ret += checkhoriz(i, j, state);
				if(j < state.getHeight()-3) {
				    ret += checkvert(i, j, state);
				    if(i < width - 3) {
				        ret += checkdowndiag(i, j, state);
				    }
				    if(i >= 3) {
				        ret += checkupdiag(i, j, state);
				    }			    
				}
			}
		deltaret = ret - deltaret;
		deltaret = ret;
		
		return ret; 
	}

	private int checkhoriz(int x, int y, final GameStateModule state)
	{
		int [] player = new int[3];
		for(int i = 0; i < 3; i++)
			player[i] = 0;
		for(int i = 0; i < 4; i++)
			player[state.getAt(x + i, y)]++;
		if(player[1] > 0 && player[2] > 0)
			return 0;
		// add check to see if our win is above 2 of their wins below: (add later)
		if(player[ourPlayer] > 0) { 
		    // favor the middle
		    if(width == 8) {
		        if(x >= 3 || x <=6) {
                    return (weight[player[ourPlayer]] + center_pref);
                }
                else
                    return weight[player[ourPlayer]];
		    } // for width of 8
		    if(width == 7) {
		        if(x >= 2 || x <=4) {
                        return (weight[player[ourPlayer]] + center_pref);
                    }
                    else
                        return weight[player[ourPlayer]];
		    }
		}
		if(player[0] == 4)
			return 0;
		return -1 * weight[player[ourPlayer % 2 + 1]];
	}
	
	private int checkvert(int x, int y, final GameStateModule state)
	{
		int [] player = new int[3];
		for(int i = 0; i < 3; i++)
			player[i] = 0;
		for(int i = 0; i < 4; i++)
			player[state.getAt(x, y + i)]++;
		if(player[1] > 0 && player[2] > 0)
			return 0;
		if(player[ourPlayer] > 0) { 
		    // favor the middle
		    if(width == 8) {
		        if(x >= 3 || x <=6) {
                    return (weight[player[ourPlayer]] + center_pref);
                }
                else
                    return weight[player[ourPlayer]];
		    } // for width of 8
		    if(width == 7) {
		        if(x >= 2 || x <=4) {
                        return (weight[player[ourPlayer]] + center_pref);
                    }
                    else
                        return weight[player[ourPlayer]];
		    }
		}
		if(player[0] == 4)
			return 0;
		return -1 * weight[player[ourPlayer % 2 + 1]];
	}
	
	private int checkdowndiag(int x, int y, final GameStateModule state)
	{
		int [] player = new int[3];
		for(int i = 0; i < 3; i++)
			player[i] = 0;
		for(int i = 0; i < 4; i++)
			player[state.getAt(x + i, y + i)]++;
		if(player[1] > 0 && player[2] > 0)
			return 0;
		if(player[ourPlayer] > 0) { 
		    // favor the middle
		    if(width == 8) {
		        if(x >= 3 || x <=6) {
                    return (weight[player[ourPlayer]] + center_pref);
                }
                else
                    return weight[player[ourPlayer]];
		    } // for width of 8
		    if(width == 7) {
		        if(x >= 2 || x <=4) {
                        return (weight[player[ourPlayer]] + center_pref);
                    }
                    else
                        return weight[player[ourPlayer]];
		    }
		}
		if(player[0] == 4)
			return 0;
		return -1 * weight[player[ourPlayer % 2 + 1]];
	}
	
	private int checkupdiag(int x, int y, final GameStateModule state)
	{
		int [] player = new int[3];
		for(int i = 0; i < 3; i++)
			player[i] = 0;
		for(int i = 0; i < 4; i++)
			player[state.getAt(x - i, y + i)]++;
		if(player[1] > 0 && player[2] > 0)
			return 0;
		if(player[ourPlayer] > 0) { 
		    // favor the middle
		    if(width == 8) {
		        if(x >= 3 || x <=6) {
                    return (weight[player[ourPlayer]] + center_pref);
                }
                else
                    return weight[player[ourPlayer]];
		    } // for width of 8
		    if(width == 7) {
		        if(x >= 2 || x <=4) {
                        return (weight[player[ourPlayer]] + center_pref);
                    }
                    else
                        return weight[player[ourPlayer]];
		    }
		}
		if(player[0] == 4)
			return 0;
		return -1 * weight[player[ourPlayer % 2 + 1]];
	}

	private void printState(final GameStateModule state)
	{
		System.out.println("Heuristic = " + heuristic(state));
		for(int j = state.getHeight() - 1; j >= 0; j--)
		{
			for(int i = 0; i < state.getWidth(); i++)
				System.out.print(state.getAt(i, j));
			System.out.println("");
		}		
		System.out.println("");
	}
}

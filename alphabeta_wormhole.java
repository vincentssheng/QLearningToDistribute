import java.util.Arrays;

public class alphabeta_wormhole extends AIModule {
	private int us; //save a call to game.getActivePlayer()
	private int them; //save a call to game.getActivePlayer()
	private int width; //save a call to game.getWidth()
	private int height; //save a call to game.getHeight()
	private int depth = 0;
	private alphabetaTree tree;
	private GameStateModule state;
	
	public void getNextMove(final GameStateModule game) {
		if (us == 0)
			initialize(game);
		else
			update(game);
		while (!terminate) {
			depth++;
			//System.out.print(depth + ", ");
			chosenMove = bestMove(game);
			//System.out.print(chosenMove + " ");
		}
	}
	
	private void initialize(final GameStateModule game) {
		us = game.getActivePlayer();
		if (us == 1)
			them = 2;
		else
			them = 1;
		width = game.getWidth();
		height = game.getHeight();
		state = game.copy();
		tree = new alphabetaTree(width);
	}
	
	private void update(final GameStateModule game) {
		int y;
		for (int i = 0; i < width; i++) {
			y = state.getHeightAt(i);
			if (y >= height)
				continue;
			if (game.getAt(i, y) == us) {
				if (tree.getChild(i) == null) {
					tree.addChild(i);
					moveAndAnalyze(state, i, tree.getChild(i));
					setOrdering(state, tree.getChild(i));
				} else
					state.makeMove(i);
				tree = tree.getChild(i);
				break;
			}
		}
		for (int i = 0; i < width; i++) {
			y = state.getHeightAt(i);
			if (y >= height)
				continue;
			if (game.getAt(i, y) == them) {
				if (tree.getChild(i) == null) {
					tree.addChild(i);
					moveAndAnalyze(state, i, tree.getChild(i));
					setOrdering(state, tree.getChild(i));
				} else
					state.makeMove(i);
				tree = tree.getChild(i);
				break;
			}
		}
		/*for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (game.getAt(i, j) != state.getAt(i, j))
					System.out.println("inconsistency detected");*/
		//depth -= 3;
		depth = tree.getDepth() - 1;
	}
	
	private int bestMove(final GameStateModule game) {
		int move = chosenMove;
		int alpha = Integer.MIN_VALUE; //MIN_VALUE is one less than -MAX_VALUE, which is
									   //the smallest value that minPick() can return, so
									   //we are guaranteed a legal move
		int bestT = 0;
		for (int t = 0; t < width; t++) {
			int i = tree.ordering[t];
			if (game.canMakeMove(i)) {
				if (tree.getChild(i) == null) {
					tree.addChild(i);
					moveAndAnalyze(game, i, tree.getChild(i));
					setOrdering(game, tree.getChild(i));
				} else
					game.makeMove(i);
				int result = minPick(game, depth - 1, tree.getChild(i), alpha, Integer.MAX_VALUE);
				if (terminate)
					return chosenMove;
				if (result > alpha) {
					alpha = result;
					move = i;
					bestT = t;
				}
				game.unMakeMove();
			}
		}
		if (bestT > 0) {
			byte temp = tree.ordering[bestT];
			int it;
			for (it = bestT; it > 0; it--) {
				tree.ordering[it] = tree.ordering[it-1];
			}
			tree.ordering[it] = temp;
		}
		return move;
	}
	
	private int maxPick(final GameStateModule game, int depth, alphabetaTree tree, int alpha, final int beta) {
		if (game.isGameOver()) {
			if (game.getWinner() == 0)
				return 0;
			else if (us == game.getWinner())
				return Integer.MAX_VALUE;
			else
				return -Integer.MAX_VALUE;
		} else if (depth == 0)
			return evaluate(tree);
		else {
			int bestT = 0;
			for (int t = 0; t < width; t++) {
				int i = tree.ordering[t];
				if (game.canMakeMove(i)) {
					if (tree.getChild(i) == null) {
						tree.addChild(i);
						moveAndAnalyze(game, i, tree.getChild(i));
						setOrdering(game, tree.getChild(i));
					} else
						game.makeMove(i);
					int result = minPick(game, depth - 1, tree.getChild(i), alpha, beta);
					if (terminate)
						return 0; //if terminate is true, the return value is ignored
					if (result == Integer.MAX_VALUE) {
						game.unMakeMove();
						if (t > 0) {
							byte temp = tree.ordering[t];
							int it;
							for (it = t; it > 0; it--) {
								tree.ordering[it] = tree.ordering[it-1];
							}
							tree.ordering[it] = temp;
						}
						return result; //not exactly alpha-beta, but we can't get higher than this
					} else if (result > beta) {
						game.unMakeMove();
						if (t > 0) {
							byte temp = tree.ordering[t];
							int it;
							for (it = t; it > 0; it--) {
								tree.ordering[it] = tree.ordering[it-1];
							}
							tree.ordering[it] = temp;
						}
						return result; //prune
					} else if (result > alpha) {
						alpha = result;
						bestT = t;
					}
					game.unMakeMove();
				}
			}
			if (bestT > 0) {
				byte temp = tree.ordering[bestT];
				int it;
				for (it = bestT; it > 0; it--) {
					tree.ordering[it] = tree.ordering[it-1];
				}
				tree.ordering[it] = temp;
			}
			return alpha;
		}
	}
	
	private int minPick(final GameStateModule game, int depth, alphabetaTree tree, final int alpha, int beta) {
		if (game.isGameOver()) {
			if (game.getWinner() == 0)
				return 0;
			else if (us == game.getWinner())
				return Integer.MAX_VALUE;
			else
				return -Integer.MAX_VALUE;
		} else if (depth == 0)
			return evaluate(tree);
		else {
			int bestT = 0;
			for (int t = 0; t < width; t++) {
				int i = tree.ordering[t];
				if (game.canMakeMove(i)) {
					if (tree.getChild(i) == null) {
						tree.addChild(i);
						moveAndAnalyze(game, i, tree.getChild(i));
						setOrdering(game, tree.getChild(i));
					} else
						game.makeMove(i);
					int result = maxPick(game, depth - 1, tree.getChild(i), alpha, beta);
					if (terminate)
						return 0; //if terminate is true, the return value is ignored
					if (result == -Integer.MAX_VALUE) {
						game.unMakeMove();
						if (t > 0) {
							byte temp = tree.ordering[t];
							int it;
							for (it = t; it > 0; it--) {
								tree.ordering[it] = tree.ordering[it-1];
							}
							tree.ordering[it] = temp;
						}
						return result; //not exactly alpha-beta, but we can't get lower than this
					} else if (result < alpha) {
						game.unMakeMove();
						if (t > 0) {
							byte temp = tree.ordering[t];
							int it;
							for (it = t; it > 0; it--) {
								tree.ordering[it] = tree.ordering[it-1];
							}
							tree.ordering[it] = temp;
						}
						return result; //prune
					} else if (result < beta) {
						beta = result;
						bestT = t;
					}
					game.unMakeMove();
				}
			}
			if (bestT > 0) {
				byte temp = tree.ordering[bestT];
				int it;
				for (it = bestT; it > 0; it--) {
					tree.ordering[it] = tree.ordering[it-1];
				}
				tree.ordering[it] = temp;
			}
			return beta;
		}
	}
	
	private void setOrdering(GameStateModule game, alphabetaTree tree) {
		byte[] values = new byte[width]; //value[i] is the value of tree.children[tree.ordering[i]], not tree.children[i]
		for (int x = 0; x < width; x++) {
			int col = tree.ordering[x];
			//int y = game.getHeightAt(col);
			/*values[x] = 100; //arbitrary constant
			int j = tree.threats[col] - y;
			if ((j & 1) == 0 && j < values[x])
				values[x] = (byte) j;
			j = tree.threats[alphabetaTree.ODD2 + col] - y;
			if ((j & 1) == 1 && j < values[x])
				values[x] = (byte) j;
			j = tree.threats[alphabetaTree.EVEN1 + col] - y;
			if ((j & 1) == 0 && j < values[x])
				values[x] = (byte) j;
			j = tree.threats[alphabetaTree.EVEN2 + col] - y;
			if ((j & 1) == 1 && j < values[x])
				values[x] = (byte) j;*/
			values[x] = (byte) (Math.abs(width-1 - (x<<1)) - game.getHeightAt(col));
			
			if (x > 0 && values[x] < values[x-1]) {
				//insertion sort
				byte temp = values[x];
				values[x] = values[x-1];
				tree.ordering[x] = tree.ordering[x-1];
				int i;
				for (i = x-1; i >= 0; i--) {
					if (i > 0 && temp < values[i-1]) {
						values[i] = values[i-1];
						tree.ordering[i] = tree.ordering[i-1];
					} else
						break;
				}
				values[i] = temp;
				tree.ordering[i] = (byte) col;
			}
		}
	}
	
	private int evaluate(alphabetaTree tree) {
		int score = 0;
		int odd = 0;
		int mixed = 0;
		int even = 0;
		for (int i = 0; i < width; i++) {
			boolean temp = false;
			if (tree.threats[i] < tree.threats[alphabetaTree.EVEN2 + i]) {
				odd--;
				temp = true;
			} else if (tree.threats[i] > tree.threats[alphabetaTree.EVEN2 + i])
				even++;
			if (tree.threats[alphabetaTree.ODD2 + i] < tree.threats[alphabetaTree.EVEN1 + i]) {
				odd++;
				if (temp)
					mixed++;
			}
		}
		/*if (odd < 0) {
			1 wins
		} else if (odd == 0) {
			if (mixed % 2 == 1) {
				1 wins
			} else {//if (mixed % 2 == 0)
				if (mixed == 0) {
					if (even > 0)
						2 wins
				} else {
					2 wins
				}
			}
		} else if (odd == 1) {
			if (mixed > 0) {
				2 wins
			}
		} else {//if (odd > 1)
			2 wins
		}*/
		score -= 10000 << odd;
		if (odd == 0)
			if ((mixed & 1) == 1)
				score += 1000;
			else
				if (mixed > 0)
					score = -1000;
		else if (odd == 1)
			if (mixed > 0)
				score -= 1000 << mixed;
		score -= 100 << even;
		
		if (us == 1)
			return tree.stateValue + score;
		else
			return tree.stateValue - score;
	}
	
	private void moveAndAnalyze(final GameStateModule game, int x, alphabetaTree tree) {
		int y = game.getHeightAt(x);
		game.makeMove(x);
		int player = game.getAt(x, y);
		for (int i = x-1; i <= x+1; i += 2) {
			for (int j = y-1; j <= y+1; j++) {
				if (i < 0 || i >= width || j < 0 || j >= height)
					continue;
				if (game.getAt(i, j) == us)
					tree.stateValue--;
				else if (game.getAt(i, j) == them)
					tree.stateValue++;
				else if (player == us)
					tree.stateValue++;
				else
					tree.stateValue--;
			}
		}
		if (player == us)
			tree.stateValue -= Math.abs(width-1 - (x<<1));
		else
			tree.stateValue += Math.abs(width-1 - (x<<1));
		byte t = (byte) (x-3);
		byte i[] = new byte[7]; //for iterating over x's
		byte j[] = new byte[7]; //for iterating over y's
		for (int it = 0; it < 7; it++) {
			i[it] = t; //i = [x-3..x+3]
			t++;
		}
		for (int rep = 0; rep < 3; rep++) {
			boolean examining = true;
			t = (byte) (y-3);
			if (rep == 0)
				for (int it = 0; it < 7; it++) {
					j[it] = (byte) y; //i = [y..y]
					t++;
				}
			else if (rep == 1)
				for (int it = 0; it < 7; it++) {
					j[it] = t; //i = [y-3..y+3]
					t++;
				}
			else
				for (int it = 6; it >= 0; it--) {
					j[it] = t; //i = [y+3..y-3]
					t++;
				}
			States state = States.uuuOuuu;
			int leftOffset = -1;
			int rightOffset = -1;
			//WARNING: finite state machine
			//no loose clothing or unsupervised children
			while (examining) {
				switch (state) {
					case uuuOuuu:
						if (game.getAt(i[2], j[2]) == 0 && game.getAt(i[2], j[2]-1) == 0 && i[2] >= 0 && i[2] < width && j[2] > 0 && j[2] < height) {
							if ((j[2] & 1) == 0) {
								if (player == 1) {
									leftOffset = alphabetaTree.ODD1;
								} else {
									leftOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									leftOffset = alphabetaTree.EVEN1;
								} else {
									leftOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[leftOffset + i[2]] > j[2]) {
								state = States.uuEOuuu;
							} else {
								state = States.XXXOuuu; //uuXOuuu
							}
						} else if (game.getAt(i[2], j[2]) == player) {
							state = States.uuOOuuu;
						} else {
							state = States.XXXOuuu; //uuXOuuu
						}
						break;
					case uuEOuuu:
						if (game.getAt(i[1], j[1]) == player) {
							state = States.uOEOuuu;
						} else {
							state = States.XXEOuuu; //uXEOuuu OR uEEOuuu
						}
						break;
					case uuOOuuu:
						if (game.getAt(i[1], j[1]) == 0 && game.getAt(i[1], j[1]-1) == 0 && i[1] >= 0 && i[1] < width && j[1] > 0 && j[1] < height) {
							if ((j[1] & 1) == 0) {
								if (player == 1) {
									leftOffset = alphabetaTree.ODD1;
								} else {
									leftOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									leftOffset = alphabetaTree.EVEN1;
								} else {
									leftOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[leftOffset + i[1]] > j[1]) {
								state = States.uEOOuuu;
							} else {
								state = States.XXOOuuu; //uXOOuuu
							}
						} else if (game.getAt(i[1], j[1]) == player) {
							state = States.uOOOuuu;
						} else {
							state = States.XXOOuuu; //uXOOuuu
						}
						break;
					case uEOOuuu:
						if (game.getAt(i[0], j[0]) == player) {
							tree.threats[leftOffset + i[1]] = j[1]; //OEOOOuuu
							state = States.XXOOuuu;
						} else {
							state = States.XEOOuuu; //XEOOuuu OR EEOOuuu;
						}
						break;
					case uOEOuuu:
						if (game.getAt(i[0], j[0]) == player) {
							tree.threats[leftOffset + i[2]] = j[2]; //OOEOuuu
							state = States.XXXOuuu;
						} else {
							state = States.XOEOuuu; //XOEOuuu OR EOEOuuu
						}
						break;
					case uOOOuuu:
						if (game.getAt(i[0], j[0]) == 0 && game.getAt(i[0], j[0]-1) == 0 && i[0] >= 0 && i[0] < width && j[0] > 0 && j[0] < height) {
							if ((j[0] & 1) == 0) {
								if (player == 1) {
									if (tree.threats[alphabetaTree.ODD1 + i[0]] > j[0])
										tree.threats[alphabetaTree.ODD1 + i[0]] = j[0]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.ODD2 + i[0]] > j[0])
										tree.threats[alphabetaTree.ODD2 + i[0]] = j[0]; //EOOOuuu
								}
							} else {
								if (player == 1) {
									if (tree.threats[alphabetaTree.EVEN1 + i[0]] > j[0])
										tree.threats[alphabetaTree.EVEN1 + i[0]] = j[0]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.EVEN2 + i[0]] > j[0])
										tree.threats[alphabetaTree.EVEN2 + i[0]] = j[0]; //EOOOuuu
								}
							}
						}
						if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									if (tree.threats[alphabetaTree.ODD1 + i[4]] > j[4])
										tree.threats[alphabetaTree.ODD1 + i[4]] = j[4]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.ODD2 + i[4]] > j[4])
										tree.threats[alphabetaTree.ODD2 + i[4]] = j[4]; //EOOOuuu
								}
							} else {
								if (player == 1) {
									if (tree.threats[alphabetaTree.EVEN1 + i[4]] > j[4])
										tree.threats[alphabetaTree.EVEN1 + i[4]] = j[4]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.EVEN2 + i[4]] > j[4])
										tree.threats[alphabetaTree.EVEN2 + i[4]] = j[4]; //EOOOuuu
								}
							}
						}
						examining = false; //XOOOXuu
						break;
					case XEOOuuu:
						if (game.getAt(i[4], j[4]) == player) {
							tree.threats[leftOffset + i[1]] = j[1]; //XEOOOuu
							state = States.XXOOOuu;
						} else if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[4]] > j[4]) {
								state = States.XXOOEuu; //XEOOEuu
							} else {
								examining = false; //XEOOXuu
							}
						} else {
							examining = false; //XEOOXuu
						}
						break;
					case XOEOuuu:
						if (game.getAt(i[4], j[4]) == player) {
							tree.threats[leftOffset + i[2]] = j[2]; //XOEOOuu
							state = States.XXXOOuu;
						} else if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[4]] > j[4]) {
								state = States.XXXOEuu; //XOEOEuu
							} else {
								examining = false; //XOEOXuu
							}
						} else {
							examining = false; //XOEOXuu
						}
						break;
					case XXEOuuu:
						if (game.getAt(i[4], j[4]) == player) {
							state = States.XXEOOuu;
						} else if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[4]] > j[4]) {
								state = States.XXXOEuu; //XXEOEuu
							} else {
								examining = false; //XXEOXuu
							}
						} else {
							examining = false; //XXEOXuu
						}
						break;
					case XXEOOuu:
						if (game.getAt(i[5], j[5]) == player) {
							tree.threats[leftOffset + i[2]] = j[2]; //XXEOOOu
							state = States.XXXOOOu;
						} else if (game.getAt(i[5], j[5]) == 0 && game.getAt(i[5], j[5]-1) == 0 && i[5] >= 0 && i[5] < width && j[5] > 0 && j[5] < height) {
							if ((j[5] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[5]] > j[5]) {
								state = States.XXXOOEu; //XXEOOEu
							} else {
								examining = false; //XXEOOXu
							}
						} else {
							examining = false; //XXEOOXu
						}
						break;
					case XXOOuuu:
						if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[4]] > j[4]) {
								state = States.XXOOEuu;
							} else {
								examining = false; //XXOOXuu
							}
						} else if (game.getAt(i[4], j[4]) == player) {
							state = States.XXOOOuu;
						} else {
							examining = false; //XXOOXuu
						}
						break;
					case XXOOEuu:
						if (game.getAt(i[5], j[5]) == player) {
							tree.threats[rightOffset + i[4]] = j[4]; //XXOOEOu
							examining = false; //XXOOEOX OR XXOOEOE
						} else {
							examining = false; //XXOOEXu OR XXOOEEu
						}
						break;
					case XXOOOuu:
						if (game.getAt(i[5], j[5]) == 0 && game.getAt(i[5], j[5]-1) == 0 && i[5] >= 0 && i[5] < width && j[5] > 0 && j[5] < height) {
							if ((j[5] & 1) == 0) {
								if (player == 1) {
									if (tree.threats[alphabetaTree.ODD1 + i[5]] > j[5])
										tree.threats[alphabetaTree.ODD1 + i[5]] = j[5]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.ODD2 + i[5]] > j[5])
										tree.threats[alphabetaTree.ODD2 + i[5]] = j[5]; //EOOOuuu
								}
							} else {
								if (player == 1) {
									if (tree.threats[alphabetaTree.EVEN1 + i[5]] > j[5])
										tree.threats[alphabetaTree.EVEN1 + i[5]] = j[5]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.EVEN2 + i[5]] > j[5])
										tree.threats[alphabetaTree.EVEN2 + i[5]] = j[5]; //EOOOuuu
								}
							}
						}
						examining = false; //XXOOOEX OR XXOOOEE
						break;
					case XXXOuuu:
						if (game.getAt(i[4], j[4]) == 0 && game.getAt(i[4], j[4]-1) == 0 && i[4] >= 0 && i[4] < width && j[4] > 0 && j[4] < height) {
							if ((j[4] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[4]] > j[4]) {
								state = States.XXXOEuu;
							} else {
								examining = false; //XXXOXuu
							}
						} else if (game.getAt(i[4], j[4]) == player) {
							state = States.XXXOOuu;
						} else {
							examining = false; //XXXOXuu
						}
						break;
					case XXXOEuu:
						if (game.getAt(i[5], j[5]) == player) {
							state = States.XXXOEOu;
						} else {
							examining = false; //XXXOEXu OR XXXOEEu
						}
						break;
					case XXXOEOu:
						if (game.getAt(i[6], j[6]) == player) {
							tree.threats[rightOffset + i[4]] = j[4]; //XXXOEOO
						}
						examining = false; //XXXOEOX OR XXXOEOE
						break;
					case XXXOOuu:
						if (game.getAt(i[5], j[5]) == 0 && game.getAt(i[5], j[5]-1) == 0 && i[5] >= 0 && i[5] < width && j[5] > 0 && j[5] < height) {
							if ((j[5] & 1) == 0) {
								if (player == 1) {
									rightOffset = alphabetaTree.ODD1;
								} else {
									rightOffset = alphabetaTree.ODD2;
								}
							} else {
								if (player == 1) {
									rightOffset = alphabetaTree.EVEN1;
								} else {
									rightOffset = alphabetaTree.EVEN2;
								}
							}
							if (tree.threats[rightOffset + i[5]] > j[5]) {
								state = States.XXXOOEu;
							} else {
								examining = false; //XXXOOXu
							}
						} else if (game.getAt(i[5], j[5]) == player) {
							state = States.XXXOOOu;
						} else {
							examining = false; //XXXOOXu
						}
						break;
					case XXXOOEu:
						if (game.getAt(i[6], j[6]) == player) {
							tree.threats[rightOffset + i[5]] = j[5]; //XXXOOEO
						}
						examining = false; //XXXOOEX OR XXXOOEE
						break;
					case XXXOOOu:
						if (game.getAt(i[6], j[6]) == 0 && game.getAt(i[6], j[6]-1) == 0 && i[6] >= 0 && i[6] < width && j[6] > 0 && j[6] < height) {
							if ((j[6] & 1) == 0) {
								if (player == 1) {
									if (tree.threats[alphabetaTree.ODD1 + i[6]] > j[6])
										tree.threats[alphabetaTree.ODD1 + i[6]] = j[6]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.ODD2 + i[6]] > j[6])
										tree.threats[alphabetaTree.ODD2 + i[6]] = j[6]; //EOOOuuu
								}
							} else {
								if (player == 1) {
									if (tree.threats[alphabetaTree.EVEN1 + i[6]] > j[6])
										tree.threats[alphabetaTree.EVEN1 + i[6]] = j[6]; //EOOOuuu
								} else {
									if (tree.threats[alphabetaTree.EVEN2 + i[6]] > j[6])
										tree.threats[alphabetaTree.EVEN2 + i[6]] = j[6]; //EOOOuuu
								}
							}
						}
						examining = false; //XXXOOOX
						break;
					default:
						System.out.println("state error");
						break;
				}
			}
		}
	}
}

enum States {uuuOuuu, uuEOuuu, uuOOuuu, uEOOuuu, uOEOuuu, uOOOuuu, XEOOuuu, XOEOuuu, XXEOuuu, XXEOOuu, XXOOuuu, XXOOEuu, XXOOOuu, XXXOuuu, XXXOEuu, XXXOEOu, XXXOOuu, XXXOOEu, XXXOOOu}

class alphabetaTree {
	static int WIDTH;
	byte[] ordering;
	alphabetaTree[] children;
	static int ODD1; //the index into threats for player 1's attacks on odd rows
	static int ODD2; //the index into threats for player 2's attacks on odd rows
	static int EVEN1; //the index into threats for player 1's attacks on even rows
	static int EVEN2; //the index into threats for player 2's attacks on even rows
	byte[] threats; //the the location of the lowest attack of a given type (ODD1, ODD2, EVEN1, EVEN2) for each row
	static int QUAD_WIDTH; //the length of the threats array (4*WIDTH)
	int stateValue;
	
	alphabetaTree(int width) {
		WIDTH = width;
		ODD1 = 0;
		ODD2 = width;
		EVEN1 = ODD2 + width;
		EVEN2 = EVEN1 + width;
		QUAD_WIDTH = EVEN2 + width;
		threats = new byte[QUAD_WIDTH];
		Arrays.fill(threats, Byte.MAX_VALUE);
		children = new alphabetaTree[width];
		ordering = new byte[width];
		for (byte i = 0; i < width; i++)
			ordering[i] = i;
		stateValue = 0;
	}
	
	alphabetaTree(alphabetaTree parent) {
		children = new alphabetaTree[WIDTH];
		ordering = new byte[WIDTH];
		System.arraycopy(parent.ordering, 0, ordering, 0, WIDTH);
		threats = new byte[QUAD_WIDTH];
		System.arraycopy(parent.threats, 0, threats, 0, QUAD_WIDTH);
		stateValue = parent.stateValue;
	}
	
	public void addChild(int branch) {
		children[branch] = new alphabetaTree(this);
	}
	
	public alphabetaTree getChild(int branch) {
		return children[branch];
	}
	
	public int getDepth() {
		int depth = -1;
		int temp;
		for (int i = 0; i < WIDTH; i++) {
			if (children[i] == null)
				continue;
			temp = children[i].getDepth();
			if (temp > depth)
				depth = temp;
		}
		return depth + 1;
	}
}

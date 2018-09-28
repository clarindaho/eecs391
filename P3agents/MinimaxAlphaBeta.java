package edu.cwru.sepia.agent.minimax;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

public class MinimaxAlphaBeta extends Agent {

	// comparing two states
	public class GameStateChildComparator implements Comparator<GameStateChild> {
		@Override
		public int compare(GameStateChild o1, GameStateChild o2) {
			if(o2.state.getUtility() > o1.state.getUtility()) {
				return 1;
			}
			
			else if(o1.state.getUtility() > o2.state.getUtility()) {
				return -1;
			}
			
			else {
				return 0;
			}
				
		}	
	}

	private final int numPlys;

	public MinimaxAlphaBeta(int playernum, String[] args) {
		super(playernum);

		if (args.length < 1) {
			System.err.println("You must specify the number of plys");
			System.exit(1);
		}

		numPlys = Integer.parseInt(args[0]);
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
		GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate), numPlys, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY);

		return bestChild.action;
	}

	@Override
	public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

	}

	@Override
	public void savePlayerData(OutputStream os) {

	}

	@Override
	public void loadPlayerData(InputStream is) {

	}

	/**
	 * You will implement this.
	 *
	 * This is the main entry point to the alpha beta search. Refer to the slides,
	 * assignment description and book for more information.
	 *
	 * Try to keep the logic in this function as abstract as possible (i.e. move as
	 * much SEPIA specific code into other functions and methods)
	 *
	 * @param node
	 *            The action and state to search from
	 * @param depth
	 *            The remaining number of plys under this node
	 * @param alpha
	 *            The current best value for the maximizing node from this node to
	 *            the root
	 * @param beta
	 *            The current best value for the minimizing node from this node to
	 *            the root
	 * @return The best child of this node with updated values
	 */
	public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta) {
		System.out.println(node.state.getTurnNumber());
		List<GameStateChild> children = node.state.getChildren();
		children = orderChildrenWithHeuristics(children);
		
		if (depth == 0) {
			return node;
		}
		// footman's turn
		if (node.state.getTurnNumber() == 0) {
			double v = Double.NEGATIVE_INFINITY;
			GameStateChild bestChild = null;
			for(GameStateChild child : children) {
				v = Math.max(v, alphaBetaSearch(child, depth - 1, alpha, beta).state.getUtility());
				alpha = Math.max(alpha, v);
				if(alpha == v) {
					bestChild = child;
				}
				if(beta <= alpha) {
					break;
				}
			}
			return bestChild;
		}
		// archer's turn
		else {
			double v = Double.POSITIVE_INFINITY;
			GameStateChild bestChild = null;
			for(GameStateChild child : children) {
				v = Math.min(v, alphaBetaSearch(child, depth - 1, alpha, beta).state.getUtility());
				beta = Math.min(beta, v);
				if(beta == v) {
					bestChild = child;
				}
				if(beta <= alpha) {
					break;
				}
			}
			return bestChild;
		}
	}

	/**
	 * You will implement this.
	 *
	 * Given a list of children you will order them according to heuristics you make
	 * up. See the assignment description for suggestions on heuristics to use when
	 * sorting.
	 *
	 * Use this function inside of your alphaBetaSearch method.
	 *
	 * Include a good comment about what your heuristics are and why you chose them.
	 *
	 * @param children
	 * @return The list of children sorted by your heuristic.
	 */
	public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children) {
		Collections.sort(children, new GameStateChildComparator());
		return children;
	}
}
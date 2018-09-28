package edu.cwru.sepia.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

public class AstarAgent extends Agent {

	class MapLocation {
		public int x, y;
		public MapLocation cameFrom;
		public float cost;

		public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
			this.x = x;
			this.y = y;
			this.cameFrom = cameFrom;
			this.cost = cost;
		}
	}

	Stack<MapLocation> path;
	int footmanID, townhallID, enemyFootmanID;
	MapLocation nextLoc;

	private long totalPlanTime = 0; // nsecs
	private long totalExecutionTime = 0; // nsecs

	public AstarAgent(int playernum) {
		super(playernum);

		System.out.println("Constructed AstarAgent");
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
		// get the footman location
		List<Integer> unitIDs = newstate.getUnitIds(playernum);

		if (unitIDs.size() == 0) {
			System.err.println("No units found!");
			return null;
		}

		footmanID = unitIDs.get(0);

		// double check that this is a footman
		if (!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman")) {
			System.err.println("Footman unit not found");
			return null;
		}

		// find the enemy playernum
		Integer[] playerNums = newstate.getPlayerNumbers();
		int enemyPlayerNum = -1;
		for (Integer playerNum : playerNums) {
			if (playerNum != playernum) {
				enemyPlayerNum = playerNum;
				break;
			}
		}

		if (enemyPlayerNum == -1) {
			System.err.println("Failed to get enemy playernumber");
			return null;
		}

		// find the townhall ID
		List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

		if (enemyUnitIDs.size() == 0) {
			System.err.println("Failed to find enemy units");
			return null;
		}

		townhallID = -1;
		enemyFootmanID = -1;
		for (Integer unitID : enemyUnitIDs) {
			Unit.UnitView tempUnit = newstate.getUnit(unitID);
			String unitType = tempUnit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				townhallID = unitID;
			} else if (unitType.equals("footman")) {
				enemyFootmanID = unitID;
			} else {
				System.err.println("Unknown unit type");
			}
		}

		if (townhallID == -1) {
			System.err.println("Error: Couldn't find townhall");
			return null;
		}

		long startTime = System.nanoTime();
		path = findPath(newstate);
		totalPlanTime += System.nanoTime() - startTime;

		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
		long startTime = System.nanoTime();
		long planTime = 0;

		Map<Integer, Action> actions = new HashMap<Integer, Action>();

		if (shouldReplanPath(newstate, statehistory, path)) {
			long planStartTime = System.nanoTime();
			path = findPath(newstate);
			planTime = System.nanoTime() - planStartTime;
			totalPlanTime += planTime;
		}

		Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

		int footmanX = footmanUnit.getXPosition();
		int footmanY = footmanUnit.getYPosition();

		if (!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

			// stat moving to the next step in the path
			nextLoc = path.pop();

			System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
		}

		if (nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y)) {
			int xDiff = nextLoc.x - footmanX;
			int yDiff = nextLoc.y - footmanY;

			// figure out the direction the footman needs to move in
			Direction nextDirection = getNextDirection(xDiff, yDiff);

			actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
		} else {
			Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

			// if townhall was destroyed on the last turn
			if (townhallUnit == null) {
				terminalStep(newstate, statehistory);
				return actions;
			}

			if (Math.abs(footmanX - townhallUnit.getXPosition()) > 1
					|| Math.abs(footmanY - townhallUnit.getYPosition()) > 1) {
				System.err.println("Invalid plan. Cannot attack townhall");
				totalExecutionTime += System.nanoTime() - startTime - planTime;
				return actions;
			} else {
				System.out.println("Attacking TownHall");
				// if no more movements in the planned path then attack
				actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
			}
		}

		totalExecutionTime += System.nanoTime() - startTime - planTime;
		return actions;
	}

	@Override
	public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
		System.out.println("Total turns: " + newstate.getTurnNumber());
		System.out.println("Total planning time: " + totalPlanTime / 1e9);
		System.out.println("Total execution time: " + totalExecutionTime / 1e9);
		System.out.println("Total time: " + (totalExecutionTime + totalPlanTime) / 1e9);
	}

	@Override
	public void savePlayerData(OutputStream os) {

	}

	@Override
	public void loadPlayerData(InputStream is) {

	}

	/**
	 * You will implement this method.
	 *
	 * This method should return true when the path needs to be replanned and false
	 * otherwise. This will be necessary on the dynamic map where the footman will
	 * move to block your unit.
	 *
	 * @param state
	 * @param history
	 * @param currentPath the stack containing the moves for the current path
	 * @return returns whether a new path should be planned
	 */
	private boolean shouldReplanPath(State.StateView state, History.HistoryView history,
			Stack<MapLocation> currentPath) {
		//if an enemy footman exists
		if (enemyFootmanID != -1) {
			Unit.UnitView enemyUnit = state.getUnit(enemyFootmanID);
			MapLocation enemyLocation = new MapLocation(enemyUnit.getXPosition(), enemyUnit.getYPosition(), null, 0);

			//the next position to be popped from the stack
			MapLocation next;
			if (!currentPath.isEmpty()) {
				//check up to 5 moves ahead if the enemy is in the path. If so, the path will be replanned
				for (int i = 0; i <= Math.min(5, currentPath.size() - 1); i++) {
					next = currentPath.elementAt(currentPath.size() - 1 - i);
					if (next.x == enemyLocation.x && next.y == enemyLocation.y) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * This method is implemented for you. You should look at it to see examples of
	 * how to find units and resources in Sepia.
	 *
	 * @param state
	 * @return
	 */
	private Stack<MapLocation> findPath(State.StateView state) {
		Unit.UnitView townhallUnit = state.getUnit(townhallID);
		Unit.UnitView footmanUnit = state.getUnit(footmanID);

		MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

		MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

		MapLocation footmanLoc = null;
		if (enemyFootmanID != -1) {
			Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
			footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
		}

		// get resource locations
		List<Integer> resourceIDs = state.getAllResourceIds();
		Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
		for (Integer resourceID : resourceIDs) {
			ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

			resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
		}

		return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
	}

	/**
	 * This is the method you will implement for the assignment. Your implementation
	 * will use the A* algorithm to compute the optimum path from the start position
	 * to a position adjacent to the goal position.
	 *
	 * You will return a Stack of positions with the top of the stack being the
	 * first space to move to and the bottom of the stack being the last space to
	 * move to. If there is no path to the townhall then return null from the method
	 * and the agent will print a message and do nothing. The code to execute the
	 * plan is provided for you in the middleStep method.
	 *
	 * As an example consider the following simple map
	 *
	 * F - - - - x x x - x H - - - -
	 *
	 * F is the footman H is the townhall x's are occupied spaces
	 *
	 * xExtent would be 5 for this map with valid X coordinates in the range of [0,
	 * 4] x=0 is the left most column and x=4 is the right most column
	 *
	 * yExtent would be 3 for this map with valid Y coordinates in the range of [0,
	 * 2] y=0 is the top most row and y=2 is the bottom most row
	 *
	 * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
	 *
	 * The path would be
	 *
	 * (1,0) (2,0) (3,1) (2,2) (1,2)
	 *
	 * Notice how the initial footman position and the townhall position are not
	 * included in the path stack
	 *
	 * @param start
	 *            Starting position of the footman
	 * @param goal
	 *            MapLocation of the townhall
	 * @param xExtent
	 *            Width of the map
	 * @param yExtent
	 *            Height of the map
	 * @param resourceLocations
	 *            Set of positions occupied by resources
	 * @return Stack of positions with top of stack being first move in plan
	 */
	private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent,
			MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations) {

		boolean pathExists = true;
		
		//the stack that initially gets filled
		Stack<MapLocation> reversePath = new Stack<MapLocation>();
		//the stack that gets the final path, which is the reversed representation of reversePath
		Stack<MapLocation> finalPath = new Stack<MapLocation>();

		//a representation of the map that determines which positions cannot be traveled to
		boolean[][] eliminated = new boolean[xExtent][yExtent];
		
		//marks each location with a tree as eliminated
		for (MapLocation resourceNode : resourceLocations) {
			eliminated[resourceNode.x][resourceNode.y] = true;
		}
		
		//if an enemy footman exists, its location is also marked as eliminated
		if(enemyFootmanLoc != null) {
			eliminated[enemyFootmanLoc.x][enemyFootmanLoc.y] = true;
		}

		//the list of possible moves from the current state
		List<MapLocation> moves;
		//the current state
		MapLocation current = start;

		//continues to find a path until the footman is 1 unit away from the townhall or it is determined that there is no possible path
		while ((Math.abs(current.x - goal.x) > 1 || Math.abs(current.y - goal.y) > 1) && pathExists) {
			moves = possibleMoves(current, eliminated, xExtent, yExtent);
			
			//if there are no possible moves that have not been visited
			if (moves.size() == 0) {
				//if there is no "came from" parent for the location, then every path has been tested and no path exists
				if (current.cameFrom == null) {
					pathExists = false;
				} 
				//if there is a parent, backtrack and continue to look for a path
				else {
					current = current.cameFrom;
					reversePath.pop();
				}

			} 
			//otherwise check which is the best possible move at a given state
			else {
				current = minimumChebyshev(goal, moves);
				reversePath.push(current);
				eliminated[current.x][current.y] = true;
			}

		}
		
		//since the reversePath stack has the final move at the top, the stack needs to be inverted into a new stack
		while (reversePath.size() > 0) {
			finalPath.push(reversePath.pop());
		}

		if (pathExists) {
			// if path exists, return path
			return finalPath;
		} else {
			// if no path exists
			System.err.println("No available path");
			System.exit(0);

			return null;
		}
	}

	/**
	 * calculates the Chebyshev distance for two points
	 * @param x1 the x value for the first point
	 * @param y1 the y value for the first point
	 * @param x2 the x value for the second point
	 * @param y2 the y value for the second point
	 * @return returns the value for the Chebyshev equation
	 */
	private int Chebyshev(int x1, int y1, int x2, int y2) {
		return Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	/**
	 * determines the list of possible moves based on the current location and eliminated locations
	 * @param current the footman's current location
	 * @param eliminated the map that determines which positions cannot be traveled to
	 * @param xExtent the length of the map in the x direction
	 * @param yExtent the length of the map in the y direction
	 * @return returns the list of possible moves based on the current location and eliminated locations
	 */
	private List<MapLocation> possibleMoves(MapLocation current, boolean[][] eliminated, int xExtent, int yExtent) {
		List<MapLocation> moves = new ArrayList<MapLocation>();

		int x_min = Math.max(current.x - 1, 0);
		int x_max = Math.min(current.x + 1, xExtent);
		int y_min = Math.max(current.y - 1, 0);
		int y_max = Math.min(current.y + 1, yExtent);

		//loops through the positions that are located 1 unit away in the horizontal, vertical, and diagonal directions
		for (int i = x_min; i <= x_max; i++) {
			for (int j = y_min; j <= y_max; j++) {
				//does not consider the current location as a possible move
				if (i == current.x && j == current.y) {
					continue;
				}

				if (!eliminated[i][j]) {
					moves.add(new MapLocation(i, j, current, 0));
				}
			}
		}
		return moves;
	}

	/**
	 * takes a list of possible moves and the goal state and calculates which is the best move option based on the Chebyshev equation
	 * @param goal the goal state
	 * @param adjacent the list of possible states for the next move
	 * @return returns the move with the best heuristic (lowest Chebyshev value)
	 */
	private MapLocation minimumChebyshev(MapLocation goal, List<MapLocation> adjacent) {
		MapLocation minimum = adjacent.get(0);

		for (MapLocation next : adjacent) {
			int minimumCheby = Chebyshev(goal.x, goal.y, minimum.x, minimum.y);
			int nextCheby = Chebyshev(goal.x, goal.y, next.x, next.y);
			if (minimumCheby > nextCheby)
				minimum = next;
			//in the case of tie-breakers, choose the state that will move the footman closer to the goal in the direction that didn't determine the Cheyshev value
			else if (minimumCheby == nextCheby) {
				if (next.x == minimum.x) {
					if (Math.abs(next.y - goal.y) < Math.abs(minimum.y - goal.y)) {
						minimum = next;
					}
				} else {
					if (Math.abs(next.x - goal.x) < Math.abs(minimum.x - goal.x)) {
						minimum = next;
					}
				}
			}
		}

		return minimum;
	}

	/**
	 * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc) This converts
	 * the difference between the current position and the desired position to a
	 * direction.
	 *
	 * @param xDiff
	 *            Integer equal to 1, 0 or -1
	 * @param yDiff
	 *            Integer equal to 1, 0 or -1
	 * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
	 */
	private Direction getNextDirection(int xDiff, int yDiff) {

		// figure out the direction the footman needs to move in
		if (xDiff == 1 && yDiff == 1) {
			return Direction.SOUTHEAST;
		} else if (xDiff == 1 && yDiff == 0) {
			return Direction.EAST;
		} else if (xDiff == 1 && yDiff == -1) {
			return Direction.NORTHEAST;
		} else if (xDiff == 0 && yDiff == 1) {
			return Direction.SOUTH;
		} else if (xDiff == 0 && yDiff == -1) {
			return Direction.NORTH;
		} else if (xDiff == -1 && yDiff == 1) {
			return Direction.SOUTHWEST;
		} else if (xDiff == -1 && yDiff == 0) {
			return Direction.WEST;
		} else if (xDiff == -1 && yDiff == -1) {
			return Direction.NORTHWEST;
		}

		System.err.println("Invalid path. Could not determine direction");
		return null;
	}
}
package edu.cwru.sepia.agent.minimax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.LocatedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.util.Direction;

/**
 * This class stores all of the information the agent needs to know about the
 * state of the game. For example this might include things like footmen HP and
 * positions.
 *
 * Add any information or methods you would like to this class, but do not
 * delete or change the signatures of the provided methods.
 */
public class GameState {
	//
	// instance fields
	//

	// state
	State.StateView state;

	// player numbers
	private int enemyPlayerNum = 1;

	// map dimensions
	private int xExtent;
	private int yExtent;

	// resource nodes
	private List<ResourceView> resourceNodes;

	// map layout
	private boolean[][] map;

	// units
	private List<UnitView> allUnits;
	private List<UnitView> footmanUnits = new ArrayList<UnitView>();
	private List<UnitView> enemyUnits = new ArrayList<UnitView>();

	private int utility;

	private int turnNumber;
	

	/**
	 * You will implement this constructor. It will extract all of the needed state
	 * information from the built in SEPIA state view.
	 *
	 * You may find the following state methods useful:
	 *
	 * state.getXExtent() and state.getYExtent(): get the map dimensions
	 * state.getAllResourceIDs(): returns all of the obstacles in the map
	 * state.getResourceNode(Integer resourceID): Return a ResourceView for the
	 * given ID
	 *
	 * For a given ResourceView you can query the position using
	 * resource.getXPosition() and resource.getYPosition()
	 *
	 * For a given unit you will need to find the attack damage, range and max HP
	 * unitView.getTemplateView().getRange(): This gives you the attack range
	 * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit
	 * deals unitView.getTemplateView().getBaseHealth(): The maximum amount of
	 * health of this unit
	 *
	 * @param state
	 *            Current state of the episode
	 */
	public GameState(State.StateView state) {

		// current state
		this.state = state;
		
		this.turnNumber = (state.getTurnNumber() % 2);
		
		// get the map dimensions
		xExtent = state.getXExtent();
		yExtent = state.getYExtent();

		// get all resource nodes
		resourceNodes = state.getAllResourceNodes();

		// build map
		map = new boolean[xExtent][yExtent];
		for (int x = 0; x < xExtent; x++) {
			for (int y = 0; y < yExtent; y++) {
				map[x][y] = false;
			}
		}

		// identify resource positions
		for (ResourceView resourceNode : resourceNodes) {
			map[resourceNode.getXPosition()][resourceNode.getYPosition()] = true;
		}

		// get all units and classify them
		allUnits = state.getAllUnits();
		for (UnitView unit : allUnits) {
			if (unit.getTemplateView().getPlayer() == 1) {
				enemyUnits.add(unit);
			} else {
				footmanUnits.add(unit);
			}
		}

		utility = getUtility();
	}

	/**
	 * You will implement this function.
	 *
	 * You should use weighted linear combination of features. The features may be
	 * primitives from the state (such as hp of a unit) or they may be higher level
	 * summaries of information from the state such as distance to a specific
	 * location. Come up with whatever features you think are useful and weight them
	 * appropriately.
	 *
	 * It is recommended that you start simple until you have your algorithm
	 * working. Then watch your agent play and try to add features that correct
	 * mistakes it makes. However, remember that your features should be as fast as
	 * possible to compute. If the features are slow then you will be able to do
	 * less plys in a turn.
	 *
	 * Add a good comment about what is in your utility and why you chose those
	 * features.
	 *
	 * @return The weighted linear combination of the features
	 */
	public int getUtility() {
		// minimize distance between footmen and closest enemy archer (D)
		// D = min(Abs(x distance) + Abs(y distance))
		// minimize archer's total hp (HA)
		// HA = sum of all Archers' health
		// maximize footmen's total hp (HF)
		// HF = sum of all Footmen's health

		// Beginning HA = 50, HF = 160

		// Utility = HF - D - HA
		int distance = 0;

		int footmenHealth = 0;
		int enemyHealth = 0;
		for (UnitView footman : footmanUnits) {
			int minDistance = this.xExtent + this.yExtent;
			for (UnitView enemy : enemyUnits) {
				int d = Math.abs(footman.getXPosition() - enemy.getXPosition())
						+ Math.abs(footman.getYPosition() - enemy.getYPosition());
				if (d < minDistance) {
					minDistance = d;
				}
			}
			distance += minDistance;

			footmenHealth += footman.getHP();
		}

		for (UnitView enemy : enemyUnits) {
			enemyHealth += enemy.getHP();
		}
		this.utility = distance + enemyHealth - footmenHealth;
		this.utility = (int)(distance + 30 * Math.log(enemyHealth) - 30 * Math.log(footmenHealth));
		return utility;
	}

	/**
	 * You will implement this function.
	 *
	 * This will return a list of GameStateChild objects. You will generate all of
	 * the possible actions in a step and then determine the resulting game state
	 * from that action. These are your GameStateChildren.
	 *
	 * You may find it useful to iterate over all the different directions in SEPIA.
	 *
	 * for(Direction direction : Directions.values())
	 *
	 * To get the resulting position from a move in that direction you can do the
	 * following x += direction.xComponent() y += direction.yComponent()
	 *
	 * @return All possible actions and their associated resulting game state
	 */
	public List<GameStateChild> getChildren() {
		List<GameStateChild> children = new ArrayList<GameStateChild>();
		List<List<Action>> agentActions = new ArrayList<List<Action>>();

		// footman turn
		if (this.getTurnNumber() == 0) {
			// footman helper method
			for (UnitView footman : footmanUnits) {
				agentActions.add(footmanChildren(footman));
			}
		}

		// archer turn
		else {
			// archer helper method
			for (UnitView archer : enemyUnits) {
				agentActions.add(archerChildren(archer));
			}
		}
		
		
		children = finalChildrenStates(agentActions);
		return children;
	}

	// generate list of moves for an individual archer
	private List<Action> archerChildren(UnitView archer) {
		List<Action> actions = new ArrayList<Action>();

		int x = archer.getXPosition();
		int y = archer.getYPosition();
		int id = archer.getID();
		int range = archer.getTemplateView().getRange();

		// check if an footman is within range
		for (UnitView footman : footmanUnits) {
			if (Math.abs(footman.getXPosition() - x) + Math.abs(footman.getYPosition() - y) <= range) {
				actions.add(Action.createCompoundAttack(id, footman.getID()));
			}
		}
		List<Direction> directions = new ArrayList<Direction>();
		directions.add(Direction.NORTH);
		directions.add(Direction.SOUTH);
		directions.add(Direction.EAST);
		directions.add(Direction.WEST);

		for (Direction direction : directions) {
			int x2 = x + direction.xComponent();
			int y2 = y + direction.yComponent();

			// check if move is within the bounds and is not over a resource
			if (legalMove(x2, y2) && !map[x2][y2]) {
				actions.add(Action.createCompoundMove(id, x2, y2));
			}
		}

		return actions;
	}

	// generate list of moves for individual footman
	private List<Action> footmanChildren(UnitView footman) {
		List<Action> actions = new ArrayList<Action>();

		int x = footman.getXPosition();
		int y = footman.getYPosition();
		int id = footman.getID();

		// check if an enemy is next to the footman
		for (UnitView enemy : enemyUnits) {
			if (Math.abs(enemy.getXPosition() - x) == 1 ^ Math.abs(enemy.getYPosition() - y) == 1) {
				actions.add(new TargetedAction(id, (Action.createCompoundAttack(id, enemy.getID()).getType()),
						enemy.getID()));
			}
		}
		List<Direction> directions = new ArrayList<Direction>();
		directions.add(Direction.NORTH);
		directions.add(Direction.SOUTH);
		directions.add(Direction.EAST);
		directions.add(Direction.WEST);
		for (Direction direction : directions) {
			int x2 = x + direction.xComponent();
			int y2 = y + direction.yComponent();

			// check if move is within the bounds and is not over a resource
			if (legalMove(x2, y2) && !map[x2][y2]) {
				actions.add(Action.createCompoundMove(id, x2, y2));
			}
		}

		return actions;
	}

	// creates a new game state that is determined by the actions of the child
	private GameState createNewGameState(Map<Integer, Action> actions) {
		State stateHolder = null;
		State.StateView newState = null;
		try {
			stateHolder = this.state.getStateCreator().createState();
<<<<<<< HEAD
			//stateHolder.incrementTurn();
=======
>>>>>>> a65ccf1640e6e5d29ef8c7288e96ec9a846f1c2f
			newState = stateHolder.getView(this.getTurnNumber());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//changes the turn for the children states
		GameState newGameState = new GameState(newState);
		newGameState.setTurnNumber(this.switchTurnNumber());
		

		for (Action action : actions.values()) {
			//changes the state if an agent is attacking
			if (action.getType() == ActionType.COMPOUNDATTACK) {
				boolean attackingEnemy = false;
				int targetID = ((TargetedAction) action).getTargetId();
				int attackerID = ((TargetedAction) action).getUnitId();
				UnitView target = null;
				UnitView attacker = null;
				for (UnitView enemy : newGameState.getEnemyUnits()) {
					if (enemy.getID() == targetID) {
						attackingEnemy = true;
						target = enemy;
					}
					if (enemy.getID() == attackerID) {
						attackingEnemy = false;
						attacker = enemy;
					}
				}
				for (UnitView footman : newGameState.getFootmanUnits()) {
					if (footman.getID() == targetID) {
						attackingEnemy = false;
						target = footman;
					}
					if (footman.getID() == attackerID) {
						attackingEnemy = true;
						attacker = footman;
					}
				}

				int attackAmount = attacker.getTemplateView().getBasicAttack();

				if (attackingEnemy) {
					UnitView enemyUnit = newGameState.state.getUnit(target.getID());
					UnitTemplate newEnemyTemplate = new UnitTemplate(enemyUnit.getID());

					// set name, basic attack, attack range
					newEnemyTemplate.setPlayer(enemyUnit.getTemplateView().getPlayer());
					newEnemyTemplate.setName(enemyUnit.getTemplateView().getName());
					newEnemyTemplate.setBasicAttack(enemyUnit.getTemplateView().getBasicAttack());
					newEnemyTemplate.setRange(enemyUnit.getTemplateView().getRange());

					Unit newEnemy = new Unit(newEnemyTemplate, enemyUnit.getID());
					newEnemy.setHP(enemyUnit.getHP() - attackAmount);
					newEnemy.setxPosition(enemyUnit.getXPosition());
					newEnemy.setyPosition(enemyUnit.getYPosition());
					UnitView newEnemyUnitView = new UnitView(newEnemy);
					newGameState.getEnemyUnits().remove(enemyUnit);
					newGameState.getEnemyUnits().add(newEnemyUnitView);
				} else {
					// figure out how to change HP
					// remove from enemyUnits and add the new enemy to the list
					UnitView footmanUnit = newGameState.state.getUnit(target.getID());
					UnitTemplate newFootmanTemplate = new UnitTemplate(footmanUnit.getID());

					// set name, basic attack, attack range
					newFootmanTemplate.setPlayer(footmanUnit.getTemplateView().getPlayer());
					newFootmanTemplate.setName(footmanUnit.getTemplateView().getName());
					newFootmanTemplate.setBasicAttack(footmanUnit.getTemplateView().getBasicAttack());
					newFootmanTemplate.setRange(footmanUnit.getTemplateView().getRange());

					Unit newFootman = new Unit(newFootmanTemplate, footmanUnit.getID());
					newFootman.setHP(footmanUnit.getHP() - attackAmount);
					newFootman.setxPosition(footmanUnit.getXPosition());
					newFootman.setyPosition(footmanUnit.getYPosition());
					UnitView newFootmanUnitView = new UnitView(newFootman);
					newGameState.getFootmanUnits().remove(footmanUnit);
					newGameState.getFootmanUnits().add(newFootmanUnitView);
				}	
			} 
			//changes the state if an agent is moving
			else if (action.getType() == ActionType.COMPOUNDMOVE) {
				// footman's turn
				int currentFootmanID = action.getUnitId();
				if (newGameState.getTurnNumber() == 0) {
					UnitView footmanUnit = newGameState.state.getUnit(currentFootmanID);
					UnitTemplate newFootmanTemplate = new UnitTemplate(footmanUnit.getID());
					
					int newX = ((LocatedAction) action).getX();
					int newY = ((LocatedAction) action).getY();
					
					// set name, basic attack, attack range
					newFootmanTemplate.setPlayer(footmanUnit.getTemplateView().getPlayer());
					newFootmanTemplate.setName(footmanUnit.getTemplateView().getName());
					newFootmanTemplate.setBasicAttack(footmanUnit.getTemplateView().getBasicAttack());
					newFootmanTemplate.setRange(footmanUnit.getTemplateView().getRange());

					Unit newFootman = new Unit(newFootmanTemplate, footmanUnit.getID());
					newFootman.setHP(footmanUnit.getHP());
					newFootman.setxPosition(newX);
					newFootman.setyPosition(newY);
					UnitView newFootmanUnitView = new UnitView(newFootman);
					newGameState.getFootmanUnits().remove(footmanUnit);
					newGameState.getFootmanUnits().add(newFootmanUnitView);
				}
				// enemy's turn
				else if (newGameState.getTurnNumber() == 1) {
					UnitView enemyUnit = newGameState.state.getUnit(currentFootmanID);
					UnitTemplate newEnemyTemplate = new UnitTemplate(enemyUnit.getID());

					// set name, basic attack, attack range
					newEnemyTemplate.setPlayer(enemyUnit.getTemplateView().getPlayer());
					newEnemyTemplate.setName(enemyUnit.getTemplateView().getName());
					newEnemyTemplate.setBasicAttack(enemyUnit.getTemplateView().getBasicAttack());
					newEnemyTemplate.setRange(enemyUnit.getTemplateView().getRange());
					
					int newX = ((LocatedAction) action).getX();
					int newY = ((LocatedAction) action).getY();
					
					Unit newEnemy = new Unit(newEnemyTemplate, enemyUnit.getID());
					newEnemy.setHP(enemyUnit.getHP());
					newEnemy.setxPosition(newX);
					newEnemy.setyPosition(newY);
					UnitView newEnemyUnitView = new UnitView(newEnemy);
					newGameState.getEnemyUnits().remove(enemyUnit);
					newGameState.getEnemyUnits().add(newEnemyUnitView);
				}
			}
		}

		return newGameState;
	}

	public int getxExtent() {
		return xExtent;
	}

	public int getyExtent() {
		return yExtent;
	}

	public List<ResourceView> getResourceNodes() {
		return resourceNodes;
	}

	public List<UnitView> getFootmanUnits() {
		return footmanUnits;
	}

	public List<UnitView> getEnemyUnits() {
		return enemyUnits;
	}

	public int getTurnNumber() {
		return this.turnNumber;
	}
	
	public void setTurnNumber(int turnNumber) {
		this.turnNumber = turnNumber;
	}
	
<<<<<<< HEAD
=======
	//returns the opposite of the turn
>>>>>>> a65ccf1640e6e5d29ef8c7288e96ec9a846f1c2f
	public int switchTurnNumber() {
		if(this.turnNumber == 0) {
			return 1;
		}
		else {
			return 0;
		}
	}

	// generate final children
	private List<GameStateChild> finalChildrenStates(List<List<Action>> agentActions) {
		List<GameStateChild> children = new ArrayList<GameStateChild>();

		if (agentActions.size() == 1) {
			List<Action> currentAgentAction = agentActions.get(0);
			for (Action action : currentAgentAction) {
				Map<Integer, Action> actionMap = new HashMap<>();
				actionMap.put(action.getUnitId(), action);
				GameStateChild datNewChild = new GameStateChild(actionMap, createNewGameState(actionMap));
			//	datNewChild.state.switchTurnNumber();
				children.add(datNewChild);
			}
		} else {
			List<Action> firstAgentAction = agentActions.get(0);
			List<Action> secondAgentAction = agentActions.get(1);
			for (Action firstAction : firstAgentAction) {
				for (Action secondAction : secondAgentAction) {
					Map<Integer, Action> actionMap = new HashMap<>();
					actionMap.put(firstAction.getUnitId(), firstAction);
					actionMap.put(secondAction.getUnitId(), secondAction);
					GameStateChild datNewChild = new GameStateChild(actionMap, createNewGameState(actionMap));
				//	datNewChild.state.switchTurnNumber();
					children.add(datNewChild);
				}
			}
		}
		
		return children;
	}

	/* helper methods to check if a move is legal */
	private boolean legalMove(int x, int y) {
		// check if spot on board is empty --> for future
		if (x >= xExtent || x < 0 || y >= yExtent || y < 0) {
			return false;
		}
		return true;
	}

}

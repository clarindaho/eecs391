package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.environment.model.state.UnitTemplate;

public class DepositGoldAction implements StripsAction {

	//
	// instance fields
	//

	private Position townHallPosition = null;

	private UnitView peasant = null;
	private Position newPosition = null;

	private String stripsAction;
	private Action sepiaAction;

	GameState parent;

	//
	// constructor methods
	//
	public DepositGoldAction(UnitView peasant, Position newPosition, Position townHallPosition, GameState parent) {
		this.peasant = peasant;
		this.newPosition = newPosition;
		this.townHallPosition = townHallPosition;
		this.parent = parent;
	}

	public GameState getParent() {
		return this.parent;
	}

	//
	// accessor methods
	//

	/**
	 * Check if peasant's new position is at the townhall.
	 * 
	 * @return true if peasant is at townhall, otherwise false
	 */
	@Override
	public boolean preconditionsMet(GameState state) {
		if (hasGold() && newPosition.equals(townHallPosition))
			return true;
		else
			return false;
	}

	/**
	 * Check if peasant is currently holding gold.
	 * 
	 * @return true if peasant is holding gold, otherwise false
	 */
	public boolean hasGold() {
		if (peasant.getCargoAmount() > 0 && peasant.getCargoType() == ResourceType.GOLD) {
			return true;
		}
		return false;
	}

	//
	// mutator methods
	//

	@Override
	public GameState apply(GameState state) {
		// create the new gameState
		GameState newGameState = new GameState(state);

		// find the unit that is harvesting, update the value, and put the new unit into the list
		UnitView changedUnit = newGameState.findUnit(peasant.getID(), newGameState.getPlayerUnits());
		Unit newUnit = new Unit(new UnitTemplate(changedUnit.getID()), changedUnit.getID());
		newUnit.setxPosition(peasant.getXPosition());
		newUnit.setyPosition(peasant.getYPosition());
		newUnit.clearCargo();
		newGameState.getPlayerUnits().remove(changedUnit);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		// update the gold amount
		newGameState.addGold(peasant.getCargoAmount());

		// create the STRIPS action
		// DepositWood(peasantID, townHallDirection)
		stripsAction = "DepositGold(" + changedUnit.getID() + "," + determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), townHallPosition.x, townHallPosition.y) + ")";

		// create the SEPIA action
		// Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
		sepiaAction = Action.createPrimitiveDeposit(changedUnit.getID(), determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), townHallPosition.x, townHallPosition.y));

		// update the cost
		// double cost = newUnit.getTemplate().getDurationDeposit();
		double cost = 1;
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update the plan
		newGameState.addPlan(this);

		return newGameState;
	}

	// determine which direction the peasant must move to deposit something to the
	// townhall
	private Direction determineDirection(int peasantX, int peasantY, int townhallX, int townhallY) {
		int x = townhallX - peasantX;
		int y = townhallY - peasantY;

		for (Direction d : Direction.values()) {
			if (x == d.xComponent() && y == d.yComponent()) {
				return d;
			}
		}
		return null;
	}

	// creates the SEPIA Action from the strips action
	@Override
	public Action createSEPIAaction() {
		return sepiaAction;
	}

	@Override
	public String toString() {
		return stripsAction;
	}
}
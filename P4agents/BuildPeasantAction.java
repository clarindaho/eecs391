package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.util.Direction;

public class BuildPeasantAction implements StripsAction {

	private int currentGold;
	private int peasantGoldCost;

	private UnitView peasant;
	private int newX;
	private int newY;
	private String stripsAction;
	private Action sepiaAction;
	private GameState parent;

	public BuildPeasantAction(GameState parent) {
		this.parent = parent;
	}

	//checks to see if the action can be executed on the state
	@Override
	public boolean preconditionsMet(GameState state) {
		currentGold = state.getCurrentGold();
		peasantGoldCost = state.getState().getTemplate(state.getPlayerNum(), "Peasant").getGoldCost();

		// check if the resources are enough to build a peasant
		if (state.isBuildPeasants() && currentGold > peasantGoldCost) {
			// check if there are empty positions around townhall to place peasant
			int townHallXPos = state.getTownHall().getXPosition();
			int townHallYPos = state.getTownHall().getYPosition();

			for (Direction d : Direction.values()) {
				newX = townHallXPos + d.xComponent();
				newY = townHallYPos + d.yComponent();
				if (legalPosition(state, newX, newY)) {
					return true;
				}
			}
		}

		return false;
	}

	//applies the action and returns a new gameState
	@Override
	public GameState apply(GameState state) {

		GameState newGameState = new GameState(state);

		// create new peasant
		TemplateView peasantTemplate = newGameState.getState().getTemplate(newGameState.getPlayerNum(), "Peasant");
		int peasantTemplateID = peasantTemplate.getID();

		Unit newUnit = new Unit(new UnitTemplate(peasantTemplateID), peasantTemplateID);
		newUnit.setxPosition(newX);
		newUnit.setyPosition(newY);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		// decrement current gold after building peasant
		newGameState.addGold(-(peasantGoldCost));

		// set STRIPS command
		// Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
		stripsAction = "BuildPeasant(" + state.getTownHall().getID() + "," + peasantTemplateID + ")";
		sepiaAction = Action.createPrimitiveProduction(state.getTownHall().getID(), peasantTemplateID);
		
		// update the cost
		double cost = newUnit.getTemplate().getTimeCost();
		newGameState.addCost(cost);

		// update the plan
		newGameState.addPlan(this);

		return newGameState;
	}

	/* helper methods to check if the position is legal to place peasant */
	private boolean legalPosition(GameState state, int x, int y) {

		if (x >= state.getxExtent() || x < 0 || y >= state.getyExtent() || y < 0) {
			return false;
		} else if (state.getMap()[x][y]) {
			return false;
		} else {
			for (UnitView unit : state.getPlayerUnits()) {
				if (x == unit.getXPosition() && y == unit.getYPosition()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return stripsAction;
	}

	@Override
	public Action createSEPIAaction() {
		return sepiaAction;
	}

	@Override
	public GameState getParent() {
		return this.parent;
	}
}
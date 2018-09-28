import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class MyCombatAgent extends Agent {

	//
	// instance fields
	//

	// enemy player number
	private int enemyPlayerNum = 1;

	// stores the action that each unit will perform
	private Map<Integer, Action> actions;

	// list of all of your units
	private List<Integer> myUnitIDs;

	// stores the unit IDs
	private List<Integer> footmenIds;
	private List<Integer> archerIds;
	private List<Integer> ballistaIds;
	private List<Integer> towerIds;

	// list of enemy units
	private List<Integer> enemyUnitIDs;

	// stores the enemy unit IDs
	private List<Integer> enemyFootmenIds;
	private List<Integer> enemyArcherIds;
	private List<Integer> enemyBallistaIds;
	private List<Integer> enemyTowerIds;

	//
	// constructor method
	//

	public MyCombatAgent(int playernum, String[] otherargs) {
		super(playernum);

		if (otherargs.length > 0) {
			enemyPlayerNum = new Integer(otherargs[0]);
		}

		System.out.println("Constructed MyCombatAgent");
	}

	//
	// inherited methods
	//

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
		// if there are no changes to the current actions then this map will be empty
		actions = new HashMap<Integer, Action>();

		myUnitIDs = newstate.getUnitIds(playernum);
		enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

		if (enemyUnitIDs.size() == 0) {
			// Nothing to do because there is no one left to attack
			return actions;
		}

		// get details about my units
		footmenIds = new ArrayList<Integer>();
		archerIds = new ArrayList<Integer>();
		ballistaIds = new ArrayList<Integer>();
		towerIds = new ArrayList<Integer>();

		UnitView unit;
		String unitTypeName;
		for (Integer unitID : myUnitIDs) {
			unit = newstate.getUnit(unitID);
			unitTypeName = unit.getTemplateView().getName();

			if (unitTypeName.equals("ScoutTower"))
				towerIds.add(unitID);
			else if (unitTypeName.equals("Footman"))
				footmenIds.add(unitID);
			else if (unitTypeName.equals("Archer"))
				archerIds.add(unitID);
			else if (unitTypeName.equals("Ballista"))
				ballistaIds.add(unitID);
			else
				System.err.println("Unexpected Unit type: " + unitTypeName);
		}

		// start by commanding units to move
		for (Integer myUnitID : ballistaIds) {
			actions.put(myUnitID, Action.createCompoundMove(myUnitID, 0, 16));
		}
		for (Integer myUnitID : archerIds) {
			actions.put(myUnitID, Action.createCompoundMove(myUnitID, 0, 15));
		}
		for (Integer myUnitID : footmenIds) {
			actions.put(myUnitID, Action.createCompoundMove(myUnitID, 0, 15));
		}

		return actions;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
		// This stores the action that each unit will perform if there are no changes to
		// the current actions then this map will be empty
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

		myUnitIDs = newstate.getUnitIds(playernum);
		enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

		// get details about my units
		footmenIds = new ArrayList<Integer>();
		archerIds = new ArrayList<Integer>();
		ballistaIds = new ArrayList<Integer>();
		towerIds = new ArrayList<Integer>();

		UnitView unit;
		String unitTypeName;
		for (Integer unitID : myUnitIDs) {
			unit = newstate.getUnit(unitID);
			unitTypeName = unit.getTemplateView().getName();

			if (unitTypeName.equals("ScoutTower"))
				towerIds.add(unitID);
			else if (unitTypeName.equals("Footman"))
				footmenIds.add(unitID);
			else if (unitTypeName.equals("Archer"))
				archerIds.add(unitID);
			else if (unitTypeName.equals("Ballista"))
				ballistaIds.add(unitID);
			else
				System.err.println("Unexpected Unit type: " + unitTypeName);
		}

		// get details about enemy units
		enemyFootmenIds = new ArrayList<Integer>();
		enemyArcherIds = new ArrayList<Integer>();
		enemyBallistaIds = new ArrayList<Integer>();
		enemyTowerIds = new ArrayList<Integer>();

		UnitView enemyUnit;
		String enemyUnitTypeName;
		for (Integer enemyUnitID : enemyUnitIDs) {
			enemyUnit = newstate.getUnit(enemyUnitID);
			enemyUnitTypeName = enemyUnit.getTemplateView().getName();

			if (enemyUnitTypeName.equals("ScoutTower"))
				enemyTowerIds.add(enemyUnitID);
			else if (enemyUnitTypeName.equals("Footman"))
				enemyFootmenIds.add(enemyUnitID);
			else if (enemyUnitTypeName.equals("Archer"))
				enemyArcherIds.add(enemyUnitID);
			else if (enemyUnitTypeName.equals("Ballista"))
				enemyBallistaIds.add(enemyUnitID);
			else
				System.err.println("Unexpected Enemy Unit type: " + enemyUnitTypeName);
		}

		if (enemyUnitIDs.size() == 0) {
			// Nothing to do because there is no one left to attack
			return actions;
		}

		int currentStep = newstate.getTurnNumber();

		// go through the action history
		for (ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep - 1).values()) {
			// if the previous action is no longer in progress (either due to failure or
			// completion) then add a new action for this unit

			if (feedback.getFeedback() != ActionFeedback.INCOMPLETE) {
				int unitID = feedback.getAction().getUnitId();

				if (newstate.getUnit(unitID).getTemplateView().getName().equals("Footman")) {
					if (!enemyFootmenIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyFootmenIds.get(0)));
					} else if (!enemyArcherIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyArcherIds.get(0)));
					} else if (!enemyBallistaIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyBallistaIds.get(0)));
					} else if (!enemyTowerIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyTowerIds.get(0)));
					}
				} else if (newstate.getUnit(unitID).getTemplateView().getName().equals("Archer")) {
					if (!enemyFootmenIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyFootmenIds.get(0)));
					} else if (!enemyArcherIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyArcherIds.get(0)));
					} else if (!enemyBallistaIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyBallistaIds.get(0)));
					} else if (!enemyTowerIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyTowerIds.get(0)));
					}
				} else if (newstate.getUnit(unitID).getTemplateView().getName().equals("Ballista")) {
					if (!enemyFootmenIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyFootmenIds.get(0)));
					} else if (!enemyArcherIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyArcherIds.get(0)));
					} else if (!enemyBallistaIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyBallistaIds.get(0)));
					} else if (!enemyTowerIds.isEmpty()) {
						actions.put(unitID, Action.createCompoundAttack(unitID, enemyTowerIds.get(0)));
					}
				}
			}
		}

		return actions;
	}

	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		System.out.println("Finished the episode");
	}

	@Override
	public void savePlayerData(OutputStream os) {
	}

	@Override
	public void loadPlayerData(InputStream is) {
	}

}
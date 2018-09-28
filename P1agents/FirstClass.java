import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class FirstClass extends Agent {

	//
	// references
	//

	// peasant
	private final int peasantCap = 8;
	private int peasantFoodCost;
	private int peasantGoldCost;
	private int peasantWoodCost;

	// farm
	private final int farmCap = 5;
	private int farmFoodCost;
	private int farmGoldCost;
	private int farmWoodCost;

	// barracks
	private final int barracksCap = 1;
	private int barracksFoodCost;
	private int barracksGoldCost;
	private int barracksWoodCost;

	// wall
	private final int wallCap = 2;
	private int wallFoodCost;
	private int wallGoldCost;
	private int wallWoodCost;

	//
	// instance fields
	//

	// stores the list of all of your units
	private List<Integer> myUnitIds;

	// stores the unit IDs
	private List<Integer> peasantIds;
	private List<Integer> townhallIds;
	private List<Integer> farmIds;
	private List<Integer> barracksIds;
	private List<Integer> wallIds;

	// stores the resources IDs
	private List<Integer> goldMines;
	private List<Integer> trees;

	// stores the action that each unit will perform
	private Map<Integer, Action> actions;

	// stores the current amount of resources
	private int currentGold;
	private int currentWood;

	//
	// constructor method
	//

	public FirstClass(int playernum) {
		super(playernum);
	}

	//
	// inherited methods
	//

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
		// update costs of peasants
		TemplateView defaultPeasantTemplate = newstate.getTemplate(playernum, "Peasant");
		peasantFoodCost = defaultPeasantTemplate.getFoodCost();
		peasantGoldCost = defaultPeasantTemplate.getGoldCost();
		peasantWoodCost = defaultPeasantTemplate.getWoodCost();

		// update costs of farms
		TemplateView defaultFarmTemplate = newstate.getTemplate(playernum, "Farm");
		farmFoodCost = defaultFarmTemplate.getFoodCost();
		farmGoldCost = defaultFarmTemplate.getGoldCost();
		farmWoodCost = defaultFarmTemplate.getWoodCost();

		// update costs of barracks
		TemplateView defaultBarracksTemplate = newstate.getTemplate(playernum, "Barracks");
		barracksFoodCost = defaultBarracksTemplate.getFoodCost();
		barracksGoldCost = defaultBarracksTemplate.getGoldCost();
		barracksWoodCost = defaultBarracksTemplate.getWoodCost();

		// update costs of walls
		TemplateView defaultWallTemplate = newstate.getTemplate(playernum, "Wall");
		wallFoodCost = defaultWallTemplate.getFoodCost();
		wallGoldCost = defaultWallTemplate.getGoldCost();
		wallWoodCost = defaultWallTemplate.getWoodCost();

		return middleStep(newstate, statehistory);
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
		// if there are no changes to the current actions then this map will be empty
		actions = new HashMap<Integer, Action>();

		// get a list of all of your units
		myUnitIds = newstate.getUnitIds(playernum);

		// determine unit's type
		peasantIds = new ArrayList<Integer>();
		townhallIds = new ArrayList<Integer>();
		farmIds = new ArrayList<Integer>();
		barracksIds = new ArrayList<Integer>();
		wallIds = new ArrayList<Integer>();

		UnitView unit;
		String unitTypeName;
		for (Integer unitID : myUnitIds) {
			// UnitViews extract information about a specified unit id from the current
			// state. Using a unit view you can determine the type of the unit with the
			// given ID as well as other information such as health and resources carried.
			unit = newstate.getUnit(unitID);

			// To find properties that all units of a given type share access the
			// UnitTemplateView using the `getTemplateView()` method of a UnitView instance.
			unitTypeName = unit.getTemplateView().getName();

			if (unitTypeName.equals("TownHall"))
				townhallIds.add(unitID);
			else if (unitTypeName.equals("Peasant"))
				peasantIds.add(unitID);
			else if (unitTypeName.equals("Farm"))
				farmIds.add(unitID);
			else if (unitTypeName.equals("Barracks"))
				barracksIds.add(unitID);
			else if (unitTypeName.equals("Wall"))
				wallIds.add(unitID);
			else
				System.err.println("Unexpected Unit type: " + unitTypeName);
		}

		// get the amount of wood and gold you have in your Town Hall
		currentGold = newstate.getResourceAmount(playernum, ResourceType.GOLD);
		currentWood = newstate.getResourceAmount(playernum, ResourceType.WOOD);

		goldMines = newstate.getResourceNodeIds(Type.GOLD_MINE);
		trees = newstate.getResourceNodeIds(Type.TREE);

		// assign our peasants to collect resources
		boolean farmCreation = false;
		boolean barracksCreation = false;
		boolean wallCreation = false;
		for (Integer peasantID : peasantIds) {
			Action action = null;

			if (newstate.getUnit(peasantID).getCargoAmount() > 0) {
				// If the agent is carrying cargo then command it to deposit what its carrying
				// at the townhall.
				// Here we are constructing a new TargetedAction. The first parameter is the
				// unit being commanded.
				// The second parameter is the action type, in this case a COMPOUNDDEPOSIT. The
				// actions starting with COMPOUND are convenience actions made up of multiple
				// move actions and another final action in this case DEPOSIT. The moves are
				// determined using A* planning to the location of the unit specified by the 3rd
				// argument of the constructor.
				action = new TargetedAction(peasantID, ActionType.COMPOUNDDEPOSIT, townhallIds.get(0));
			} else {
				// build five farms
				// if there are enough resources, a farm is built
				if (!farmCreation && (farmIds.size() < farmCap) && (currentGold >= farmGoldCost)
						&& (currentWood >= farmWoodCost)) {
					// get the farm templates unique ID
					TemplateView farmTemplate = newstate.getTemplate(playernum, "Farm");
					int farmTemplateID = farmTemplate.getID();

					// create a compoundBuild for the specific peasant which tells the peasant to
					// build the farm at the specified location
					actions.put(peasantID, Action.createCompoundBuild(peasantID, farmTemplateID, 5, 5));

					farmCreation = true;
				}

				// build a barracks
				else if (!barracksCreation && (barracksIds.size() < barracksCap) && (currentGold >= barracksGoldCost)
						&& (currentWood >= barracksWoodCost)) {
					// get the barracks templates unique ID
					TemplateView barracksTemplate = newstate.getTemplate(playernum, "Barracks");
					int barracksTemplateID = barracksTemplate.getID();

					// create a compoundBuild for the specific peasant which tells the peasant to
					// build the barracks at the specified location
					actions.put(peasantIds.get(0), Action.createCompoundBuild(peasantID, barracksTemplateID, 8, 10));

					barracksCreation = true;
				}

				// build up to 8 more walls only if 2 farms have already been created
				// if there are enough resources, a wall is built
				else if (!wallCreation && (farmIds.size() > 2) && (wallIds.size() < wallCap)
						&& (currentGold >= wallGoldCost) && (currentWood >= wallWoodCost)) {
					// get the wall templates unique ID
					TemplateView wallTemplate = newstate.getTemplate(playernum, "Wall");
					int wallTemplateID = wallTemplate.getID();

					// create a compoundBuild for the specific peasant which tells the peasant to
					// build the wall at the specified location
					actions.put(peasantIds.get(0), Action.createCompoundBuild(peasantID, wallTemplateID, 10, 10));

					wallCreation = true;
				}

				// collect either gold or wood, whichever you have less of
				else {
					if (currentGold <= currentWood) {
						action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, goldMines.get(0));
					} else {
						action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, trees.get(0));
					}
				}
			}

			// Put the actions in the action map.
			// Without this step your agent will do nothing.
			if (action != null)
				actions.put(peasantID, action);
		}

		// build peasants
		if (peasantIds.size() < peasantCap) {
			// if there are enough resources, build a new peasant
			if ((currentGold >= peasantGoldCost) && (currentWood >= peasantWoodCost)) {
				// get the peasant template's unique ID
				// this is how SEPIA identifies what type of unit to build
				TemplateView peasantTemplate = newstate.getTemplate(playernum, "Peasant");
				int peasantTemplateID = peasantTemplate.getID();

				// grab the first townhall, assuming there is at least one townhall in the map
				int townhallID = townhallIds.get(0);

				// instructs the specified townhall to build a peasant with given template ID
				actions.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
			}
		}

		return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
	}

	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		System.out.println("Finished the episode");

	}

}
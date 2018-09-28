package edu.cwru.sepia.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class RLAgent extends Agent {

	/**
	 * Set in the constructor. Defines how many learning episodes your agent should
	 * run for. When starting an episode, if the count is greater than this value
	 * print a message and call sys.exit(0)
	 */
	public final int numEpisodes;

	/**
	 * List of your footmen and your enemies footmen
	 */
	private List<Integer> myFootmen;
	private List<Integer> enemyFootmen;

	/**
	 * Convenience variable specifying enemy agent number. Use this whenever
	 * referring to the enemy agent. We will make sure it is set to the proper
	 * number when testing your code.
	 */
	public static final int ENEMY_PLAYERNUM = 1;

	public static final int PLAYERNUM = 0;

	/**
	 * Set this to whatever size your feature vector is.
	 */
	public static final int NUM_FEATURES = 6;

	/**
	 * number of testing episodes
	 */
	public static final int NUM_EVALUATING = 5;

	/**
	 * number of learning episodes
	 */
	public static final int NUM_LEARNING = 10;

	/**
	 * number of episodes we've done
	 */
	private int episodesCompleted;

	/** rewards for each footman for the current episode **/
	private Map<Integer, Double> episodeRewards;

	/**
	 * maps a friendly footman ID to the features that it sed on the previous turn
	 **/
	private Map<Integer, double[]> episodeFeatures;

	/** indicates if the episode is a learning episode or evaluation episode **/
	private boolean isLearning;

	/** stores the cumulative Rewards over evaluation episodes **/
	private double cumulativeRewards;

	/** stores the average rewards over 5-evaluation episodes **/
	private List<Double> averageRewards;

	/**
	 * Use this random number generator for your epsilon exploration. When you
	 * submit we will change this seed so make sure that your agent works for more
	 * than the default seed.
	 */
	public final Random random = new Random(12345);

	/**
	 * Your Q-function weights.
	 */
	public Double[] weights;

	/**
	 * These variables are set for you according to the assignment definition. You
	 * can change them, but it is not recommended. If you do change them please let
	 * us know and explain your reasoning for changing them.
	 */
	public final double gamma = 0.9;
	public final double learningRate = .0001;
	public final double epsilon = .02;

	public RLAgent(int playernum, String[] args) {
		super(playernum);
		if (args.length >= 1) {
			numEpisodes = Integer.parseInt(args[0]);
			System.out.println("Running " + numEpisodes + " episodes.");
		} else {
			numEpisodes = 10;
			System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
		}

		boolean loadWeights = false;
		if (args.length >= 2) {
			loadWeights = Boolean.parseBoolean(args[1]);
		} else {
			System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
		}

		if (loadWeights) {
			weights = loadWeights();
		} else {
			// initialize weights to random values between -1 and 1
			weights = new Double[NUM_FEATURES];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = random.nextDouble() * 2 - 1;
			}
		}

		averageRewards = new ArrayList<>();
	}

	/**
	 * We've implemented some setup code for your convenience. Change what you need
	 * to.
	 */
	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

		// You will need to add code to check if you are in a testing or learning
		// episode
		if (episodesCompleted % 15 < 5) {
			isLearning = false;
		} else {
			isLearning = true;
		}

		// Find all of your units
		myFootmen = new LinkedList<>();
		for (Integer unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);

			String unitName = unit.getTemplateView().getName().toLowerCase();
			if (unitName.equals("footman")) {
				myFootmen.add(unitId);
			} else {
				System.err.println("Unknown unit type: " + unitName);
			}
		}

		// Find all of the enemy units
		enemyFootmen = new LinkedList<>();
		for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
			Unit.UnitView unit = stateView.getUnit(unitId);

			String unitName = unit.getTemplateView().getName().toLowerCase();
			if (unitName.equals("footman")) {
				enemyFootmen.add(unitId);
			} else {
				System.err.println("Unknown unit type: " + unitName);
			}
		}

		// set initial episode rewards for each footman to 0
		// set intial features for each footman
		episodeRewards = new HashMap<Integer, Double>();
		episodeFeatures = new HashMap<Integer, double[]>();

		double[] featureArray = new double[NUM_FEATURES];
		for (int i = 0; i < NUM_FEATURES; i++)
			featureArray[i] = 0;
		for (Integer footman : myFootmen) {
			episodeRewards.put(footman, 0.0);
			episodeFeatures.put(footman, featureArray);
		}

		return middleStep(stateView, historyView);
	}

	/**
	 * You will need to calculate the reward at each step and update your totals.
	 * You will also need to check if an event has occurred. If it has then you will
	 * need to update your weights and select a new action.
	 *
	 * If you are using the footmen vectors you will also need to remove killed
	 * units. To do so use the historyView to get a DeathLog. Each DeathLog tells
	 * you which player's unit died and the unit ID of the dead unit. To get the
	 * deaths from the last turn do something similar to the following snippet.
	 * Please be aware that on the first turn you should not call this as you will
	 * get nothing back.
	 *
	 * for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber()
	 * -1)) { System.out.println("Player: " + deathLog.getController() + " unit: " +
	 * deathLog.getDeadUnitID()); }
	 *
	 * You should also check for completed actions using the history view. Obviously
	 * you never want a footman just sitting around doing nothing (the enemy
	 * certainly isn't going to stop attacking). So at the minimum you will have an
	 * even whenever one your footmen's targets is killed or an action fails.
	 * Actions may fail if the target is surrounded or the unit cannot find a path
	 * to the unit. To get the action results from the previous turn you can do
	 * something similar to the following. Please be aware that on the first turn
	 * you should not call this
	 *
	 * Map<Integer, ActionResult> actionResults =
	 * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
	 * for(ActionResult result : actionResults.values()) {
	 * System.out.println(result.toString()); }
	 *
	 * @return New actions to execute or nothing if an event has not occurred.
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

		// update rewards
		for (Integer footman : myFootmen) {
			calculateReward(stateView, historyView, footman);
		}

		// remove killed units
		removeKilledUnits(stateView, historyView);

		// find idle friendly units
		List<Integer> idleFriendlyUnits = findIdleFriendlyUnits(stateView, historyView);

		// assign idle friendly units to attack enemy
		for (int idleFriendlyUnit : idleFriendlyUnits) {
			int enemyUnit = selectAction(stateView, historyView, idleFriendlyUnit);
			actions.put(idleFriendlyUnit, Action.createCompoundAttack(idleFriendlyUnit, enemyUnit));

			// update weights
			if (isLearning) {
				weights = convertdoubleToDoubleArray(
						updateWeights(convertDoubleTodoubleArray(weights), episodeFeatures.get(idleFriendlyUnit),
								episodeRewards.get(idleFriendlyUnit), stateView, historyView, idleFriendlyUnit));
			}
		}

		return actions;
	}

	private Double[] convertdoubleToDoubleArray(double[] array) {
		Double[] newArray = new Double[array.length];
		for (int i = 0; i < newArray.length; i++)
			newArray[i] = array[i];

		return newArray;
	}

	private double[] convertDoubleTodoubleArray(Double[] array) {
		double[] newArray = new double[array.length];
		for (int i = 0; i < newArray.length; i++)
			newArray[i] = array[i];

		return newArray;
	}

	/**
	 * Remove killed units from the footmen vectors.
	 * 
	 * @param stateView
	 *            the current StateView
	 * @param historyView
	 *            the current HistoryView
	 */
	private void removeKilledUnits(State.StateView stateView, History.HistoryView historyView) {
		// if it is not the first turn
		if (stateView.getTurnNumber() > 0) {
			for (DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() - 1)) {

				// if a friendly unit was killed
				if (playernum == deathLog.getController())
					myFootmen.remove(new Integer(deathLog.getDeadUnitID()));

				// if an enemy unit was killed
				if (ENEMY_PLAYERNUM == deathLog.getController())
					enemyFootmen.remove(new Integer(deathLog.getDeadUnitID()));
			}
		}
	}

	/**
	 * Find all idle friendly units.
	 * 
	 * @param stateView
	 *            the current StateView
	 * @param historyView
	 *            the current HistoryView
	 * @return a list of idle friendly units
	 */
	private List<Integer> findIdleFriendlyUnits(State.StateView stateView, History.HistoryView historyView) {
		List<Integer> idleFootmen = new ArrayList<Integer>();

		// if it is not the first turn
		if (stateView.getTurnNumber() > 0) {
			Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum,
					stateView.getTurnNumber() - 1);
			for (ActionResult result : actionResults.values()) {

				if (myFootmen.contains(result.getAction().getUnitId())) {
					// if an action failed
					if (ActionFeedback.FAILED == result.getFeedback())
						idleFootmen.add(new Integer(result.getAction().getUnitId()));

					// if an action was completed
					if (ActionFeedback.COMPLETED == result.getFeedback()) {
						idleFootmen.add(new Integer(result.getAction().getUnitId()));
					}
				}
			}
		}
		// if it is the first turn
		else if (stateView.getTurnNumber() == 0) {
			idleFootmen = myFootmen;
		}

		return idleFootmen;
	}

	/**
	 * Here you will calculate the cumulative average rewards for your testing
	 * episodes. If you have just finished a set of test episodes you will call out
	 * testEpisode.
	 *
	 * It is also a good idea to save your weights with the saveWeights function.
	 */
	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

		// learning phase
		if (isLearning) {
			if (episodesCompleted % 15 == 14) {
				// do something
				isLearning = false;
				cumulativeRewards = 0;
			}
		}

		// evaluation phase
		else {
			if (episodesCompleted % 15 == 4) {
				// average rewards
				averageRewards.add(cumulativeRewards / NUM_EVALUATING);
				isLearning = true;
				cumulativeRewards = 0;
				printTestData(averageRewards);
			}
		}

		episodesCompleted++;

		if (episodesCompleted > 29990) {
			System.out.println("At episode " + episodesCompleted);
		}
		if (episodesCompleted >= numEpisodes) {
			saveWeights(weights);
			printLearningCurve(averageRewards);
			System.exit(0);

		}
	}

	/**
	 * Calculate the updated weights for this agent.
	 * 
	 * @param oldWeights
	 *            Weights prior to update
	 * @param oldFeatures
	 *            Features from (s,a)
	 * @param totalReward
	 *            Cumulative discounted reward for this footman.
	 * @param stateView
	 *            Current state of the game.
	 * @param historyView
	 *            History of the game up until this point
	 * @param footmanId
	 *            The footman we are updating the weights for
	 * @return The updated weight vector.
	 */
	public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward,
			State.StateView stateView, History.HistoryView historyView, int footmanId) {
		double[] updatedWeights = new double[NUM_FEATURES];

		double maxQ = Double.NEGATIVE_INFINITY;
		double q = 0;
		int enemyId = -1;
		/**
		 * use Q value calculations to get the max Q and enemy Id
		 */
		for (int enemy : enemyFootmen) {
			for (int i = 0; i < NUM_FEATURES; i++) {
				q += oldFeatures[i] * oldWeights[i];
			}
			double currentQ = calcQValue(stateView, historyView, footmanId, enemy);
			if (currentQ > maxQ) {
				enemyId = enemy;
				maxQ = currentQ;
			}
		}

		for (int i = 0; i < NUM_FEATURES; i++) {
			q += oldFeatures[i] * oldWeights[i];
		}

		double[] newFeatures = calculateFeatureVector(stateView, historyView, footmanId, enemyId);
		for (int i = 0; i < NUM_FEATURES; i++) {
			updatedWeights[i] = oldWeights[i] + learningRate * ((totalReward + gamma * maxQ) - q) * newFeatures[i];
		}
		return updatedWeights;
	}

	/**
	 * Given a footman and the current state and history of the game select the
	 * enemy that this unit should attack. This is where you would do the
	 * epsilon-greedy action selection.
	 *
	 * @param stateView
	 *            Current state of the game
	 * @param historyView
	 *            The entire history of this episode
	 * @param attackerId
	 *            The footman that will be attacking
	 * @return The enemy footman ID this unit should attack
	 */
	public int selectAction(State.StateView stateView, History.HistoryView historyView, int attackerId) {
		double randomValue = random.nextDouble();
		int bestEnemy = -1;
		double bestQValue = Double.NEGATIVE_INFINITY;

		for (int enemyId : enemyFootmen) {
			double currentQValue = calcQValue(stateView, historyView, attackerId, enemyId);
			if (currentQValue > bestQValue) {
				bestQValue = currentQValue;
				bestEnemy = enemyId;
			}
		}

		if (isLearning) {
			// choose best Enemy
			if (randomValue > epsilon) {
				// update features
				double[] features = calculateFeatureVector(stateView, historyView, attackerId, bestEnemy);
				if (episodeFeatures.containsKey(attackerId))
					episodeFeatures.replace(attackerId, features);
				else
					episodeFeatures.put(attackerId, features);

				return bestEnemy;
			}
			// pick random enemy when random value is < epsilon
			else {
				int randomEnemy = enemyFootmen.get(random.nextInt(enemyFootmen.size()));
				double[] features = calculateFeatureVector(stateView, historyView, attackerId, randomEnemy);
				if (episodeFeatures.containsKey(attackerId))
					episodeFeatures.replace(attackerId, features);
				else
					episodeFeatures.put(attackerId, features);

				return randomEnemy;
			}
		} else {
			double[] features = calculateFeatureVector(stateView, historyView, attackerId, bestEnemy);
			if (episodeFeatures.containsKey(attackerId))
				episodeFeatures.replace(attackerId, features);
			else
				episodeFeatures.put(attackerId, features);

			return bestEnemy;
		}
	}

	/**
	 * Given the current state and the footman in question calculate the reward
	 * received on the last turn. This is where you will check for things like Did
	 * this footman take or give damage? Did this footman die or kill its enemy. Did
	 * this footman start an action on the last turn? See the assignment description
	 * for the full list of rewards.
	 *
	 * Remember that you will need to discount this reward based on the timestep it
	 * is received on. See the assignment description for more details.
	 *
	 * As part of the reward you will need to calculate if any of the units have
	 * taken damage. You can use the history view to get a list of damages dealt in
	 * the previous turn. Use something like the following.
	 *
	 * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
	 * System.out.println("Defending player: " + damageLog.getDefenderController() +
	 * " defending unit: " + \ damageLog.getDefenderID() + " attacking player: " +
	 * damageLog.getAttackerController() + \ "attacking unit: " +
	 * damageLog.getAttackerID()); }
	 *
	 * You will do something similar for the deaths. See the middle step
	 * documentation for a snippet showing how to use the deathLogs.
	 *
	 * To see if a command was issued you can check the commands issued log.
	 *
	 * Map<Integer, Action> commandsIssued =
	 * historyView.getCommandsIssued(playernum, lastTurnNumber); for
	 * (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
	 * System.out.println("Unit " + commandEntry.getKey() + " was command to " +
	 * commandEntry.getValue().toString); }
	 *
	 * @param stateView
	 *            The current state of the game.
	 * @param historyView
	 *            History of the episode up until this turn.
	 * @param footmanId
	 *            The footman ID you are looking for the reward from.
	 * @return The current reward
	 */
	public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId) {
		double currentReward = 0;
		int lastTurnNumber = stateView.getTurnNumber() - 1;
		int attackedEnemyUnit = -1;

		// update reward based on damages
		for (DamageLog damageLog : historyView.getDamageLogs(lastTurnNumber)) {

			// if the footman is the defending unit and is a friendly unit
			if (playernum == damageLog.getDefenderController() && footmanId == damageLog.getDefenderID())
				currentReward -= damageLog.getDamage();

			// if the footman is the attacking unit and is a friendly unit
			if (playernum == damageLog.getAttackerController() && footmanId == damageLog.getAttackerID()) {
				currentReward += damageLog.getDamage();
				attackedEnemyUnit = damageLog.getDefenderID();
			}
		}

		// update reward based on deaths
		for (DeathLog deathLog : historyView.getDeathLogs(lastTurnNumber)) {

			// if the footman died and is a friendly unit
			if (playernum == deathLog.getController() && footmanId == deathLog.getDeadUnitID())
				currentReward -= 100;

			// if the attacked enemy unit died
			if (attackedEnemyUnit != -1) {
				if (ENEMY_PLAYERNUM == deathLog.getController() && attackedEnemyUnit == deathLog.getDeadUnitID())
					currentReward += 100;
			}
		}

		// update reward based on commands
		Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
		for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
			// System.out.println("Unit " + commandEntry.getKey() + " was command to " +
			// commandEntry.getValue().toString());

			// if the footman started an action in the last turn
			if (footmanId == commandEntry.getKey())
				currentReward -= 0.1;
		}

		// if the current episode is an evaluation episode
		if (!isLearning) {
			cumulativeRewards += currentReward;
		}

		// discount the reward
		// update the rewards for the corresponding footman
		if (episodeRewards.containsKey(footmanId)) {
			// get the reward up to the last turn
			double previousReward = episodeRewards.get(footmanId);
			currentReward = previousReward + Math.pow(gamma, stateView.getTurnNumber()) * currentReward;
			episodeRewards.replace(footmanId, currentReward);
		} else {
			episodeRewards.put(footmanId, currentReward);
		}

		return currentReward;
	}

	/**
	 * Calculate the Q-Value for a given state action pair. The state in this
	 * scenario is the current state view and the history of this episode. The
	 * action is the attacker and the enemy pair for the SEPIA attack action.
	 *
	 * This returns the Q-value according to your feature approximation. This is
	 * where you will calculate your features and multiply them by your current
	 * weights to get the approximate Q-value.
	 *
	 * @param stateView
	 *            Current SEPIA state
	 * @param historyView
	 *            Episode history up to this point in the game
	 * @param attackerId
	 *            Your footman. The one doing the attacking.
	 * @param defenderId
	 *            An enemy footman that your footman would be attacking
	 * @return The approximate Q-value
	 */
	public double calcQValue(State.StateView stateView, History.HistoryView historyView, int attackerId,
			int defenderId) {
		double qVal = weights[0];
		double[] currentFeatures = calculateFeatureVector(stateView, historyView, attackerId, defenderId);

		for (int i = 1; i < NUM_FEATURES; i++) {
			qVal = qVal + (currentFeatures[i] + weights[i]);
		}
		return qVal;
	}

	/**
	 * Given a state and action calculate your features here. Please include a
	 * comment explaining what features you chose and why you chose them.
	 *
	 * All of your feature functions should evaluate to a double. Collect all of
	 * these into an array. You will take a dot product of this array with the
	 * weights array to get a Q-value for a given state action.
	 *
	 * It is a good idea to make the first value in your array a constant. This just
	 * helps remove any offset from 0 in the Q-function. The other features are up
	 * to you. Many are suggested in the assignment description.
	 *
	 * @param stateView
	 *            Current state of the SEPIA game
	 * @param historyView
	 *            History of the game up until this turn
	 * @param attackerId
	 *            Your footman. The one doing the attacking.
	 * @param defenderId
	 *            An enemy footman. The one you are considering attacking.
	 * @return The array of feature function outputs.
	 */
	public double[] calculateFeatureVector(State.StateView stateView, History.HistoryView historyView, int attackerId,
			int defenderId) {
		/**
		 * the features we implemented are: whether the enemy being attacked is the
		 * closest one to the footman, the health ratio between the footman and
		 * attacker, the number of footmen attacking the enemy, the number of enemies
		 * attacking the footman, whether the enemy that is being attacked is the enemy
		 * with the lowest health
		 */

		double[] featureVector = new double[NUM_FEATURES];

		final double CONSTANT = 1;

		// chebyshev distance
		double isClosestChebyshev = (featureIsClosestEnemy(attackerId, defenderId, stateView)) ? 1.0 : 0.0;

		// ratio of health points between attacker/defender
		double healthRatio = featureHealthPointsRatio(attackerId, defenderId, stateView);

		// number of other footmen attacking the defender
		double numFootmenAttackingE = featureNumFootmenAttackingE(stateView, historyView, attackerId, defenderId);

		// number of enemy attacking our footman
		double numAttackingMe = (featureCurrentlyAttackingMe(stateView, historyView, attackerId, defenderId)) ? 1.0
				: 0.0;

		double isWeakestEnemy = (featureIsWeakestEnemy(stateView, historyView, defenderId)) ? 1.0 : 0.0;

		featureVector[0] = CONSTANT;
		featureVector[1] = isClosestChebyshev;
		featureVector[2] = healthRatio;
		featureVector[3] = numFootmenAttackingE;
		featureVector[4] = numAttackingMe;
		featureVector[5] = isWeakestEnemy;
		return featureVector;

	}

	/**
	 * determines whether the enemy is the closest one to the friendly footman
	 * 
	 * @param friendlyID
	 *            the friendly unit ID
	 * @param stateView
	 *            the current StateView
	 * @return whether the enemy is the closest one to the friendly footman
	 */
	private boolean featureIsClosestEnemy(int friendlyID, int enemyID, State.StateView stateView) {
		UnitView friendlyUnit = stateView.getUnit(friendlyID);
		int friendlyX = friendlyUnit.getXPosition();
		int friendlyY = friendlyUnit.getYPosition();

		int bestID = 0;
		int bestChebyshev = Integer.MAX_VALUE;

		for (int enemy : enemyFootmen) {
			UnitView enemyUnit = stateView.getUnit(enemy);
			int enemyX = enemyUnit.getXPosition();
			int enemyY = enemyUnit.getYPosition();

			int currentChebyshev = Chebyshev(friendlyX, friendlyY, enemyX, enemyY);
			if (currentChebyshev < bestChebyshev) {
				bestChebyshev = currentChebyshev;
				bestID = enemyID;
			}
		}
		if (bestID == enemyID) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Calculates the Chebyshev distance for two points.
	 * 
	 * @param x1
	 *            the x value for the first point
	 * @param y1
	 *            the y value for the first point
	 * @param x2
	 *            the x value for the second point
	 * @param y2
	 *            the y value for the second point
	 * @return returns the Chebyshev distance between the two points
	 */
	private int Chebyshev(int x1, int y1, int x2, int y2) {
		return Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	/**
	 * returns the health ratio (attacker health / defender health) for the given
	 * friendly and enemy unit
	 */
	private double featureHealthPointsRatio(int attackerId, int defenderId, State.StateView stateView) {
		UnitView friendlyUnit = stateView.getUnit(attackerId);
		UnitView enemyUnit = stateView.getUnit(defenderId);

		int friendlyHP = friendlyUnit.getHP();
		int enemyHP = enemyUnit.getHP();

		return friendlyHP / enemyHP;
	}

	// number of footmen attacking the enemy last turn
	private double featureNumFootmenAttackingE(StateView stateView, HistoryView historyView, int attackerId,
			int defenderId) {
		int turnNumber = stateView.getTurnNumber() - 1;
		int numAttackingE = 0;
		// turn number might cause an error
		Map<Integer, Action> previousCommands = historyView.getCommandsIssued(PLAYERNUM, turnNumber);
		for (Action action : previousCommands.values()) {
			if (action.getType() == ActionType.PRIMITIVEATTACK) {
				TargetedAction target = (TargetedAction) action;
				if (target.getTargetId() == defenderId) {
					numAttackingE++;
				}
			}
		}
		return numAttackingE;
	}

	// is e currently attacking me?
	private boolean featureCurrentlyAttackingMe(StateView stateView, HistoryView historyView, int attackerId,
			int defenderId) {
		int turnNumber = stateView.getTurnNumber() - 1;
		Map<Integer, Action> previousCommands = historyView.getCommandsIssued(PLAYERNUM, turnNumber);
		for (Action action : previousCommands.values()) {
			if (action.getType() == ActionType.PRIMITIVEATTACK) {
				TargetedAction target = (TargetedAction) action;
				if (target.getTargetId() == attackerId && target.getUnitId() == defenderId) {
					return true;
				}
			}
		}
		return false;
	}

	// returns true if the enemy provided has the lowest health
	private boolean featureIsWeakestEnemy(StateView stateView, HistoryView historyView, int enemyId) {
		UnitView currentEnemy = stateView.getUnit(enemyId);
		int enemyHP = currentEnemy.getHP();
		for (int enemy : enemyFootmen) {
			if (stateView.getUnit(enemy).getHP() < enemyHP) {
				return false;
			}
		}
		return true;
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * Prints the learning rate data described in the assignment. Do not modify this
	 * method.
	 *
	 * @param averageRewards
	 *            List of cumulative average rewards from test episodes.
	 */
	public void printTestData(List<Double> averageRewards) {
		System.out.println("");
		System.out.println("Games Played      Average Cumulative Reward");
		System.out.println("-------------     -------------------------");
		for (int i = 0; i < averageRewards.size(); i++) {
			String gamesPlayed = Integer.toString(10 * i);
			String averageReward = String.format("%.2f", averageRewards.get(i));

			int numSpaces = "-------------     ".length() - gamesPlayed.length();
			StringBuffer spaceBuffer = new StringBuffer(numSpaces);
			for (int j = 0; j < numSpaces; j++) {
				spaceBuffer.append(" ");
			}
			System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
		}
		System.out.println("");
	}

	// writes the average cumulative rewards to a file to make the learning curve
	private void printLearningCurve(List<Double> averageRewards) {
		File path = new File("agent_weights/LC.txt");
		// create the directories if they do not already exist
		path.getAbsoluteFile().getParentFile().mkdirs();

		try {
			// open a new file writer. Set append to false
			BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

			for (Double reward : averageRewards) {
				writer.write(String.format("%f\n", reward));
			}

			writer.flush();
			writer.close();
		} catch (IOException ex) {
			System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
		}
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * This function will take your set of weights and save them to a file.
	 * Overwriting whatever file is currently there. You will use this when training
	 * your agents. You will include th output of this function from your trained
	 * agent with your submission.
	 *
	 * Look in the agent_weights folder for the output.
	 *
	 * @param weights
	 *            Array of weights
	 */
	public void saveWeights(Double[] weights) {
		File path = new File("agent_weights/weights.txt");
		// create the directories if they do not already exist
		path.getAbsoluteFile().getParentFile().mkdirs();

		try {
			// open a new file writer. Set append to false
			BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

			for (double weight : weights) {
				writer.write(String.format("%f\n", weight));
			}
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
		}
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * This function will load the weights stored at agent_weights/weights.txt. The
	 * contents of this file can be created using the saveWeights function. You will
	 * use this function if the load weights argument of the agent is set to 1.
	 *
	 * @return The array of weights
	 */
	public Double[] loadWeights() {
		File path = new File("agent_weights/weights.txt");
		if (!path.exists()) {
			System.err.println("Failed to load weights. File does not exist");
			return null;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			List<Double> weights = new LinkedList<>();
			while ((line = reader.readLine()) != null) {
				weights.add(Double.parseDouble(line));
			}
			reader.close();

			return weights.toArray(new Double[weights.size()]);
		} catch (IOException ex) {
			System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
		}
		return null;
	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}
}

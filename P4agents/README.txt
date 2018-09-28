Clarinda Ho (cqh), Veijay Raj (vxr137), Vishal Shah (vcs9)
Professor Ray
EECS 391, Class #4163
24 March 2018

AI Programming Assignment #4


New Files
- DepositGoldAction
- DepositWoodAction
- HarvestGoldAction
- HarvestWoodAction
- MoveAction
- BuildPeasantAction

Modified Files:
- GameState
- PlannerAgent
- PEAgent
- StripsAction


GameState:
The heuristic() method will determine the heuristic value for a given gameState by taking into account whether a peasant is holding something and whether it can harvest or deposit on a given action
The cost is updated when the new gamestate iscreated in each StripsAction class

PlannerAgent:
The AStarSearch() method will pick the best child for the next action to be executed by using an open and closed list of possible actions.

PEAgent:
The middelStep will execute the plan when all previous actions have already been implemented

DepositGoldAction:
A STRIPS action that will create a new game state after gold has been deposited if the preconditions are met.

DepositWoodAction:
A STRIPS action that will create a new game state after wood has been deposited if the preconditions are met.

HarvestGoldAction:
A STRIPS action that will create a new game state after good has been harvested if the preconditions are met.

HarvestWoodAction:
A STRIPS action that will create a new game state after wood has been harvested if the preconditions are met.

BuildPeasantAction:
A STRIPS action that will create a new game state after a peasant has been created if the preconditions are met.


Each Person's Responsibilities:
We worked on it altogether at the same time using paired programming, so the commits do not accurately represent what each person did since we all worked on it together.


Clarinda: 
-BuildPeasantAction
-PEAgent, toString() method to write each STRIPS action to a file
-updated AStarSearch in PlannerAgent

Vishal: 
-MoveAction
-HarvestGoldAction
-reworked GameState's generateChildren() method to compensate for CompoundMove rather than PrimitiveMove, 
-findBestResource in GameState

Veijay: 
-AStarSearch in PlannerAgent, 
-DepositWoodAction, DepositGoldAction, HarvestWoodAction
-heuristic() in GameState


General Comments:
The API became more clear after having used it in the previous project. SOme confusing parts were having to set the unitTemplate's canGather value to true before using it for a peasant unit.
Other issues that we faced were resourceNodes not updating as we would expect.
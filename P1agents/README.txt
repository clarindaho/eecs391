Clarinda Ho (cqh), Veijay Raj (vxr137), Vishal Shah (vcs9)
Professor Ray
EECS 391, Class #4163
1 February 2017

AI Programming Assignment #1


Modified Agents:
- FirstClass.java
- MyCombatAgent.java

First Class: We modified the code from the Building Units example to build farms, a barracks, and walls. The scenario will finish if either the wood or gold amount surpasses 1000, so the barracks, some of the farms, and some of the walls may not be built.
- The farm costs 500 Gold and 250 Wood. The agent will create 5 of them around the coordinates (5, 5) if it has enough resources.
- The agent will create 1 barracks if it has enough resources.
- The walls cost 10 Wood. The agent will create 8 of them only if it has enough Wood and at least 3 farms have already been created (under the assumption that creating farms first is more important than walls).
- The peasants cost 50 gold. The agent will create peasants until it reaches a peasant cap of 8.

My Combat Agent: We modified the code from the Combat Agent example to initially move our units to a spot further away from the enemy in order to lure the enemy units out and away from the Scout Tower. Then our units attack the enemy units (that are not the Scout Tower). The scenario will finish if either all our units die, or all our enemy units die. We managed to kill only one enemy footman so far.


Each Person's Responsibilities:
Clarinda: modified My Combat Agent to have our units move and attack; added the peasant cap and building barracks code to First Class; improved the efficiency of First Class
Veijay: added building farms code to First Class
Vishal: added building walls code to First Class


Documentation Experience: 
- When setting up the project, Veijay ran into an error about JAXBException not being found when trying to start the jvm. He tried the solutions recommended in the StackOverflow thread, but they didn't work. In the end, he had to remove Java 9 and download Java 8.
Clarinda Ho (cqh), Veijay Raj (vxr137), Vishal Shah (vcs9)
Professor Ray
EECS 391, Class #4163
5 February 2018

AI Programming Assignment #3


Modified Agents:
- Game State
- Minimax Alpha Beta

Game State:
- added implementation to determine game state information (such as footmen location) from the current state
- added a utility function based on distance between footmen and closest enemy archer, archer's total HP, footmen's total HP
- added implementation to get all children of the current game state 
- creates a new state for each child based on the actions that it performs


Minimax Alpha Beta:
- Fixed errors
- Added comparator
- Added functionality for alphaBetaSearch
- Added functionality for orderChildrenByHeuristic to sort the children by their utility


Each Person's Responsibilities:
Clarinda: Wrote the constructor for GameState. Created fields for GameState class. Added the createNewGameState method.
Veijay: Added to getChildren() method and created method to check whether a move is legal. Fixed errors in MinimaxAlphaBeta class and added comparator and alphaBetaSearch method.
Vishal: Created getUtility() method for GameState. Created orderChildrenWithHeuristics. Fix errors with createNewGameState. Located turnNumber issue where children keep same turnNumber.


General Comments:
The Sepia documentation and API were difficult to follow and use. Most of our group time was spent trying to understand the documentation compared to implementing the alphaBetaSearch and the other AI concepts.

The one problem that we had was that the turn numbers were not changing for the children even though we would switch it when creating the new child.

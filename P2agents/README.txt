Clarinda Ho (cqh), Veijay Raj (vxr137), Vishal Shah (vcs9)
Professor Ray
EECS 391, Class #4163
5 February 2017

AI Programming Assignment #2


Modified Agents:
- Moves implements the A* search algorithm which relies on the Chebyshev calculation for the heuristic function
- shouldReplanPath checks the next five moves in the current path and the enemy footman location to determine whether we should replan the path (if the enemy footman is in the way) or keep the current path


Errors That We Noticed:
- After successfully finding a path and reaching the townhall, an error message that says "Invalid path. Could not determine direction" showed up after all the execution time results are printed. We suspect that this error message shows up because middleStep is repeatedly called even though terminalStep has been called.
- For the dynamic map, the execution times are repeatedly printing even after the map has been completed. The turn numbers are updated every time, but the execution times remain the same. We also suspect that this is because middleStep is repeatedly called even though terminalStep has been called.
We did not fix these errors since they were likely caused by other parts of the code that we did not write. Since the instructions said to modify AstarSearch() and shouldReplan(), we did not alter the other code.

Each Person's Responsibilities:
Clarinda: Made the minimumChebyshev() and Chebyshev() helper methods; updated the shouldReplanPath method to check in advance 5 moves in the current path
Veijay: Made the possibleMoves() helper method; started the shouldReplanPath method
Vishal: Made the while loop in the AstarSearch() method which calls all of the helper methods; added ability to change path and pop moves off the stack
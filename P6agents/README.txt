Clarinda Ho (cqh), Veijay Raj (vxr137), Vishal Shah (vcs9)
Professor Ray
EECS 391, Class #4163
18 April 2018

AI Programming Assignment #5

Modified Agents:
- RLAgent

Each Person's Responsibilities:
We worked on it altogether at the same time using paired programming, so the commits do not accurately represent what each person did since we all worked on it together.

Clarinda: Updated calculateReward and added map to track rewards for each footman, Updated middleStep and findIdleFriendlyUnits
Veijay: fixed updateWeights() - errors with q value, terminalStep(), created graphs
Vishal: Coded selectAction() and updated calculateFeatureVector() in RLAgent, fixed NullPointer errors in middleStep and findFriendlyIdleUnits()

General Comments:

This week, we mainly debugged our code that we submitted for the first commit. We add features such as checking if the enemy is the weakest among all the enemies.
We added functionality to track the rewards and features for each footman for a given epiode to make it easier to update the weights for each.

One thing that we were confused about initially remember what color we were and since both our friendly footmen and enemy footmen were labelled with "f", we were not sure which side was winning.
Having worked with the SEPIA API and documentation in previous projects, there were not any major issues this week with SEPIA-related bugs.

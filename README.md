# TheAccountant
My solution for The Accountant contest on CodinGames in which I placed 158th out of 2328

The solution consists of search algorithm with pruning.
For each depth a list of candidate moves are generated.
The moves are the following:
  
  
  -For the 4 most dangerous enemies:
    -If shooting this enemy will remove more than one third of his health or the enemy is closest than 3000 units then shoot
    -If this enemy's HP is higher than the damage I can do from this distance and distance is higher than 3000 units then move towards this enemy.
   -For enemies that are closer than 3000 units run away from these enemies average coord
While expanding the tree:
  -If a gamestate's score is 0 or below then we prune it from the tree.
Finally we choose the move that generates the highest score.

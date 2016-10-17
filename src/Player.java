import com.sun.org.apache.xpath.internal.operations.Equals;
import java.io.File;
import java.util.*;

class Enemy implements Comparable<Enemy>{
    public int id;
    public int x;
    public int y;
    public int life;
    public int mvX;
    public int mvY;
    public GameState gameState;
    public int dangerIndex;
    public DataPoint closestDatapoint;
    private boolean moved;
    public static int MAX_DAMAGE = 14;

    public Enemy(int id, int x, int y, int life, GameState gameState){
        this.id = id;
        this.x = x;
        this.y = y;
        this.life = life;
        this.gameState = gameState;
        this.closestDatapoint = null;
        moved = false;
        updateDanger();
    }
    
    private double getDistanceToDatapoint(DataPoint dp){
        return Math.sqrt( ( (dp.x-x)*(dp.x-x) + (dp.y-y)*(dp.y-y) ) );
    }
    
    public int getTurnsToKill(){
        int turnsToKill = (int) Math.ceil( (float)life/(float)MAX_DAMAGE );
        turnsToKill += (int)Math.ceil((float)(gameState.wolff.getDistanceFrom(this)-2000)/1000d);
        int turnsToKillFromHere = (int) Math.ceil ( (float) life / (float)  getDamageFromWolff() );
        return Math.min(turnsToKill, turnsToKillFromHere);
    }
    
    public DataPoint getClosestDatapoint(){
        closestDatapoint = gameState.exists(closestDatapoint) ? closestDatapoint : null;
        if (closestDatapoint == null) {
            long dist = Long.MAX_VALUE;
            long tempDist;
            for (DataPoint dp : gameState.dataPoints.keySet()) {
                tempDist = (dp.x - x) * (dp.x - x) + (dp.y - y) * (dp.y - y);
                if (tempDist < dist) {
                    dist = tempDist;
                    closestDatapoint = dp;
                }
                if (tempDist == dist && dp.id < closestDatapoint.id) {
                    dist = tempDist;
                    closestDatapoint = dp;
                }
            }
        }
        return closestDatapoint;
    }
    
    public Enemy(Enemy copyFrom, GameState copyGameState){
        id = copyFrom.id;
        x = copyFrom.x;
        y = copyFrom.y;
        life = copyFrom.life;
        gameState = copyGameState;
        closestDatapoint = copyFrom.closestDatapoint;
    }

    public void move(){
        if(!moved) {
            DataPoint dp = getClosestDatapoint();
            double actualDistance = getDistanceToDatapoint(dp);
            if (actualDistance <= 500) {
                gameState.markForRemoval(dp, this);
                mvX = dp.x - x;
                mvY = dp.y - y;
                x = dp.x;
                y = dp .y;
            } else {
                mvX = (int)(((dp.x - x) * 500d)/actualDistance);
                mvY = (int)(((dp.y - y) * 500d)/actualDistance);
                x += ((dp.x - x) * 500d)/actualDistance;
                y += ((dp.y - y) * 500d)/actualDistance;
            }
            updateDanger();
            moved = true;
        }
    }

    public void resetMove(){
        moved = false;
    }

    public void updateDanger(){
        float distanceFromWolff = (float)Math.sqrt( (x-gameState.wolff.x)*(x-gameState.wolff.x) + (y-gameState.wolff.y)*(y-gameState.wolff.y) ) - 2000f;
        int dangerToWolff = (int) (distanceFromWolff/500d);
        if(distanceFromWolff < 500){
            dangerIndex = -1;
            return;
        }
        DataPoint dp = getClosestDatapoint();
        int dangerToDatapoint = (int) Math.ceil((double)getDistanceToDatapoint(dp)/500d) -getTurnsToKill();
        if(dangerToDatapoint < 0 && gameState.dataPoints.size() > 1){
            Set<DataPoint> taken = new HashSet<>();
            taken.add(dp);
            while (dangerToDatapoint < 0 && taken.size() < gameState.dataPoints.size()) {
                int lowestDist = Integer.MAX_VALUE;
                DataPoint closest = dp;
                for (DataPoint dpIter : gameState.dataPoints.keySet()) {
                    if (!taken.contains(dpIter)) {
                        int iterDist = (int) Math.sqrt((closest.x - dpIter.x) * (closest.x - dpIter.x) + (closest.y - dpIter.y) * (closest.y - dpIter.y));
                        if (iterDist < lowestDist) {
                            lowestDist = iterDist;
                            closest = dpIter;
                        }
                    }
                }
                dangerToDatapoint += lowestDist;
                taken.add(closest);
            }
        }
        dangerIndex = Math.min(dangerToDatapoint, dangerToWolff);
    }

    public int shotAt(){
        int damage = getDamageFromWolff();
        if(damage > life)
            damage = life;
        life -= damage;
        if(life == 0)
            gameState.remove(this);
        return damage;
    }

    public int getDamageFromWolff(){
        double distance = Math.sqrt( (gameState.wolff.x-x) * (gameState.wolff.x-x) + (gameState.wolff.y-y)*(gameState.wolff.y-y) );
        int damage = (int) Math.round(125000d/Math.pow(distance, 1.2d));
        return damage;
    }

    @Override
    public boolean equals(Object o){
        if( o instanceof Enemy)
            return id == ((Enemy) o).id;
        return false;
    }

    @Override
    public int hashCode(){
        return id;
    }

    @Override
    public int compareTo(Enemy t) {
        return dangerIndex - t.dangerIndex;
    }

    @Override
    public String toString(){
        return "Enemy" + id + ": " + x + " " + y + " Life: " + life + " Damage: " + getDamageFromWolff() + " Danger: " + dangerIndex + " Closest dp: " + closestDatapoint + " Ttk: " + getTurnsToKill() + " Dist: " + gameState.wolff.getDistanceFrom(this);
    }
}

class DataPoint{
    public int x;
    public int y;
    public int id;

    public DataPoint(int id, int x, int y){
        this.x = x;
        this.y = y;
        this.id = id;
    }
    
    @Override
    public boolean equals(Object o){
        if( o instanceof DataPoint)
            return x == ((DataPoint) o).x &&  y == ((DataPoint) o).y;
        return false;
    }

    @Override
    public int hashCode(){
        return 31*x + y;
    }

    @Override
    public String toString(){
        return "DataPoint" + id + ": " + x + " " + y;
    }
}

class Wolff{
    public int x;
    public int y;
    public boolean isDead;
    
    public Wolff(){
        isDead = false;
    }
    
    public Wolff(Wolff copyFrom){
        x = copyFrom.x;
        y = copyFrom.y;
    }

    public double getDistanceFrom(Enemy enemy){
        return Math.sqrt( (x-enemy.x)*(x-enemy.x) + (y-enemy.y)*(y-enemy.y) );
    }
    
    public void move(int targetX, int targetY){
        float distanceFromTarget = (float)Math.sqrt( (x-targetX)*(x-targetX) + (y-targetY)*(y-targetY) );
        float ratio = (float) ((distanceFromTarget>1000) ? 1000f/distanceFromTarget : 1f);
        x += (targetX-x)*ratio;
        x = Math.max(0, Math.min(15999, x));
        y += (targetY-y)*ratio;
        y = Math.max(0, Math.min(8999, y));
    }

    @Override
    public String toString(){
        return "Wolff: " + x + " " + y + ( isDead ? " DEAD" : ""); 
    }
}

class WolffAction{
    public static int ACTION_ID_TRACKER;
    private int id;
    public boolean shoot;
    public int targetId;
    public int targetX;
    public int targetY;

    public WolffAction(boolean shoot, int enemyId, int targetX, int targetY){
        this.shoot = shoot;
        this.targetId = enemyId;
        this.targetX = targetX;
        this.targetY = targetY;
        id = ACTION_ID_TRACKER;
        ACTION_ID_TRACKER++;
    }
    
    public int getId(){
        return id;
    }
    
    @Override
    public boolean equals(Object o){
        if(!(o instanceof WolffAction)) return false;
        return id == ((WolffAction)o).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString(){
        return ( shoot ? "SHOOT " : "MOVE " ) + " TargetId: " + targetId + " TargetX: " + targetX + " TargetY: " + targetY; 
    }
}

class GameState{
    public static int ID_TRACKER;
    public static int EVALED_ENEMIES;
    public static int INSTATION_TIME;
    Map<DataPoint, Integer> dataPoints;
    Map<Integer, Enemy> enemies;
    Integer markedForRemovalEnemy;
    List<Object[]> markedForRemovalDatapoints;
    Wolff wolff;
    List<GameState> childStates;
    List<WolffAction> interruptedActions;
    GameState parent;
    private WolffAction appliedAction;
    int totalEnemyLife;
    int shotsFired;
    int enemysKilled;
    int score;
    int damageCaused;
    int depth;
    int id;
    GameState bestChild;
    private boolean expanded;
    private int childrenPropagated;

    public GameState(){
        damageCaused = 0;
        score = -100;
        totalEnemyLife = 0;
        enemysKilled = 0;
        bestChild = null;
        markedForRemovalDatapoints = new ArrayList<>();
        markedForRemovalEnemy = -1;
        wolff = new Wolff();
        dataPoints = new HashMap<>();
        enemies = new HashMap<>();
        childStates = new ArrayList<>();
        parent = null;
        depth = 0;
        expanded = false;
        id = ID_TRACKER;
        ID_TRACKER++;
    }

    public GameState(GameState parentState, WolffAction action){
        long startInitTime = System.currentTimeMillis();
        score = -100;
        markedForRemovalDatapoints = new ArrayList<>();
        markedForRemovalEnemy = -1;
        damageCaused = parentState.damageCaused;
        enemysKilled = parentState.enemysKilled;
        totalEnemyLife = parentState.totalEnemyLife;
        shotsFired = parentState.shotsFired;
        wolff = new Wolff(parentState.wolff);
        dataPoints = new HashMap<>();
        dataPoints.putAll(parentState.dataPoints);
        bestChild = null;
        childStates = new ArrayList<>();
        enemies = new HashMap<>();
        for(Enemy e : parentState.enemies.values()){
            enemies.put(e.id, new Enemy(e, this));
        }
        this.parent = parentState;
        depth = parentState.depth + 1;
        applyAction(action);
        expanded = false;
        id = ID_TRACKER;
        ID_TRACKER++;
        INSTATION_TIME += System.currentTimeMillis()-startInitTime;
    }
    
    public boolean exists(DataPoint dataPoint){
        return dataPoints.containsKey(dataPoint);
    }
        
    public GameState getBestChild(){
        return bestChild;
    }
    
    public List<GameState> getChildStates(){
        if(!isGameOver()){
            List<WolffAction> potentialActions;
            if(!expanded){
                score = -100;
                expanded = true;
                potentialActions = getPotentialActions();
            }else if(interruptedActions != null && !interruptedActions.isEmpty()){
                potentialActions = interruptedActions;
            }else
                return childStates;
            for(WolffAction action : potentialActions){
                GameState child = new GameState(this, action);
                childStates.add(child);
                if (Brain.START_TIME + Brain.TIME_LIMIT < System.currentTimeMillis()) {
                    if(interruptedActions == null)
                        interruptedActions = new ArrayList<>();
                    interruptedActions = potentialActions.subList(potentialActions.indexOf(action) + 1, potentialActions.size());
                    return childStates;
                }
            }
            interruptedActions = null;
        }else{
            childStates.clear();
        }
        return childStates;
    }

    public WolffAction getAppliedAction() {
        return appliedAction;
    }

    public void addToTotalEnemyLife(int life){
        totalEnemyLife+=life;
    }
    
    public void calcDangers(){
        for(Enemy e : enemies.values() )
            e.updateDanger();
    }

    public void remove(Enemy enemy){
        enemysKilled++;
        markedForRemovalEnemy = enemy.id;
    }

    public void markForRemoval(DataPoint dataPoint, Enemy enemy){
        markedForRemovalDatapoints.add(new Object[]{ dataPoint, enemy.id });
    }

    public void clearDataPoints(){
        dataPoints.clear();
    }

    public void clearEnemies(){
        enemies.clear();
    }

    public void addDataPoint(int id, int x, int y){
        DataPoint toInsert = new DataPoint(id, x ,y);
        int count = dataPoints.containsKey(toInsert) ? dataPoints.get(toInsert) : 0;
        dataPoints.put(toInsert, count + 1);
    }

    public void removeDataPoint(DataPoint toRemove){
        int count = dataPoints.containsKey(toRemove) ? dataPoints.get(toRemove)-1 : 0;
        if(count == 0) dataPoints.remove(toRemove);
    }

    public void addEnemy(int id, int x, int y, int life){
        enemies.put(id, new Enemy(id, x, y, life, this) );
    }

    private List<WolffAction> getPotentialActions(){
        List<WolffAction> actions = new ArrayList<>();
        List<Enemy> mostDangerous = new ArrayList<>();
        mostDangerous.addAll(enemies.values());
        if(mostDangerous.size()>EVALED_ENEMIES){
            Collections.sort(mostDangerous);
            mostDangerous = mostDangerous.subList(0, EVALED_ENEMIES);
        }
        int averageX = 0;
        int averageY = 0;
        int numberOfCloseEnemies = 0;
        for(Enemy e : mostDangerous){
            int dist = (int) Math.ceil(wolff.getDistanceFrom(e));
            int damage = e.getDamageFromWolff();
            int life = e.life;
            if( life/3 <= damage || dist < 3000 )
                actions.add(new WolffAction(true, e.id, e.x, e.y));
            if( life > damage && dist > 3000){
                if(dist > 6000) actions.add(new WolffAction(false, e.id, e.x + (int)(wolff.getDistanceFrom(e)/2000)*e.mvX, e.y + (int)(wolff.getDistanceFrom(e)/2000)*e.mvY));
                else actions.add(new WolffAction(false, e.id, e.x, e.y));
            }
            if( dist < 3000 ){
                averageX += e.x;
                averageY += e.y;
                numberOfCloseEnemies++;
            }
        }
        if( numberOfCloseEnemies > 0 ){
            averageX /= numberOfCloseEnemies;
            averageY /= numberOfCloseEnemies;
            int runAwayToX = Math.min( 15999, Math.max( 0, wolff.x + (wolff.x - averageX) ) );
            int runAwayToY = Math.min( 8999, Math.max( 0, wolff.y + (wolff.y - averageY) ) );
            if( Math.abs( wolff.x - runAwayToX ) > Math.abs( wolff.y - runAwayToY ) ){
                actions.add(new WolffAction(false, -1, runAwayToX, runAwayToY + 1000));
            }
            if( Math.abs( wolff.x - runAwayToX ) <= Math.abs( wolff.y - runAwayToY ) ){
                actions.add(new WolffAction(false, -1, runAwayToX - 1000, runAwayToY));
            }
            actions.add(new WolffAction(false, -1, runAwayToX, runAwayToY));
        }
        return actions;
    }

    private void applyAction(WolffAction action){
        appliedAction = action;
        for(Enemy e : enemies.values()) e.move();
        if(!action.shoot) wolff.move(action.targetX, action.targetY);
        for(Enemy e : enemies.values()){
            wolff.isDead = wolff.getDistanceFrom(e) <= 2000;
            if(wolff.isDead){
                parent.backPropagate( this, -1 );
                return;
            }
        }
        if(action.shoot){
            shotsFired++;
            damageCaused += enemies.get(action.targetId).shotAt();
            enemies.remove(markedForRemovalEnemy);
        }
        markedForRemovalEnemy = -1;
        for(Object[] i : markedForRemovalDatapoints)
            if(enemies.containsKey((Integer)i[1]))
                dataPoints.remove((DataPoint)i[0]);
        parent.backPropagate( this, calcScore() );
    }
    
    public void backPropagate(GameState childState, int childScore){
        if( bestChild == null || score < childScore ){
            bestChild = childState;
            score = childScore;
            if(parent != null && parent.score < score)
                parent.backPropagate(this, score);
        }
        if((childScore == -1 || childScore == 0) && childStates.size()>1)
            childStates.remove(childState);
    }

    public boolean isGameOver(){
        return dataPoints.isEmpty() || enemies.isEmpty() || wolff.isDead || score == -1 || score == 0;
    }

    public int calcScore(){
        if(wolff.isDead){
            score = -1;
            return score;
        }
        if(score == -100){
            score = dataPoints.size() * 100 + enemysKilled * 10 + damageCaused - 3*shotsFired;
            if( enemies.isEmpty() || dataPoints.isEmpty() )
                score = dataPoints.size() * 100 + enemysKilled * 10 + 3*dataPoints.size()*(Math.max( 0, totalEnemyLife-3*shotsFired ));
        }
        return score;
    }

    @Override
    public String toString(){
        String output;
        output = "Gamestate:"+id+"\n"
                +wolff.toString()+"\n"
                +enemies.values().stream().map( e -> e.toString()+"\n" ).reduce( "\n", String::concat )
                +dataPoints.keySet().stream().map( d -> d.toString()+"\n" ).reduce( "\n", String::concat );
        return output;
    }
    
    @Override
    public boolean equals(Object o){
        if( o instanceof GameState)
            return ((GameState) o).id == id;
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.id;
        return hash;
    }
}

class Brain{
    private GameState gameState;
    public static int TIME_LIMIT;
    public static int POSITION_LIMIT;
    private int turn;
    public static long START_TIME;
    private static int POSITIONS_EVALUATED;


    public Brain(){
        gameState = new GameState();
        turn = 0;
    }
    
    public void setWolffCoord(int x, int y){
        gameState.wolff.x = x;
        gameState.wolff.y = y;
    }

    public void addeEnemy(int id, int x, int y, int life){
        gameState.addToTotalEnemyLife(life);
        gameState.addEnemy(id, x, y, life);
    }

    public void addDataPoint(int id, int x, int y){
        gameState.addDataPoint(id, x, y);
    }
    
    private int expand(List<GameState> gameStateQueue, int depth, int turn){
        GameState gameStateIter;
        while(!gameStateQueue.isEmpty()){
            POSITIONS_EVALUATED++;
            gameStateIter = gameStateQueue.remove(0);
            if(System.currentTimeMillis() >= START_TIME + TIME_LIMIT || POSITIONS_EVALUATED > POSITION_LIMIT)
                return depth;
            depth = gameStateIter.depth - turn;
            gameStateQueue.addAll(gameStateIter.getChildStates());
        }
        return depth;
    }

    public String getBestMove(){
        String command = "";
        START_TIME = System.currentTimeMillis();
        List<GameState> gameStateQueue = new LinkedList<>();
        gameStateQueue.add(gameState);
        POSITIONS_EVALUATED = 0;
        int depth = expand( gameStateQueue, 0, turn);
        gameStateQueue.clear();
        int bestScore = gameState.calcScore();
        WolffAction bestMove = gameState.getBestChild().getAppliedAction();
        command += (bestMove.shoot
                ? "SHOOT " + bestMove.targetId
                : "MOVE " + bestMove.targetX + " " + bestMove.targetY)
                + " Score: " + bestScore + " Depth:" + depth + " Time:" + (System.currentTimeMillis() - START_TIME) + " Pos:" + POSITIONS_EVALUATED;
        gameState = gameState.getBestChild();
        gameState.parent = null;
        turn++;
        return command;
    }
}

class Player {

    public static void main(String args[]) {
        Scanner in;
        try{
            Brain.TIME_LIMIT = 5000;
            Brain.POSITION_LIMIT = 20000;
            GameState.EVALED_ENEMIES = 20;
            in = new Scanner(new File("32-Extreme"));
        }catch(Exception e){
            Brain.TIME_LIMIT = 50;
            Brain.POSITION_LIMIT = 8000;
            GameState.EVALED_ENEMIES = 4;
            in = new Scanner(System.in);
        }
        Brain brain = new Brain();
        boolean firstTurn = true;
        String command = "";
        while (true) {
            if(firstTurn){
                int x = in.nextInt();
                int y = in.nextInt();
                brain.setWolffCoord(x, y);
                int dataCount = in.nextInt();
                for (int i = 0; i < dataCount; i++) {
                    int dataId = in.nextInt();
                    int dataX = in.nextInt();
                    int dataY = in.nextInt();
                    brain.addDataPoint(dataId, dataX, dataY);
                }
                int enemyCount = in.nextInt();
                for (int i = 0; i < enemyCount; i++) {
                    int enemyId = in.nextInt();
                    int enemyX = in.nextInt();
                    int enemyY = in.nextInt();
                    int enemyLife = in.nextInt();
                    brain.addeEnemy(enemyId, enemyX, enemyY, enemyLife);
                }
                firstTurn = false;
            }
            command = brain.getBestMove();
            System.out.println(command);
        }
    }
}
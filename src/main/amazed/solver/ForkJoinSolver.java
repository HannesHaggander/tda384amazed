package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.*;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver
{
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visitedNodes
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visitedNodes nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute(){
        return parallelDepthFirstSearch();
    }

    int playerID;
    int currentPosition;
    int remoteStart;
    boolean forked = false;

    public volatile Set<ForkJoinSolver> activePlayers = null;
    //public volatile Set<Integer> visitedList = null;

    private synchronized List<Integer> parallelDepthFirstSearch() {
        init();
        int startPos = forked ? remoteStart : start;

//        print(String.format("Start pos: %s | Active Players: %s | Visited size: %s",
//                startPos,
//                activePlayers.size(),
//                visitedList.size()
//        ));

        // start new player
        playerID = maze.newPlayer(startPos);
        frontier.push(startPos);

        while(!frontier.isEmpty()){
            currentPosition = frontier.pop();

            if(maze.hasGoal(currentPosition)){
                print(String.format("Found goal: Start: %s | end: %s", start, currentPosition));
                maze.move(playerID, currentPosition);
                return pathFromTo(start, currentPosition);
            }

            maze.move(playerID, currentPosition);
            Set<Integer> neighbours = maze.neighbors(currentPosition);

            for(int n : neighbours){
                if(visited.add(n)){
                    ForkJoinSolver tmpSolver = new ForkJoinSolver(maze, this.forkAfter);
                    activePlayers.add(tmpSolver);
                    predecessor.put(n, currentPosition);
                    visited.add(n);
                    tmpSolver.remoteStart = n;
                    tmpSolver.forked = true;
                    tmpSolver.predecessor = this.predecessor;
                    tmpSolver.visited = this.visited;
                }
            }

            for(ForkJoinSolver tmp : activePlayers){
                //print("Forked");
                tmp.fork();
            }
            for(ForkJoinSolver tmp : activePlayers){
                try{
                    List<Integer> path = tmp.join();
                    if(path != null){ return path; }
                }
                catch (Exception ex){ err("Error: " + ex.getLocalizedMessage(), ex); }
            }
        }

        return null;
    }

    private void init(){
        if(activePlayers == null){ activePlayers = new HashSet<>(); }
        if(!(visited instanceof ConcurrentSkipListSet) && visited.isEmpty()){
            print("Visited is now concurrent skip set");
            visited = new ConcurrentSkipListSet<>();
        }
    }

    private void delay(final long aDelay){
        try { Thread.sleep(aDelay); }
        catch (Exception ex){}
    }

    /**
     * Lazy print function
     * @param aText
     */
    private void print(final String aText){
        System.out.println(aText);
    }

    private void err(final String aText, Exception ex){ System.err.println(aText + "\n" + ex.toString());}
}
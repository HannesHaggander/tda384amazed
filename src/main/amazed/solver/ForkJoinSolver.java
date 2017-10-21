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
    int moveTo = -1;
    int startPos;

    public static ForkJoinPool sForkPool = null;
    public static ConcurrentSkipListSet<Integer> visitedList = null;

    private List<Integer> parallelDepthFirstSearch() {
        startPos = moveTo > 0 ? moveTo : start;
        if(maze.hasGoal(start)){
            List<Integer> tmpList = new ArrayList<>();
            tmpList.add(start);
            return tmpList;
        }

        if(getVisited().contains(startPos)){ return null; }

        playerID = maze.newPlayer(startPos);
        addToVisited(startPos);
        if(frontier.isEmpty()){ frontier.push(startPos); }

        while(!frontier.isEmpty()){
            currentPosition = frontier.pop();
            if(maze.hasGoal(currentPosition)){
                //maze.move(playerID, currentPosition);
                return pathFromTo(start, currentPosition);
            }

            Set<Integer> tmpNeighbour = maze.neighbors(currentPosition);
            for(int neighbour : tmpNeighbour){
                List<ForkJoinSolver> tmpFork = new ArrayList<>();
                if(!getVisited().contains(neighbour)){
                    frontier.push(neighbour);
                    addToVisited(neighbour);
                    predecessor.put(neighbour, currentPosition);
                    ForkJoinSolver tmpSolver = new ForkJoinSolver(maze);
                    tmpSolver.moveTo = neighbour;
                    tmpFork.add(tmpSolver);
                    addFork(tmpSolver.fork());
                }
                for(ForkJoinSolver fork : tmpFork){
                    try {
                        List<Integer> tmpResult = fork.join();
                        if(tmpResult != null){
                            return tmpResult;
                        }
                    }
                    catch (Exception ex){
                        continue;
                    }
                }
            }
        }

        return null;
    }

    private static ConcurrentSkipListSet<Integer> getVisited(){
        if(visitedList == null){ visitedList = new ConcurrentSkipListSet<>(); }
        return visitedList;
    }

    public static boolean addToVisited(int node){
        getVisited();
        if(!visitedList.contains(node)){
            visitedList.add(node);
            return true;
        }
        return false;
    }

    private void addFork(ForkJoinTask<List<Integer>> aFork){
        if(sForkPool == null){ sForkPool = new ForkJoinPool(); }
        sForkPool.submit(aFork);
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

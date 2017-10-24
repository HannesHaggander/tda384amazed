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

    protected int playerID;
    protected int currentPosition;
    protected int remoteStart;
    protected boolean forked = false;
    protected int steps = 0;
    protected boolean forkWhenAvailable = false;

    private Set<ForkJoinSolver> activePlayers = new HashSet<>();

    private synchronized List<Integer> parallelDepthFirstSearch() {
        init();
        // if forked, then start on another location rather than moving there
        int startPos = forked ? remoteStart : start;

        visited.add(startPos);
        // start new player
        playerID = maze.newPlayer(startPos);
        frontier.push(startPos);

        while(!frontier.isEmpty()){
            currentPosition = frontier.pop();

            // check if the current node is the goal node
            if(maze.hasGoal(currentPosition)){
                maze.move(playerID, currentPosition);
                return pathFromTo(start, currentPosition);
            }

            // get all available neighbours to the current node (includes visited ones)
            Set<Integer> neighbours = maze.neighbors(currentPosition);

            // Extract all the visited nodes from the current neighbour set
            // if the visited list does not contain the visited node then add it to
            // the list and add it to predecessor
            Set<Integer> nonVisited = new HashSet<>();
            for(int n : neighbours){
                if(visited.add(n)){
                    predecessor.put(n, currentPosition);
                    nonVisited.add(n);
                }
            }

            // only allow forking after a certain amount of steps
            // we only need to fork when there are multiple neighbours
            steps += 1;
            if(forkAfter > 0 && steps % (forkAfter) == 0){ forkWhenAvailable = true; }

            for(int n : nonVisited){
                if(nonVisited.isEmpty()){ break; }
                // only fork when there are multiple neighbours
                if(nonVisited.size() > 1 && forkWhenAvailable) {
                    ForkJoinSolver tmpSolver = new ForkJoinSolver(maze, this.forkAfter);
                    activePlayers.add(tmpSolver);
                    tmpSolver.remoteStart = n;
                    tmpSolver.forked = true;
                    tmpSolver.predecessor = this.predecessor;
                    tmpSolver.visited = this.visited;
                    tmpSolver.steps = this.steps;
                }
                else {
                    // if there only is one neighbour then move to it rather than forking
                    maze.move(playerID, n);
                    frontier.push(n);
                }
            }

            // if some threads have been spawned, then reset the fork when available value
            if(!activePlayers.isEmpty()){ forkWhenAvailable = false; }

            // fork all instances
            for(ForkJoinSolver tmp : activePlayers){
                tmp.fork();
            }

            //join up all instances in hopes that one of the children found a path to the goal
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

    /**
     * Make the visited list a concurrent safe list if its not.
     * Also make the predecessor concurrent safe if its not.
     */
    private void init(){
        if(!(visited instanceof ConcurrentSkipListSet) && visited.isEmpty()){
            visited = new ConcurrentSkipListSet<>();
        }
        if(!(predecessor instanceof ConcurrentSkipListMap) && predecessor.isEmpty()){
            predecessor = new ConcurrentSkipListMap<>();
        }
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
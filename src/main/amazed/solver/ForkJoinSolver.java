package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

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
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
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

    ForkJoinPool mForkPool = new ForkJoinPool();
    private List<Integer> parallelDepthFirstSearch() {

        int _spawnedPlayer = maze.newPlayer(this.start);
        frontier.push(start);
        int currentPos = -1;

        while(!frontier.isEmpty()){
            currentPos = frontier.pop();

            if(maze.hasGoal(currentPos)){
                print(">Found goal");
                maze.move(_spawnedPlayer, currentPos);
                join();
                return pathFromTo(start, currentPos);
            }

            if(visited.contains(currentPos)){
                print("!Already visited: " + currentPos);
                //awaitTermination();
                continue;
            }

            Set<Integer> tmpNeighbours = maze.neighbors(currentPos);

            if(tmpNeighbours.isEmpty()){
                print("No neighbours to: " + tmpNeighbours);
                join();
                awaitTermination();
            }

            if(tmpNeighbours.size() > 2){
                print("more than two neighbours");
                for(int neighbour : tmpNeighbours){
                    print("\t> All neighbour: " + neighbour);
                    frontier.push(neighbour);
                    if(!visited.contains(neighbour)){ predecessor.put(neighbour, currentPos); }
                    visited.add(neighbour);
                    mForkPool.submit(fork());
                }
            }
            else {
                print("only one neighbour, move to neighbour");
                maze.move(_spawnedPlayer, currentPos);
                if(!visited.contains(currentPos)){ predecessor.put(tmpNeighbours.iterator().next(), currentPos); }
                visited.add(currentPos);

            }
        }

        return null;
    }

    private void awaitTermination(){
        try{ mForkPool.awaitTermination(60 , TimeUnit.SECONDS); }
        catch (Exception ex){ err("Failed to wait termination", ex); }
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

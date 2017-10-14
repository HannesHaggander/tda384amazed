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
                maze.move(_spawnedPlayer, currentPos);
                join();
                mForkPool.shutdown();
                print("found goal!");
                return pathFromTo(start, currentPos);
            }

            if(!visited.contains(currentPos)){
                visited.add(currentPos);
                maze.move(_spawnedPlayer, currentPos);
                Set<Integer> tmpNeighbours = maze.neighbors(currentPos);

                if(tmpNeighbours.isEmpty()){
                    print("no neighbours");
                    awaitTermination();
                }
                if(tmpNeighbours.size() > 1){
                    //more than one neighbours
                    for(int neighbour : tmpNeighbours){
                        if(visited.contains(neighbour)){
                            continue;
                        }
                        frontier.add(neighbour);
                        predecessor.put(neighbour, currentPos);
                        mForkPool.submit(fork());
                    }
                }
                else {
                    //only one neighbour
                    int onlyNeighbour = tmpNeighbours.iterator().next();
                    if(!visited.contains(onlyNeighbour)){
                        frontier.add(onlyNeighbour);
                        predecessor.put(onlyNeighbour, currentPos);
                        maze.move(_spawnedPlayer, onlyNeighbour);
                    }
                }
            }
        }

        awaitTermination();
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

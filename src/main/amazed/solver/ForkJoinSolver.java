package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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
    public ForkJoinSolver mForkJoinSolver;
    public static ForkJoinPool mForkJoinPool;
    public ForkJoinTask<List<Integer>> mFork;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        if(mForkJoinPool == null){ mForkJoinPool = new ForkJoinPool(); }
        mForkJoinSolver = this;
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

    private List<Integer> parallelDepthFirstSearch() {
        // one player active on the maze at start
        int player = maze.newPlayer(start);
        // start with start node
        frontier.push(start);
        // as long as not all nodes have been processed
        while (!frontier.empty()) {
            // get the new node to process
            int current = frontier.pop();
            // if current node has a goal
            if (maze.hasGoal(current)) {
                // move player to goal
                maze.move(player, current);
                mFork.join();
                mForkJoinPool.shutdown();
                // search finished: reconstruct and return path
                return pathFromTo(start, current);
            }
            // if current node has not been visited yet
            if (!visited.contains(current)){
                // mark node as visited
                visited.add(current);
                // for every node nb adjacent to current
                Set<Integer> tmpNeighbours = maze.neighbors(current);
                if(tmpNeighbours.isEmpty()){
                    print("no possible neighbour");
                }
                else {
                    for (int nb: tmpNeighbours) {
                        // add nb to the nodes to be processed
                        frontier.push(nb);
                        // if nb has not been already visited,
                        // nb can be reached from current (i.e., current is nb's predecessor)
                        if (!visited.contains(nb))
                            predecessor.put(nb, current);
                        if (tmpNeighbours.size() == 1) {
                            print("move player to " + current);
                            maze.move(player, current);
                        } else {
                            delayDebug(250);
                            print("fork player - start at: " + nb);
                            ForkJoinSolver tmpInstance = newFork(nb);
                            mFork = tmpInstance.fork();
                            mForkJoinPool.submit(mFork);
                        }
                    }
                }
            }
        }
        while(mForkJoinPool.getRunningThreadCount() > 0){
            delayDebug(100);
        }
        return null;
    }

    private void delayDebug(int delayMS){
        try{
            Thread.sleep(delayMS);
        }
        catch (Exception ex){

        }
    }

    private ForkJoinSolver newFork(int startAt){
        ForkJoinSolver tmpForkJoinSolver = new ForkJoinSolver(maze);
        tmpForkJoinSolver.predecessor = this.predecessor;
        tmpForkJoinSolver.visited = this.visited;
        tmpForkJoinSolver.start = startAt;
        return tmpForkJoinSolver;
    }

    /**
     * Lazy print function
     * @param aText
     */
    private void print(final String aText){
        System.out.println(aText);
    }
}

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

    public static List<ForkJoinTask<List<Integer>>> allForks = null;
    private List<Integer> parallelDepthFirstSearch() {
        if(allForks == null){ allForks = new ArrayList<>(); }
        int _spawnedPlayer = maze.newPlayer(this.start);
        frontier.push(start);
        if(!visited.contains(start)){ visited.add(start); }
        int currentPos;

        //print(String.format("Spawning new player at {0} at pos {1}", _spawnedPlayer, start));

        while(!frontier.isEmpty()){
            currentPos = frontier.pop();

            // found goal
            if(maze.hasGoal(currentPos)){
                visited.add(currentPos);
                maze.move(_spawnedPlayer, currentPos);
                ForkJoinPool.commonPool().shutdown();
                print("Found goal!");
                return pathFromTo(start, currentPos);
            }

            Set<Integer> currentNeighbours = maze.neighbors(currentPos);
            if(currentNeighbours.isEmpty()){
                continue;
            }
            else if(currentNeighbours.size() == 1) {
                int neighbourNode = currentNeighbours.iterator().next();
                if(visited.contains(neighbourNode)){ continue; }
                visited.add(currentPos);
                predecessor.put(neighbourNode, currentPos);
                maze.move(_spawnedPlayer, currentPos);
                continue;
            }
            else {
                //print("Multiple neighbours: " + currentNeighbours.size());
                Set<Integer> nonVisit = new HashSet<>();
                for(int neighbour : currentNeighbours){
                    if(visited.contains(neighbour)){ continue; }
                    frontier.push(neighbour);
                    visited.add(neighbour);
                    predecessor.put(neighbour, currentPos);
                    nonVisit.add(neighbour);
                }

                for (int noVisit : nonVisit){
                    makeNewPlayer(noVisit);
                }

            }
        }
        join();
        return null;
    }

    private void makeNewPlayer(int pos){
        try { Thread.sleep(1000); }
        catch (Exception ex){}

        print("Make new player");
        ForkJoinSolver tmpSolver = new ForkJoinSolver(maze);
        tmpSolver.predecessor = this.predecessor;
        tmpSolver.visited = this.visited;
        tmpSolver.frontier = this.frontier;
        tmpSolver.start = pos;
        ForkJoinTask<List<Integer>> tmpFork = tmpSolver.fork();
        if(!allForks.contains(tmpFork)){
            allForks.add(tmpFork);
            ForkJoinPool.commonPool().invoke(tmpFork);
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

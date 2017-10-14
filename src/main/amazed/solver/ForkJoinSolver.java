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
        print("parallelDepthFirstSearch");
        int _spawnedPlayer = maze.newPlayer(this.start);
        frontier.push(start);
        int currentPos = -1;

        print(String.format("Spawning player {0} at pos {1}" +
                "\tVisited Nodes: {2}" +
                "\tPred nodes: {3}",
                _spawnedPlayer,
                start,
                visited.size(),
                predecessor.size()
        ));

        while(!frontier.isEmpty()){
            currentPos = frontier.pop();
            if(maze.hasGoal(currentPos)){
                maze.move(_spawnedPlayer, currentPos);
                return pathFromTo(start, currentPos);
            }

            for (int nbr : maze.neighbors(currentPos)){
                if(visited.contains(nbr)){ continue; }
                frontier.push(nbr);
                visited.add(nbr);
                predecessor.put(nbr, currentPos);
                maze.move(_spawnedPlayer, nbr);
                mForkPool.submit(fork());
            }
        }

        return null;
    }

    /**
     *
     * @param aPlayerID
     * @param aCurrentPosition
     * @return
     */
    private boolean isCurrentGoal(int aPlayerID, int aCurrentPosition){
        if(maze.hasGoal(aCurrentPosition)){
            maze.move(aPlayerID, aCurrentPosition);
            mForkPool.shutdown();
            return true;
        }
        return false;
    }

    /**
     *
     * @param playerId
     * @param aCurrentPosition
     */
    private void checkNeighbours(int playerId, int aCurrentPosition){
        if(visited.contains(aCurrentPosition)){
            print("Already contains: " + aCurrentPosition);
            awaitTermination();
            return;
        }

        Set<Integer> tmpNeighbours = maze.neighbors(aCurrentPosition);
        if(tmpNeighbours.isEmpty()){
            print("No neighbours");
            return;
        }
        else if(tmpNeighbours.size() > 1){ handleMultipleNeighbours(tmpNeighbours, aCurrentPosition); }
        else { handleSingleNeighbour(playerId, tmpNeighbours.iterator().next(), aCurrentPosition); }
    }

    /**
     *
     * @param aNeighbours
     * @param aCurrentPosition
     */
    private void handleMultipleNeighbours(Set<Integer> aNeighbours, int aCurrentPosition){
        print("More than one neighbour");
        Set<Integer> notVisited = new HashSet<>();
        for(int nbr : aNeighbours){
            if(!visited.contains(nbr)){
                frontier.push(nbr);
                notVisited.add(nbr);
            }
        }
        for(int nbr : notVisited){
            start = nbr;
            predecessor.put(nbr, aCurrentPosition);
            mForkPool.submit(fork());
        }
    }

    private void handleSingleNeighbour(int aPlayerId, int aNeighbour, int aCurrent){
        print("only one neighbour");
        predecessor.put(aNeighbour, aCurrent);
        maze.move(aPlayerId, aNeighbour);
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

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

    public int spawnOn = -1;
    List<SequentialSolver> forks = new LinkedList<>();
    int _spawnedPlayer;
    private List<Integer> parallelDepthFirstSearch() {
        _spawnedPlayer = maze.newPlayer(spawnOn < 0 ? spawnOn : this.start);
        frontier.push(start);
        if(!visited.contains(start)){ visited.add(start); }
        int currentPos;

        while(!frontier.isEmpty()){
            currentPos = frontier.pop();
            // found goal
            if(maze.hasGoal(currentPos)){
                visited.add(currentPos);
                maze.move(_spawnedPlayer, currentPos);
                print("Goal is: " + currentPos + " start was: " + start);
                StringBuilder sb = new StringBuilder();
                sb.append("Found goal! Path: ");
                sb.append("[Length " + predecessor.keySet().size() + "] ");
                sb.append("\n\tFrom end: ");
                int goalcurrent = currentPos;
                while(predecessor.get(goalcurrent) != null){
                    goalcurrent = predecessor.get(goalcurrent);
                    sb.append(goalcurrent + " -> ");
                }

                sb.append("\n\tFrom start: ");
                goalcurrent = start;
                while(predecessor.get(goalcurrent) != null){
                    goalcurrent = predecessor.get(goalcurrent);
                    sb.append(goalcurrent + " -> ");
                }
                sb.append(String.format("\nList contains start: %s | contains end: %s",
                        predecessor.get(start) != null,
                        predecessor.get(currentPos) != null
                ));
                print(sb.toString());

                return pathFromTo(start, currentPos);
            }

            Set<Integer> currentNeighbours = maze.neighbors(currentPos);
            Set<Integer> nonVisited = new ConcurrentSkipListSet<>();

            for(int neighbour : currentNeighbours){
                if(visited.contains(neighbour)){ continue; }
                visited.add(neighbour);
                frontier.push(neighbour);
                nonVisited.add(neighbour);
            }

            forks.clear();
            for (int noVisit : nonVisited){
                ForkJoinSolver tmpSolver = new ForkJoinSolver(maze);
                tmpSolver.spawnOn = noVisit;
                tmpSolver.visited = this.visited;
                tmpSolver.predecessor = this.predecessor;
                tmpSolver.predecessor.put(noVisit, currentPos);
                tmpSolver.frontier = this.frontier;
                forks.add(tmpSolver);
                tmpSolver.fork();
            }

            for(SequentialSolver solver : forks){
                List<Integer> tmpPath = null;
                try{ tmpPath = solver.join(); }
                catch (Exception ex){ System.err.println("Failed to get join: " + ex.getLocalizedMessage()); }

                for(int tmpVisited : solver.visited){
                    if(!this.visited.contains(tmpVisited)){
                        print(String.format("added %s to visited", tmpVisited));
                        this.visited.add(tmpVisited);
                    }
                }

                if(tmpPath != null){
                    return tmpPath;
                }
            }
        }

        return null;
    }

    private boolean isValidPath(int tmpCurrent)
    {
        List<Integer> path = new LinkedList<>();
        Integer current = tmpCurrent;
        while (current != null) {
            path.add(current);
            current = this.predecessor.get(current);
        }
        Collections.reverse(path);

        if (path.isEmpty()){
            print("path is empty");
            return false;
        }
        ListIterator<Integer> iter = path.listIterator();
        int prev = 0, curr = iter.next();
        if (curr != this.start){
            print("current is start pos");
            return false;
        }
        while (iter.hasNext()) {
            prev = curr;
            curr = iter.next();
            if (!maze.neighbors(prev).contains(curr)){
                print("neighbours does to previous does not contain current");
                return false;
            }
        }
        print("is current goal? " + maze.hasGoal(curr));
        return maze.hasGoal(curr);
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

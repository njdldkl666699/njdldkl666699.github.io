package io.njdldkl.algorithm;

import io.njdldkl.State;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class IterativeDeepeningDFS extends Algorithm {

    private Set<State> visited;

    public IterativeDeepeningDFS(State start, State goal) {
        super(start, goal);
    }

    public IterativeDeepeningDFS(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    @Override
    public Result solve() {
        visited = new HashSet<>();
        int maxDepth = 0;
        while (true) {
            boolean found = dfs(start, 0, maxDepth);
            if (found) {
                return new Result(maxDepth, true);
            }
            maxDepth++;
        }
    }

    private boolean dfs(State state, int depth, int maxDepth) {
        if (depth > maxDepth) {
            return false;
        }
        if (state.equals(goal)) {
            return true;
        }

        visited.add(state);
        if (callback != null) {
            callback.accept(state);
        }
        for (State successor : state.getSuccessors()) {
            if (!visited.contains(successor)) {
                if (dfs(successor, depth + 1, maxDepth)) {
                    return true;
                }
            }
        }

        visited.remove(state);
        return false;
    }
}

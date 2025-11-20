package io.njdldkl.algorithm;

import io.njdldkl.State;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class DFSLimitDepth extends Algorithm {

    private Set<State> visited;

    private int step;

    public DFSLimitDepth(State start, State goal) {
        super(start, goal);
    }

    public DFSLimitDepth(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    @Setter
    private int depthLimit = 32;

    @Override
    public Result solve() {
        step = 0;
        visited = new HashSet<>();
        boolean found = dfs(start, 0);
        return new Result(step, found);
    }

    private boolean dfs(State state, int depth) {
        if (depth >= depthLimit) {
            return false;
        }
        if (state.equals(goal)) {
            step = depth;
            return true;
        }

        visited.add(state);
        if (callback != null) {
            callback.accept(state);
        }
        for (State successor : state.getSuccessors()) {
            if (!visited.contains(successor)) {
                if (dfs(successor, depth + 1)) {
                    return true;
                }
            }
        }

        visited.remove(state);
        return false;
    }
}

package io.njdldkl.algorithm;

import io.njdldkl.State;

import java.util.*;
import java.util.function.Consumer;

public class BFS extends Algorithm {

    public BFS(State start, State goal) {
        super(start, goal);
    }

    public BFS(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    @Override
    public Result solve() {
        var visited = new HashMap<State, Integer>();
        Queue<State> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            if (current.equals(goal)) {
                return new Result(visited.getOrDefault(current, 0), true);
            }

            visited.put(current, visited.getOrDefault(current, 0));
            if (callback != null) {
                callback.accept(current);
            }

            for (State successor : current.getSuccessors()) {
                if (!visited.containsKey(successor)) {
                    visited.put(successor, visited.get(current) + 1);
                    queue.add(successor);
                }
            }
        }

        return new Result(0, false);
    }
}

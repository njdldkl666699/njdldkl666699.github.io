package io.njdldkl.algorithm;

import io.njdldkl.State;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public class DFS extends Algorithm {

    public DFS(State start, State goal) {
        super(start, goal);
    }

    public DFS(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    // 模拟栈，否则会栈溢出
    @Override
    public Result solve() {
        Set<State> visited = new HashSet<>();
        Stack<State> stack = new Stack<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            State current = stack.pop();
            if (current.equals(goal)) {
                return new Result(stack.size(), true);
            }

            if (!visited.contains(current)) {
                visited.add(current);
                if (callback != null) {
                    callback.accept(current);
                }

                for (State successor : current.getSuccessors()) {
                    if (!visited.contains(successor)) {
                        stack.push(successor);
                    }
                }
            }
        }

        return new Result(0, false);
    }

}

package io.njdldkl.algorithm;

import io.njdldkl.State;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class BiDirectionalBFS extends Algorithm {

    public BiDirectionalBFS(State start, State goal) {
        super(start, goal);
    }

    public BiDirectionalBFS(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    @Override
    public Result solve() {
        var startVisited = new HashMap<State, Integer>();
        var goalVisited = new HashMap<State, Integer>();
        Queue<State> startQueue = new LinkedList<>();
        Queue<State> goalQueue = new LinkedList<>();

        startVisited.put(start, 0);
        startQueue.add(start);

        goalVisited.put(goal, 0);
        goalQueue.add(goal);

        while (!startQueue.isEmpty() && !goalQueue.isEmpty()) {
            State currentFromStart = startQueue.poll();
            int distFromStart = startVisited.get(currentFromStart);
            if (callback != null) {
                callback.accept(currentFromStart);
            }
            for (State successor : currentFromStart.getSuccessors()) {
                if (startVisited.containsKey(successor)) {
                    continue;
                }
                startVisited.put(successor, distFromStart + 1);
                // 如果对方已经访问过，说明在 successor 相遇
                if (goalVisited.containsKey(successor)) {
                    int totalSteps = startVisited.get(successor) + goalVisited.get(successor);
                    return new Result(totalSteps, true);
                }
                startQueue.add(successor);
            }

            // 从 goal 方向扩展一步
            State currentFromGoal = goalQueue.poll();
            int distFromGoal = goalVisited.get(currentFromGoal);
            if (callback != null) {
                callback.accept(currentFromGoal);
            }
            for (State successor : currentFromGoal.getSuccessors()) {
                if (goalVisited.containsKey(successor)) {
                    continue;
                }
                goalVisited.put(successor, distFromGoal + 1);
                if (startVisited.containsKey(successor)) {
                    int totalSteps = startVisited.get(successor) + goalVisited.get(successor);
                    return new Result(totalSteps, true);
                }
                goalQueue.add(successor);
            }
        }

        return new Result(0, false);
    }
}

package io.njdldkl.algorithm;

import io.njdldkl.State;

import java.util.*;
import java.util.function.Consumer;

public class BiDirectionalDFS extends Algorithm {

    public BiDirectionalDFS(State start, State goal) {
        super(start, goal);
    }

    public BiDirectionalDFS(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    @Override
    public Result solve() {
        Stack<State> startStack = new Stack<>();
        Set<State> startVisited = new HashSet<>();
        Map<State, State> startParent = new HashMap<>();

        Stack<State> goalStack = new Stack<>();
        Set<State> goalVisited = new HashSet<>();
        Map<State, State> goalParent = new HashMap<>();

        startStack.push(start);
        startParent.put(start, null);

        goalStack.push(goal);
        goalParent.put(goal, null);

        while (!startStack.isEmpty() && !goalStack.isEmpty()) {
            // 从 start 方向扩展一步
            State currentFromStart = startStack.pop();
            if (!startVisited.contains(currentFromStart)) {
                startVisited.add(currentFromStart);
                if (callback != null) {
                    callback.accept(currentFromStart);
                }
                // 当前节点已经被 goal 方向访问，交汇
                if (goalVisited.contains(currentFromStart)) {
                    int pathLen = reconstructPathLength(
                            currentFromStart,
                            startParent,
                            goalParent
                    );
                    return new Result(pathLen, true);
                }
                for (State successor : currentFromStart.getSuccessors()) {
                    if (startVisited.contains(successor)) {
                        continue;
                    }
                    // 记录父指针
                    if (!startParent.containsKey(successor)) {
                        startParent.put(successor, currentFromStart);
                    }
                    if (goalVisited.contains(successor)) {
                        int pathLen = reconstructPathLength(
                                successor,
                                startParent,
                                goalParent
                        );
                        return new Result(pathLen, true);
                    }
                    startStack.push(successor);
                }
            }

            // 从 goal 方向扩展一步
            State currentFromGoal = goalStack.pop();
            if (!goalVisited.contains(currentFromGoal)) {
                goalVisited.add(currentFromGoal);
                if (callback != null) {
                    callback.accept(currentFromGoal);
                }
                if (startVisited.contains(currentFromGoal)) {
                    int pathLen = reconstructPathLength(
                            currentFromGoal,
                            startParent,
                            goalParent
                    );
                    return new Result(pathLen, true);
                }
                for (State successor : currentFromGoal.getSuccessors()) {
                    if (goalVisited.contains(successor)) {
                        continue;
                    }
                    if (!goalParent.containsKey(successor)) {
                        goalParent.put(successor, currentFromGoal);
                    }
                    if (startVisited.contains(successor)) {
                        int pathLen = reconstructPathLength(
                                successor,
                                startParent,
                                goalParent
                        );
                        return new Result(pathLen, true);
                    }
                    goalStack.push(successor);
                }
            }
        }

        // 没有路径，长度可以约定为 0 或 -1，这里用 -1 表示
        return new Result(-1, false);
    }

    /**
     * 根据交汇点 `meet`，以及 start/goal 两边的父指针，计算整条路径长度。
     * 路径长度定义为边数：start -> ... -> meet -> ... -> goal。
     */
    private int reconstructPathLength(State meet,
                                      Map<State, State> startParent,
                                      Map<State, State> goalParent) {
        // 从 meet 回溯到 start，计数（包含 meet，不包含 start 前面的 null）
        int lenStartSide = 0;
        State cur = meet;
        while (cur != null) {
            State parent = startParent.get(cur);
            if (parent == null) {
                break;
            }
            lenStartSide++;
            cur = parent;
        }

        // 从 meet 回溯到 goal，计数（同样按边数算）
        int lenGoalSide = 0;
        cur = meet;
        while (cur != null) {
            State parent = goalParent.get(cur);
            if (parent == null) {
                break;
            }
            lenGoalSide++;
            cur = parent;
        }

        // 两边边数相加，就是总路径长度
        return lenStartSide + lenGoalSide;
    }
}

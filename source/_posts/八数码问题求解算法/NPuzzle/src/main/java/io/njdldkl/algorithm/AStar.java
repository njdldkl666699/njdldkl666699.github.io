package io.njdldkl.algorithm;

import io.njdldkl.State;
import lombok.Setter;

import java.util.*;
import java.util.function.Consumer;

public class AStar extends Algorithm {

    public AStar(State start, State goal) {
        super(start, goal);
    }

    public AStar(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    /**
     * 启发式函数的接口
     */
    @FunctionalInterface
    public interface Heuristic {
        int calculate(State s1, State s2);
    }

    /**
     * Dijkstra启发式函数实现，g(n) = 0
     */
    public static final Heuristic DIJKSTRA = (s1, s2) -> 0;

    /**
     * 曼哈顿距离启发式函数实现
     */
    public static final Heuristic MANHATTAN = (s1, s2) -> {
        var board1 = s1.board();
        var board2 = s2.board();
        int n = board1.length;

        int distance = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                byte value1 = board1[i][j];
                State.Point2D pos2 = s2.getNumberPosition(value1);
                if (pos2 == null) {
                    continue;
                }
                distance += Math.abs(i - pos2.row()) + Math.abs(j - pos2.col());
            }
        }
        return distance;
    };

    /**
     * 错位数字数量启发式函数实现
     */
    public static final Heuristic MISPLACED_TILES = (s1, s2) -> {
        var board1 = s1.board();
        var board2 = s2.board();
        int n = board1.length;

        int misplaced = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board1[i][j] != board2[i][j]) {
                    misplaced++;
                }
            }
        }
        return misplaced;
    };

    /**
     * 从起始状态到当前状态的实际代价
     */
    protected Map<State, Integer> gScore;

    /**
     * 比较两个状态的f值
     */
    protected int compare(State s1, State s2) {
        int f1 = gScore.getOrDefault(s1, Integer.MAX_VALUE) + heuristic.calculate(s1, goal);
        int f2 = gScore.getOrDefault(s2, Integer.MAX_VALUE) + heuristic.calculate(s2, goal);
        return Integer.compare(f1, f2);
    }

    /**
     * 启发式函数，默认为Dijkstra
     */
    @Setter
    protected Heuristic heuristic = DIJKSTRA;

    /**
     * 每一步的代价，默认为1
     */
    @Setter
    protected int stepCost = 1;

    @Override
    public Result solve() {
        gScore = new HashMap<>();
        var frontier = new PriorityQueue<>(this::compare);
        var closed = new HashSet<State>();
        frontier.add(start);
        gScore.put(start, 0);

        while (!frontier.isEmpty()) {
            State current = frontier.poll();

            if (current.equals(goal)) {
                return new Result(gScore.get(current), true);
            }

            closed.add(current);
            if (callback != null) {
                callback.accept(current);
            }

            for (State successor : current.getSuccessors()) {
                if (closed.contains(successor)) {
                    continue;
                }

                int newCost = gScore.get(current) + stepCost;
                if (!gScore.containsKey(successor) || newCost < gScore.get(successor)) {
                    gScore.put(successor, newCost);
                    frontier.add(successor);
                }
            }
        }

        return new Result(0, false);
    }
}

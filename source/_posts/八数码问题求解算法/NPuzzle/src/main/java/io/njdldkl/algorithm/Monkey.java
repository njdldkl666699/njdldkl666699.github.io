package io.njdldkl.algorithm;

import io.njdldkl.State;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * 猴子算法
 */
public class Monkey extends Algorithm {

    public Monkey(State start, State goal) {
        super(start, goal);
    }

    public Monkey(State start, State goal, Consumer<State> callback) {
        super(start, goal, callback);
    }

    /**
     * 愚蠢的猴子总是能找到一条路，但可能需要很长时间；<br>
     * 聪明的猴子会记住已经访问过的状态，避免重复访问，但是可能找不到路
     */
    @Setter
    private boolean smartClever = false;

    @Override
    public Result solve() {
        var stack = new Stack<State>();
        var visited = smartClever ? new HashSet<State>() : null;
        var random = new Random();
        int step = 0;
        stack.push(start);
        while (!stack.isEmpty()) {
            var current = stack.pop();
            if (current.equals(goal)) {
                return new Result(step, true);
            }

            if (visited != null) {
                visited.add(current);
            }
            if (callback != null) {
                callback.accept(current);
            }

            var successors = current.getSuccessors();
            if (visited == null) {
                // 猴子随机选择一个后继状态
                var successor = successors[random.nextInt(successors.length)];
                stack.push(successor);
                ++step;
            } else {
                // 聪明的猴子只在未访问过的后继状态中随机选择一个
                var unvisited = new ArrayList<State>(4);
                for (var successor : successors) {
                    if (!visited.contains(successor)) {
                        unvisited.add(successor);
                    }
                }
                if (!unvisited.isEmpty()) {
                    var successor = unvisited.get(random.nextInt(unvisited.size()));
                    stack.push(successor);
                    ++step;
                }
            }
        }
        return new Result(step, false);
    }
}

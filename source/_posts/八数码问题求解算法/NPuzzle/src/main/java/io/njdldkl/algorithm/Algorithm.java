package io.njdldkl.algorithm;

import io.njdldkl.State;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * 八数码问题求解算法抽象类<br>
 * 子类需实现solve方法，且自身额外的域必须对每次solve调用重新初始化
 */
@Getter
@Setter
public abstract class Algorithm {

    /**
     * 构造函数
     *
     * @param start 起始状态
     * @param goal  目标状态
     */
    public Algorithm(State start, State goal) {
        this.start = start;
        this.goal = goal;
    }

    /**
     * 构造函数
     *
     * @param start    起始状态
     * @param goal     目标状态
     * @param callback 每移动一步调用的回调函数
     */
    public Algorithm(State start, State goal, Consumer<State> callback) {
        this.start = start;
        this.goal = goal;
        this.callback = callback;
    }

    protected State start;

    protected State goal;

    protected Consumer<State> callback;

    public record Result(int steps, boolean solved) {
    }

    /**
     * 解决八数码问题，返回最短移动步数
     */
    public abstract Result solve();

}

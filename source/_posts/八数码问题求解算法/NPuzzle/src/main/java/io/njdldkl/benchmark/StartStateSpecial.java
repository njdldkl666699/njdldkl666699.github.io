package io.njdldkl.benchmark;

import io.njdldkl.State;
import lombok.Getter;

/**
 * 预定义的特殊起始状态枚举
 */
public enum StartStateSpecial {

    WORST(new State(new byte[][]{
            {8, 6, 7},
            {2, 5, 4},
            {3, 0, 1}
    })),

    MIDDLE(new State(new byte[][]{
            {1, 2, 3},
            {0, 5, 7},
            {8, 6, 4}
    })),

    EASY(new State(new byte[][]{
            {1, 2, 3},
            {4, 5, 6},
            {0, 7, 8}
    })),

    EASIEST(new State(new byte[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 0}
    }));

    StartStateSpecial(State state) {
        this.state = state;
    }

    @Getter
    private final State state;
}

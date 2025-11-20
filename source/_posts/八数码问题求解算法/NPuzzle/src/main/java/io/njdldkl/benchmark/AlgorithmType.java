package io.njdldkl.benchmark;

import io.njdldkl.algorithm.*;
import lombok.Getter;

/**
 * 八数码问题求解算法枚举
 */
public enum AlgorithmType {

    DFS(DFS.class),
    DFSLimitDepth(DFSLimitDepth.class),
    IterativeDeepeningDFS(IterativeDeepeningDFS.class),
    BiDirectionalDFS(BiDirectionalDFS.class),
    BFS(BFS.class),
    BiDirectionalBFS(BiDirectionalBFS.class),
    AStarDijkstra(AStar.class),
    AStarManhattan(AStar.class),
    AStarMisplacedTiles(AStar.class),
    StupidSillyMonkey(Monkey.class),
    SmartCleverMonkey(Monkey.class);

    AlgorithmType(Class<? extends Algorithm> algorithmClass) {
        this.algorithmClass = algorithmClass;
    }

    @Getter
    private final Class<? extends Algorithm> algorithmClass;

    public void configureAlgorithm(Algorithm algorithm) {
        switch (this) {
            case AStarManhattan -> {
                if (algorithm instanceof AStar aStar) {
                    aStar.setHeuristic(AStar.MANHATTAN);
                }
            }
            case AStarMisplacedTiles -> {
                if (algorithm instanceof AStar aStar) {
                    aStar.setHeuristic(AStar.MISPLACED_TILES);
                }
            }
            case SmartCleverMonkey -> {
                if (algorithm instanceof Monkey monkey) {
                    monkey.setSmartClever(true);
                }
            }
        }
    }

}

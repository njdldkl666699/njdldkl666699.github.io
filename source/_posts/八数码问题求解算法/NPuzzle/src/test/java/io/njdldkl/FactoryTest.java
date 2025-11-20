package io.njdldkl;

import io.njdldkl.algorithm.*;
import io.njdldkl.benchmark.StartStateSpecial;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FactoryTest {

    public static final State goal = new State(new byte[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 0}
    });

    private int runCount = 0;

    private final Consumer<State> callback = state -> runCount++;

    private final AlgorithmFactory factory = new AlgorithmFactory(StartStateSpecial.MIDDLE.getState(), goal, callback);

    public final Arguments[] algorithms = new Arguments[]{
            Arguments.of(factory.createAlgorithm(DFS.class), "DFS"),
            Arguments.of(factory.createAlgorithm(DFSLimitDepth.class), "DFSLimitDepth"),
            Arguments.of(factory.createAlgorithm(IterativeDeepeningDFS.class), "IterativeDeepeningDFS"),
            Arguments.of(factory.createAlgorithm(BiDirectionalDFS.class), "BiDirectionalDFS"),
            Arguments.of(factory.createAlgorithm(BFS.class), "BFS"),
            Arguments.of(factory.createAlgorithm(BiDirectionalBFS.class), "BiDirectionalBFS"),
            Arguments.of(factory.createAlgorithm(AStar.class), "A*: Dijkstra")
    };

    @ParameterizedTest(name = "{1} Test")
    @FieldSource("algorithms")
    public void parameterizedFactoryTest(Algorithm algorithm, String name) {
        runCount = 0;
        var result = algorithm.solve();
        log.info("{} 探索了 {} 个状态。", name, runCount);
        if (!result.solved()) {
            log.info("{} 未能找到解决方案。", name);
            return;
        }
        log.info("{} 找到的解决方案步数为: {}", name, result.steps());
    }

    @Test
    public void dfsLimitDepthTest() {
        var dfsLimitDepth = factory.createAlgorithm(DFSLimitDepth.class);
        // 这里可以设置深度限制，以测试不同的限制对算法性能的影响
        dfsLimitDepth.setDepthLimit(64);
        var result = dfsLimitDepth.solve();
        log.info("DFSLimitDepth 探索了 {} 个状态。", runCount);
        if (!result.solved()) {
            log.info("DFSLimitDepth 未能找到解决方案。");
            return;
        }
        log.info("DFSLimitDepth 找到的解决方案步数为: {}", result.steps());
    }

    @Test
    public void aStarManhattanTest() {
        var manhattan = factory.createAlgorithm(AStar.class);
        manhattan.setHeuristic(AStar.MANHATTAN);
        var result = manhattan.solve();
        log.info("A*: Manhattan 探索了 {} 个状态。", runCount);
        if (!result.solved()) {
            log.info("A*: Manhattan 未能找到解决方案。");
            return;
        }
        log.info("A*: Manhattan 找到的解决方案步数为: {}", result.steps());
    }

    @Test
    public void aStarMisplacedTilesTest() {
        var misplacedTiles = factory.createAlgorithm(AStar.class);
        misplacedTiles.setHeuristic(AStar.MISPLACED_TILES);
        var result = misplacedTiles.solve();
        log.info("A*: Misplaced Tiles 探索了 {} 个状态。", runCount);
        if (!result.solved()) {
            log.info("A*: Misplaced Tiles 未能找到解决方案。");
            return;
        }
        log.info("A*: Misplaced Tiles 找到的解决方案步数为: {}", result.steps());
    }

    @Test
    public void stupidSillyMonkeyTest() {
        var stupidSillyMonkey = factory.createAlgorithm(Monkey.class);
        var result = stupidSillyMonkey.solve();
        log.info("Stupid Silly Monkey 探索了 {} 个状态。", runCount);
        if (!result.solved()) {
            log.info("Stupid Silly Monkey 未能找到解决方案。");
            return;
        }
        log.info("Stupid Silly Monkey 找到的解决方案步数为: {}", result.steps());
    }

    @Test
    public void smartCleverMonkeyTest() {
        var smartCleverMonkey = factory.createAlgorithm(Monkey.class);
        smartCleverMonkey.setSmartClever(true);
        var result = smartCleverMonkey.solve();
        log.info("Smart Clever Monkey 探索了 {} 个状态。", runCount);
        if (!result.solved()) {
            log.info("Smart Clever Monkey 未能找到解决方案。");
            return;
        }
        log.info("Smart Clever Monkey 找到的解决方案步数为: {}", result.steps());
    }

    @Test
    public void generateMiddleStateTest() {
        State current = goal;
        State next = goal;
        State prev = null;
        int steps = 15;
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < steps; i++) {
            var successors = current.getSuccessors();
            // 避免走回头路
            do {
                next = successors[random.nextInt(successors.length)];
            } while (next.equals(prev));
            prev = current;
            current = next;
        }
        log.info("从目标状态随机游走 {} 步得到的中间状态为：{}", steps, current);
    }
}

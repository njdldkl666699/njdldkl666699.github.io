package io.njdldkl.benchmark;

import io.njdldkl.AlgorithmFactory;
import io.njdldkl.State;
import io.njdldkl.algorithm.Algorithm;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2)
@Fork(1)
public class EightPuzzleBenchMark {

    private static final State goal = new State(new byte[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 0}
    });

    private static final AlgorithmFactory factory = AlgorithmFactory.builder()
            .goal(goal)
            .build();

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @AuxCounters(AuxCounters.Type.EVENTS)
    public static class BenchmarkState {

        private Algorithm algorithm;

        public long searchCount;

        private final Consumer<State> searchStateCounter = state -> searchCount++;

        @Param
        private StartStateSpecial startStateSpecial;
        @Param
        private AlgorithmType algorithmType;

        @Setup(Level.Trial)
        public void setupTrial() {
            if (StartStateSpecial.WORST == startStateSpecial
                    && AlgorithmType.IterativeDeepeningDFS == algorithmType) {
                // IterativeDeepeningDFS在WORST状态下需要非常长的时间才能完成测试，跳过该组合
                throw new IllegalStateException("经过初期测试，IterativeDeepeningDFS在WORST状态下需要非常长的时间才能完成测试，跳过该组合");
            }
            State start = startStateSpecial.getState();
            algorithm = factory.createAlgorithm(algorithmType.getAlgorithmClass(), start);
            algorithmType.configureAlgorithm(algorithm);
            algorithm.setCallback(searchStateCounter);
        }

        @Setup(Level.Invocation)
        public void setupInvocation() {
            searchCount = 0;
        }
    }

   @Benchmark
   @Measurement(iterations = 3)
   @Timeout(time = 1, timeUnit = TimeUnit.MINUTES)
   public void benchmarkAlgorithm(BenchmarkState state, Blackhole blackhole) {
       var result = state.algorithm.solve();
       // 防止死代码消除，并避免日志IO影响基准测试结果
       blackhole.consume(result);
   }

    /**
     * 基于随机状态的基准测试状态类
     */
    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @AuxCounters(AuxCounters.Type.EVENTS)
    public static class BenchmarkStateRandom {

        private State start;

        private Algorithm algorithm;

        /**
         * <p>每次Iteration的平均搜索状态数</p>
         * 每次Iteration会调用多次Invocation，因此需要统计总搜索状态数和调用次数，最后计算平均值<br>
         */
        public double searchCount;
        private long searchCountTotal;
        private long invocationCount;

        private final Consumer<State> searchStateCounter = state -> searchCountTotal++;

        @Param
        private AlgorithmType algorithmType;

        @Setup(Level.Trial)
        public void setupTrial() {
            algorithm = factory.createAlgorithm(algorithmType.getAlgorithmClass());
            algorithmType.configureAlgorithm(algorithm);
            algorithm.setCallback(searchStateCounter);

            // 随机游走32步，生成一个随机状态作为起始状态
            State current = goal;
            var random = ThreadLocalRandom.current();
            for (int i = 0; i < 32; i++) {
                var successors = current.getSuccessors();
                current = successors[random.nextInt(successors.length)];
            }
            start = current;
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            searchCount = 0;
            searchCountTotal = 0;
            invocationCount = 0;
        }

        /**
         * 在每次调用前生成一个随机状态并初始化算法实例<br>
         * 不得不使用Level.Invocation，因为每次调用前都需要重新生成状态<br>
         * 经过初期测试，随机状态下测试一般超过1ms，使用Invocation级别的Setup不会对结果产生太大影响
         */
        @Setup(Level.Invocation)
        public void setupInvocation() {
            start = nextRandomState();
            algorithm.setStart(start);
            invocationCount++;
        }

        /**
         * 从起始状态出发，随机寻找一个后继状态作为新的起始状态<br>
         * 使用这样简单的方法生成随机状态，是为了减少生成状态所需的时间开销<br>
         *
         * @return 随机后继状态
         */
        private State nextRandomState() {
            var random = ThreadLocalRandom.current();
            var successors = start.getSuccessors();
            return successors[random.nextInt(successors.length)];
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {
            searchCount = invocationCount == 0 ? 0 : (double) searchCountTotal / invocationCount;
        }
    }

    @Benchmark
    @Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.MINUTES)
    public void benchmarkAlgorithmRandom(BenchmarkStateRandom state, Blackhole blackhole) {
        var result = state.algorithm.solve();
        blackhole.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(EightPuzzleBenchMark.class.getSimpleName())
                .result("benchmark-results.json")
                .resultFormat(ResultFormatType.JSON)
                .addProfiler(GCProfiler.class)  // 添加GC分析器
                .build();

        new Runner(options).run();
    }

}


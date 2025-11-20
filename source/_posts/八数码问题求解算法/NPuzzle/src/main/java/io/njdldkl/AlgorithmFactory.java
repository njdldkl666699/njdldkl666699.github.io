package io.njdldkl;

import io.njdldkl.algorithm.Algorithm;
import lombok.Builder;
import lombok.SneakyThrows;

import java.util.function.Consumer;

@Builder
public record AlgorithmFactory(State start, State goal, Consumer<State> callback) {

    @SneakyThrows
    public <T extends Algorithm> T createAlgorithm(Class<T> algorithmClass, State start) {
        T algorithm;
        var constructor = algorithmClass.getConstructor(State.class, State.class, Consumer.class);
        algorithm = constructor.newInstance(start, goal, callback);
        return algorithm;
    }

    public <T extends Algorithm> T createAlgorithm(Class<T> algorithmClass) {
        return createAlgorithm(algorithmClass, start);
    }
}

package jsettlers.algorithms.simplebehaviortree;

import java.io.Serializable;

import java8.util.function.Predicate;

@FunctionalInterface
public interface IBooleanConditionFunction<T> extends Predicate<T>, Serializable {}

package jsettlers.algorithms.simplebehaviortree;

import java.io.Serializable;

import java8.util.function.Function;

@FunctionalInterface
public interface IIntSupplier<T> extends Function<T, Integer>, Serializable {}
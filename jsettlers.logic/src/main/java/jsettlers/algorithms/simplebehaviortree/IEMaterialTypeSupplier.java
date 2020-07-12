package jsettlers.algorithms.simplebehaviortree;

import java.io.Serializable;

import java8.util.function.Function;
import jsettlers.common.material.EMaterialType;

@FunctionalInterface
public interface IEMaterialTypeSupplier<T> extends Function<T, EMaterialType>, Serializable {}

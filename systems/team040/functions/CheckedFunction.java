package systems.team040.functions;

@FunctionalInterface
public interface CheckedFunction<T, R, E extends  Exception> {
    R apply(T t) throws E;
}

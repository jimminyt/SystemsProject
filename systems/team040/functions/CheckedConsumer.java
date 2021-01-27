package systems.team040.functions;

/**
 * Created this and the other 'checked' interface to allow writing lambdas that throw exceptions
 *
 * (it's very cool woah)
 */
@FunctionalInterface
public interface CheckedConsumer<T, E extends  Exception> {
    void accept(T t) throws E;
}

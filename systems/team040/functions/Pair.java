package systems.team040.functions;

/**
 * Created this just to help with parameters for one function because java doesn't have tuples
 */
public class Pair<F, S> {
    public final F first;
    public final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }
}

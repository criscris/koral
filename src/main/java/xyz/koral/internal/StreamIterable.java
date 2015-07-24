package xyz.koral.internal;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface StreamIterable<T> extends Iterable<T>
{
    default Stream<T> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }
    
    default Stream<T> parallelStream() 
    {
        return StreamSupport.stream(spliterator(), true);
    }
}

package io.smallrye.reactive.converters.rxjava2;

import io.reactivex.Observable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.FromRSPublisherTCK;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ObservableFromRSPublisherTest extends FromRSPublisherTCK<Observable> {

    private ReactiveTypeConverter<Observable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Observable.class)
                .orElseThrow(() -> new AssertionError("Observable converter should be found"));
    }

    @Override
    protected boolean supportNullValues() {
        return false;
    }

    @Override
    protected boolean emitSingleValue() {
        return true;
    }

    @Override
    protected boolean emitMultipleValues() {
        return true;
    }

    @Override
    protected ReactiveTypeConverter<Observable> converter() {
        return converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getOne(Observable instance) {
        Observable<String> single = instance.cast(String.class);
        return single.blockingLast();
    }

    @Override
    protected Exception getFailure(Observable instance) {
        AtomicReference<Exception> reference = new AtomicReference<>();
        try {
            instance.blockingLast();
        } catch (Exception e) {
            reference.set(e);
        }
        return reference.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getAll(Observable instance) {
        return ((Observable<String>) instance)
                .toList().blockingGet();
    }

    @Override
    protected void consume(Observable instance) {
        instance.blockingIterable().forEach(x -> {
        });
    }
}
package io.smallrye.reactive.converters.rxjava2;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import io.smallrye.reactive.converters.tck.ToRSPublisherTCK;
import org.junit.Before;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ObservableToRSPublisherTest extends ToRSPublisherTCK<Observable> {


    private static final int DELAY = 10;
    private ReactiveTypeConverter<Observable> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Observable.class)
                .orElseThrow(() -> new AssertionError("Observable converter should be found"));
    }

    @Override
    protected Optional<Observable> createInstanceEmittingASingleValueImmediately(String value) {
        return Optional.of(Observable.just(value));
    }

    @Override
    protected Optional<Observable> createInstanceEmittingASingleValueAsynchronously(String value) {
        return Optional.of(
                Observable.just(value).delay(DELAY, TimeUnit.MILLISECONDS).observeOn(Schedulers.io())
        );
    }

    @Override
    protected Observable createInstanceFailingImmediately(RuntimeException e) {
        return Observable.error(e);
    }

    @Override
    protected Observable createInstanceFailingAsynchronously(RuntimeException e) {
        return Observable.just("X")
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .map(x -> {
                    throw e;
                })
                .observeOn(Schedulers.io());
    }

    @Override
    protected Optional<Observable> createInstanceEmittingANullValueImmediately() {
        return Optional.of(Observable.just("X").map(s -> null));
    }

    @Override
    protected Optional<Observable> createInstanceEmittingANullValueAsynchronously() {
        return Optional.of(
                Observable.just("X")
                        .delay(DELAY, TimeUnit.MILLISECONDS)
                        .observeOn(Schedulers.io())
                        .map(s -> null)
        );
    }

    @Override
    protected Optional<Observable> createInstanceEmittingMultipleValues(String... values) {
        return Optional.of(Observable.fromArray(values));
    }

    @Override
    protected Optional<Observable> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2,
                                                                                   RuntimeException e) {
        Observable<String> stream = Observable.create(emitter -> {
            emitter.onNext(v1);
            emitter.onNext(v2);
            emitter.onError(e);
        });
        return Optional.of(stream);
    }

    @Override
    protected Optional<Observable> createInstanceCompletingImmediately() {
        return empty();
    }

    @Override
    protected Optional<Observable> createInstanceCompletingAsynchronously() {
        return Optional.of(Observable.just("X")
                .delay(10, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .flatMap(x -> Observable.empty())
        );
    }

    @Override
    protected Optional<Observable> never() {
        return Optional.of(Observable.never());
    }

    @Override
    protected Optional<Observable> empty() {
        return Optional.of(Observable.empty());
    }

    @Override
    protected ReactiveTypeConverter<Observable> converter() {
        return converter;
    }
}

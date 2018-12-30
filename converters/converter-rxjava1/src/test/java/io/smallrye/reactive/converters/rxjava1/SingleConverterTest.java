package io.smallrye.reactive.converters.rxjava1;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Single;

import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SingleConverterTest {

    private static final String ONE_VALUE = "hello";
    private static final String EXCEPTION_EXPECTED = "Exception expected";
    private ReactiveTypeConverter<Single> converter;

    @Before
    public void lookup() {
        converter = Registry.lookup(Single.class)
                .orElseThrow(() -> new AssertionError("Single converter should be found"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromPublisherEmittingOneImmediateValue() {
        Single<?> single = converter.fromPublisher(Flowable.just(ONE_VALUE));
        String o = single
                .cast(String.class)
                .toBlocking().value();
        assertThat(o).isEqualTo(ONE_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromPublisherEmittingOneDelayedValue() {
        Single<?> single = converter.fromPublisher(Flowable.just(ONE_VALUE).delay(10, TimeUnit.MILLISECONDS));
        String o = single
                .cast(String.class)
                .toBlocking().value();
        assertThat(o).isEqualTo(ONE_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BoomException.class)
    public void testFromPublisherEmittingAnImmediateFailure() {
        Single<?> single = converter.fromPublisher(Flowable.error(new BoomException("BOOM")));
        //noinspection ResultOfMethodCallIgnored
        single.cast(String.class).toBlocking().value();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = BoomException.class)
    public void testFromPublisherEmittingAnDelayedFailure() {
        Single<?> single = converter.fromPublisher(Flowable.just(ONE_VALUE)
                .delay(10, TimeUnit.MILLISECONDS))
                .map(x -> {
                    throw new BoomException("BOOM");
                });
        //noinspection ResultOfMethodCallIgnored
        single.cast(String.class).toBlocking().value();
    }

    @Test(expected = NoSuchElementException.class)
    public void testFromEmptyPublisher() {
        Single<?> single = converter.fromPublisher(Flowable.empty());
        //noinspection ResultOfMethodCallIgnored
        single.toBlocking().value();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromPublisherEmittingImmediateMultipleValue() {
        Single<?> single = converter.fromPublisher(Flowable.just("h", "e", "l", "l", "o"));
        String o = single
                .cast(String.class)
                .toBlocking().value();
        assertThat(o).isEqualTo("h");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromPublisherEmittingDelayedMultipleValue() {
        Single<?> single = converter.fromPublisher(Flowable.just("h", "e", "l", "l", "o")
                .observeOn(Schedulers.computation())
        );
        String o = single
                .cast(String.class)
                .toBlocking().value();
        assertThat(o).isEqualTo("h");
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void testFromPublisherEmittingANullValueImmediately() {
        Single<String> single = converter.fromPublisher(Flowable.just(null));
        single.toBlocking().value();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void testFromPublisherEmittingADelayedNullValue() {
        Single<String> single = converter.fromPublisher(Flowable.just(ONE_VALUE).delay(10, TimeUnit.MILLISECONDS)
                .map(x -> null)
        );
        single.toBlocking().value();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromPublisherThatIsNeverEmitting() throws InterruptedException {
        Single<?> single = converter.fromPublisher(Flowable.never());

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            //noinspection ResultOfMethodCallIgnored
            single.toBlocking().value();
            latch.countDown();
        }).start();
        assertThat(latch.await(10, TimeUnit.MILLISECONDS)).isFalse();
    }


}
package io.smallrye.reactive.converters.tck;

import io.reactivex.Flowable;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class ToRSPublisherTCK<T> {

    protected abstract Optional<T> createInstanceEmittingASingleValueImmediately(String value);

    protected abstract Optional<T> createInstanceEmittingASingleValueAsynchronously(String value);

    protected abstract T createInstanceFailingImmediately(RuntimeException e);

    protected abstract T createInstanceFailingAsynchronously(RuntimeException e);

    protected abstract Optional<T> createInstanceEmittingANullValueImmediately();

    protected abstract Optional<T> createInstanceEmittingANullValueAsynchronously();

    protected abstract Optional<T> createInstanceEmittingMultipleValues(String... values);

    protected abstract Optional<T> createInstanceEmittingAMultipleValuesAndFailure(String v1, String v2, RuntimeException e);

    protected abstract Optional<T> createInstanceCompletingImmediately();

    protected abstract Optional<T> createInstanceCompletingAsynchronously();

    protected abstract Optional<T> never();

    protected abstract Optional<T> empty();

    protected abstract ReactiveTypeConverter<T> converter();

    @Test
    public void testWithImmediateValue() {
        String uuid = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingASingleValueImmediately(uuid);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        String res = Flowable.fromPublisher(publisher).blockingFirst();
        assertThat(res).isEqualTo(uuid);
    }

    @Test
    public void testWithAsynchronousValue() {
        String uuid = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingASingleValueAsynchronously(uuid);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        String res = Flowable.fromPublisher(publisher).blockingFirst();
        assertThat(res).isEqualTo(uuid);
    }

    @Test
    public void testWithImmediateFailure() {
        T instance = createInstanceFailingImmediately(new BoomException());
        Publisher<String> publisher = converter().toRSPublisher(instance);
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(BoomException.class);
        }
    }

    @Test
    public void testWithAsynchronousFailure() {
        T instance = createInstanceFailingAsynchronously(new BoomException());
        Publisher<String> publisher = converter().toRSPublisher(instance);
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(BoomException.class);
        }
    }

    @Test
    public void testWithImmediateNullValue() {
        Optional<T> optional = createInstanceEmittingANullValueImmediately();
        if (!optional.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(optional.get());
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("NullPointerException expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testWithImmediateNullValueInAStream() {
        if (! converter().supportNullValue()) {
            return;
        }
        Optional<T> optional = createInstanceEmittingMultipleValues("a", "b", null, "c");
        if (!optional.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(optional.get());
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("NullPointerException expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }


    @Test
    public void testWithAsynchronousNullValue() {
        Optional<T> optional = createInstanceEmittingANullValueAsynchronously();
        if (!optional.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(optional.get());
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("NullPointerException expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testWithSeveralValues() {
        String uuid = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        String uuid3 = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingMultipleValues(uuid, uuid2, uuid3);
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        List<String> list = Flowable.fromPublisher(publisher).toList().blockingGet();
        assertThat(list).containsExactly(uuid, uuid2, uuid3);
    }

    @Test
    public void testtSeveralValuesAndAFailure() {
        String uuid = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        Optional<T> instance = createInstanceEmittingAMultipleValuesAndFailure(uuid, uuid2, new BoomException());
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        try {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            fail("Boom exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(BoomException.class);
        }
    }


    @Test
    public void testWithNever() throws InterruptedException {
        Optional<T> instance = never();
        if (!instance.isPresent()) {
            // Test ignored.
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            Flowable.fromPublisher(publisher).blockingSubscribe();
            latch.countDown();
        });

        boolean terminated = latch.await(10, TimeUnit.MILLISECONDS);
        future.cancel(false);
        assertThat(terminated).isFalse();
    }

    @Test
    public void testWithEmpty() {
        Optional<T> instance = empty();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        assertThat(Flowable.fromPublisher(publisher).isEmpty().blockingGet()).isTrue();
    }

    @Test
    public void testStreamCompletingImmediately() {
        Optional<T> instance = createInstanceCompletingImmediately();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        assertThat(Flowable.fromPublisher(publisher).isEmpty().blockingGet()).isTrue();
    }

    @Test
    public void testStreamCompletingAsynchronously() {
        Optional<T> instance = createInstanceCompletingAsynchronously();
        if (!instance.isPresent()) {
            // Test ignored
            return;
        }
        Publisher<String> publisher = converter().toRSPublisher(instance.get());
        assertThat(Flowable.fromPublisher(publisher).isEmpty().blockingGet()).isTrue();
    }


}

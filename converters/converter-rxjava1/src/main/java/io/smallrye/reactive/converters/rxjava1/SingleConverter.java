package io.smallrye.reactive.converters.rxjava1;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import org.reactivestreams.Publisher;
import rx.Single;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Converter handling the RX Java 1 {@link Single} type.
 *
 * <p>
 * <h4>toCompletionStage</h4>
 * The {@link #toCompletionStage(Single)} method returns a {@link CompletionStage} instance completed or failed
 * according to the single emission. If the passed {@code Single} emits {@code null}, the returned
 * {@link CompletionStage} is redeemed with {@code null}.
 * </p>
 * <p>
 * <h4>fromCompletionStage</h4>
 * The {@link #fromCompletionStage(CompletionStage)} method returns a {@link Single} instance completed or failed
 * according to the passed {@link CompletionStage} completion. If the future emits a {@code null} value, the
 * {@link Single} emits {@code null}.
 * </p>
 * <p>
 * <h4>fromPublisher</h4>
 * The {@link #fromPublisher(Publisher)} method returns a {@link Single} emitting the first value of the stream. If the
 * passed {@link Publisher} is empty, the returned {@link Single} fails. If the passed stream emits more than one value,
 * only the first one is used, the other values are discarded.
 * </p>
 * <p>
 * <h4>toRSPublisher</h4>
 * The {@link #toRSPublisher(Single)} method returns a stream emitting a single value followed by the completion signal.
 * If the passed {@link Single} fails, the returned stream also fails.
 * </p>
 */
public class SingleConverter implements ReactiveTypeConverter<Single> {

    @SuppressWarnings("unchecked")
    @Override
    public <X> CompletionStage<X> toCompletionStage(Single instance) {
        CompletableFuture<X> future = new CompletableFuture<>();
        Single<X> s = Objects.requireNonNull(instance);
        //noinspection ResultOfMethodCallIgnored
        s.subscribe(
                future::complete,
                future::completeExceptionally
        );
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> Publisher<X> toRSPublisher(Single instance) {
        return RxJavaInterop.toV2Flowable(instance.toObservable());
    }

    @Override
    public <X> Single<X> fromCompletionStage(CompletionStage<X> cs) {
        CompletionStage<X> future = Objects.requireNonNull(cs);
        return Single
                .create(emitter ->
                        future.<X>whenComplete((X res, Throwable err) -> {
                            if (!emitter.isUnsubscribed()) {
                                if (err != null) {
                                    if (err instanceof CompletionException) {
                                        emitter.onError(err.getCause());
                                    } else {
                                        emitter.onError(err);
                                    }
                                } else {
                                    emitter.onSuccess(res);
                                }
                            }
                        }));
    }

    @Override
    public <X> Single fromPublisher(Publisher<X> publisher) {
        return RxJavaInterop.toV1Observable(publisher).first().toSingle();
    }

    @Override
    public Class<Single> type() {
        return Single.class;
    }
}

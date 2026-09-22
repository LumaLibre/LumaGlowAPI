package dev.lumas.glowapi.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PackPublicationRegression {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void await(CountDownLatch latch) {
        try {
            require(latch.await(5, TimeUnit.SECONDS), "Publication regression timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    public static void run() throws Exception {
        Path file = Files.createTempFile("lumaglow-publication-", ".zip");
        try {
            PackPublication publication = new PackPublication();
            int older = publication.next();
            int current = publication.next();
            require(publication.publish(current, () -> Files.writeString(file, "current pack")),
                    "The current build was not published");
            require(!publication.publish(older, () -> Files.writeString(file, "stale pack")),
                    "A stale build was published");
            require(Files.readString(file).equals("current pack"), "A stale build overwrote the current pack");

            CountDownLatch writing = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch stopping = new CountDownLatch(1);
            AtomicBoolean invalidated = new AtomicBoolean();
            try (var executor = Executors.newFixedThreadPool(2)) {
                var writer = executor.submit(() -> publication.publish(current, () -> {
                    writing.countDown();
                    await(release);
                    require(!invalidated.get(), "Reload invalidated an in-progress publication");
                    Files.writeString(file, "completed before reload");
                }));
                await(writing);
                var stop = executor.submit(() -> {
                    stopping.countDown();
                    publication.invalidate(() -> invalidated.set(true));
                });
                try {
                    await(stopping);
                    try {
                        stop.get(50, TimeUnit.MILLISECONDS);
                        throw new AssertionError("Reload interleaved the publication callback");
                    } catch (TimeoutException expected) {
                        require(!invalidated.get(), "Reload changed state during publication");
                    }
                } finally {
                    release.countDown();
                }
                require(writer.get(5, TimeUnit.SECONDS), "The active publication did not complete");
                stop.get(5, TimeUnit.SECONDS);
            }
            require(!publication.publish(current, () -> Files.writeString(file, "disabled pack")),
                    "A stopped build published again");
            int afterReload = publication.next();
            require(publication.publish(afterReload, () -> Files.writeString(file, "new mode")),
                    "A reloaded build failed to publish");
            require(Files.readString(file).equals("new mode"), "Reload did not replace the pack");
            System.out.println("Pack publication rejects stale writes and serializes reload invalidation with disk publication");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}

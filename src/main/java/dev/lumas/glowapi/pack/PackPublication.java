package dev.lumas.glowapi.pack;

import java.io.IOException;

final class PackPublication {
    private int generation;

    synchronized int next() {
        return ++generation;
    }

    synchronized boolean publish(int token, WriteAction action) throws IOException {
        if (token != generation) {
            return false;
        }
        action.run();
        return true;
    }

    synchronized void invalidate(Runnable cleanup) {
        generation++;
        cleanup.run();
    }

    @FunctionalInterface
    interface WriteAction {
        void run() throws IOException;
    }
}

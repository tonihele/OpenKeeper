/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Weak keys compared by identity. Unlike WeakHashMap, mutable equals/hashCode implementations do
 * not affect lookup. Values must not retain their keys. Callers provide synchronization when
 * sharing the map between threads.
 */
public final class WeakIdentityMap<K, V> {

    private final ReferenceQueue<K> queue = new ReferenceQueue<>();
    private final Map<Key<K>, V> entries = new HashMap<>();

    public V get(K key) {
        purge();
        return entries.get(new Key<>(Objects.requireNonNull(key), null));
    }

    public boolean containsKey(K key) {
        purge();
        return entries.containsKey(new Key<>(Objects.requireNonNull(key), null));
    }

    public V put(K key, V value) {
        purge();
        return entries.put(new Key<>(Objects.requireNonNull(key), queue), value);
    }

    public int size() {
        purge();
        return entries.size();
    }

    /** The action must not change this map. */
    public void forEach(BiConsumer<? super K, ? super V> action) {
        purge();
        entries.forEach((reference, value) -> {
            K key = reference.get();
            if (key != null) {
                action.accept(key, value);
            }
        });
    }

    public void clear() {
        entries.clear();
        while (queue.poll() != null) {
            // Drain references belonging to the old entries.
        }
    }

    private void purge() {
        for (var reference = queue.poll(); reference != null; reference = queue.poll()) {
            entries.remove(reference);
        }
    }

    private static final class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T value, ReferenceQueue<T> queue) {
            super(value, queue);
            hash = System.identityHashCode(value);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            T value = get();
            return value != null && other instanceof Key<?> reference && value == reference.get();
        }
    }
}

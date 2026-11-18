/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.Map;

class WeakIdentityMapTest {
    @Test
    void distinctEqualMutableKeysKeepSeparateEntries() {
        var map = new WeakIdentityMap<StringBuilder, String>();
        var a = new StringBuilder("same");
        var b = new StringBuilder("same");
        map.put(a, "a");
        map.put(b, "b");
        a.append("changed");
        assertEquals("a", map.get(a));
        assertEquals("b", map.get(b));
        assertEquals(2, map.size());
        var equal = new WeakIdentityMap<String, Integer>();
        String one = new String("equal"), two = new String("equal");
        equal.put(one, 1);
        equal.put(two, 2);
        assertEquals(1, equal.get(one));
        assertEquals(2, equal.get(two));
        assertEquals(2, equal.size());
        equal.clear();
        assertEquals(0, equal.size());
    }

    @Test
    void queuedClearedReferenceIsRemovedWithoutDependingOnGcTiming() throws Exception {
        var map = new WeakIdentityMap<Object, Boolean>();
        Object key = new Object();
        map.put(key, true);
        var field = WeakIdentityMap.class.getDeclaredField("entries");
        field.setAccessible(true);
        var entries = (Map<?, ?>) field.get(map);
        var reference = (Reference<?>) entries.keySet().iterator().next();
        reference.clear();
        reference.enqueue();
        assertEquals(0, map.size());
        assertFalse(map.containsKey(key));
    }
}

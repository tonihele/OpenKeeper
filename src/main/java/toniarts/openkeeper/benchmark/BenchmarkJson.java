/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.benchmark;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Minimal JSON serializer for benchmark results (no external dependency).
 */
public final class BenchmarkJson {

    private BenchmarkJson() {
    }

    public static String toJson(Map<String, ?> root) {
        StringBuilder sb = new StringBuilder(4096);
        writeValue(sb, root, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map<?, ?> map) {
            writeObject(sb, map, indent);
        } else if (value instanceof Collection<?> col) {
            writeArray(sb, col, indent);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map, int indent) {
        sb.append("{\n");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(",\n");
            } else {
                first = false;
            }
            indent(sb, indent + 1);
            sb.append('"').append(escape(String.valueOf(e.getKey()))).append("\": ");
            writeValue(sb, e.getValue(), indent + 1);
        }
        if (!map.isEmpty()) {
            sb.append('\n');
        }
        indent(sb, indent);
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Collection<?> col, int indent) {
        sb.append("[\n");
        Iterator<?> it = col.iterator();
        while (it.hasNext()) {
            indent(sb, indent + 1);
            writeValue(sb, it.next(), indent + 1);
            if (it.hasNext()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        indent(sb, indent);
        sb.append(']');
    }

    private static void indent(StringBuilder sb, int indent) {
        sb.append("  ".repeat(Math.max(0, indent)));
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}

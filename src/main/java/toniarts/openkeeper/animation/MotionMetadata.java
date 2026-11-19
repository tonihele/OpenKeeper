/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Optional semantic sidecar. It never overrides authored ArtResource timing. */
public record MotionMetadata(String resource, String sourceSha256, int converterVersion,
        BodyPlan bodyPlan, Kind kind, Transition transition, String layoutGroup,
        double referenceSpeed, List<Contact> contacts, boolean phaseVariation) {
    public enum BodyPlan { UNKNOWN, BIPED, MULTIPED, FLYER }
    public enum Kind { UNKNOWN, LOOP, ONE_SHOT }
    public enum Transition { CUT, AUTHORED }
    public record Contact(String anchor, double from, double to) {
        public Contact {
            if (anchor == null || !anchor.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}")
                    || !Double.isFinite(from) || !Double.isFinite(to) || from < 0 || to > 1 || from >= to)
                throw new IllegalArgumentException("Contacts require 0 <= from < to <= 1");
        }
    }
    private static final int MAX_BYTES = 16 * 1024;
    private static final Set<String> KEYS = Set.of("schema", "resource", "sourceSha256", "converterVersion",
            "bodyPlan", "kind", "transition", "layoutGroup", "referenceSpeed", "contacts", "phaseVariation");

    public MotionMetadata {
        if (resource == null || resource.isBlank() || resource.contains("/") || resource.contains("\\")
                || sourceSha256 == null || !sourceSha256.matches("[a-fA-F0-9]{64}") || converterVersion < 1
                || bodyPlan == null || kind == null || transition == null || layoutGroup == null
                || !Double.isFinite(referenceSpeed) || referenceSpeed < 0 || contacts == null || contacts.size() > 8
                || phaseVariation && kind != Kind.LOOP)
            throw new IllegalArgumentException("Invalid motion metadata");
        sourceSha256 = sourceSha256.toLowerCase(Locale.ROOT);
        contacts = List.copyOf(contacts);
    }

    /** Strict audit path; callers must retain classic playback on any validation failure. */
    public static MotionMetadata read(InputStream input, String expectedResource, String expectedHash,
                                      int expectedConverterVersion) throws IOException {
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IOException("Motion metadata exceeds 16 KiB");
        try {
            Properties p = new Properties() {
                @Override public synchronized Object put(Object key, Object value) {
                    if (containsKey(key)) throw new IllegalArgumentException("Duplicate metadata field: " + key);
                    return super.put(key, value);
                }
            };
            p.load(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
            if (!KEYS.containsAll(p.stringPropertyNames()) || !"1".equals(p.getProperty("schema")))
                throw new IllegalArgumentException("Unknown schema or fields");
            String variation = p.getProperty("phaseVariation", "false");
            if (!variation.equals("true") && !variation.equals("false")) throw new IllegalArgumentException("Invalid boolean");
            var contacts = new java.util.ArrayList<Contact>();
            String windows = p.getProperty("contacts", "");
            if (!windows.isBlank()) for (String window : windows.split(";", -1)) {
                String[] named = window.split(":", -1);
                if (named.length != 2) throw new IllegalArgumentException("Contact needs an anchor name");
                String[] ends = named[1].split(",", -1);
                if (ends.length != 2) throw new IllegalArgumentException("Invalid contact window");
                contacts.add(new Contact(named[0], Double.parseDouble(ends[0]), Double.parseDouble(ends[1])));
            }
            var result = new MotionMetadata(p.getProperty("resource"), p.getProperty("sourceSha256"),
                    Integer.parseInt(p.getProperty("converterVersion")),
                    BodyPlan.valueOf(p.getProperty("bodyPlan", "UNKNOWN")), Kind.valueOf(p.getProperty("kind", "UNKNOWN")),
                    Transition.valueOf(p.getProperty("transition", "CUT")), p.getProperty("layoutGroup", ""),
                    Double.parseDouble(p.getProperty("referenceSpeed", "0")), contacts, Boolean.parseBoolean(variation));
            if (!result.resource.equals(expectedResource) || !result.sourceSha256.equalsIgnoreCase(expectedHash)
                    || result.converterVersion != expectedConverterVersion)
                throw new IllegalArgumentException("Sidecar does not match resource/source/converter");
            return result;
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new IOException("Invalid motion metadata for " + expectedResource, error);
        }
    }
}

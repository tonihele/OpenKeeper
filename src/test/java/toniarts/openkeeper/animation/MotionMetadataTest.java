/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionMetadataTest {
    private static final String HASH = "ab".repeat(32);
    private static final String BASE = "schema=1\nresource=Walk\nsourceSha256="+HASH+"\nconverterVersion=16\n";
    private MotionMetadata read(String text) throws IOException {
        return MotionMetadata.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "Walk", HASH, 16);
    }
    @Test void defaultsAreConservativeAndSemanticAnnotationsAreImmutable() throws Exception {
        var classic = read(BASE);
        assertEquals(MotionMetadata.Kind.UNKNOWN, classic.kind());
        assertEquals(MotionMetadata.Transition.CUT, classic.transition()); assertFalse(classic.phaseVariation());
        var loop = read(BASE+"bodyPlan=BIPED\nkind=LOOP\ncontacts=left:0.1,0.3;right:0.6,0.9\nphaseVariation=true\nreferenceSpeed=0.8\n");
        assertEquals(2, loop.contacts().size()); assertEquals(.8, loop.referenceSpeed());
        assertThrows(UnsupportedOperationException.class, () -> loop.contacts().clear());
    }
    @Test void incorrectIdentityAndUnsupportedSchemasFailClosed() {
        for (String text : new String[]{BASE.replace("Walk", "Run"), BASE.replace(HASH, "cd".repeat(32)),
                BASE.replace("=16", "=15"), BASE.replace("schema=1", "schema=2"), BASE+"invented=true\n",
                BASE+"schema=1\n", BASE.replace(HASH, "xyz"), BASE+"phaseVariation=yes\n"})
            assertThrows(IOException.class, () -> read(text), text);
    }
    @Test void unsafeSemanticsAndOversizedFilesAreRejected() {
        for (String extra : new String[]{"referenceSpeed=NaN", "referenceSpeed=-1", "contacts=0.8,0.2",
                "contacts=left:0,2", "contacts=left:0,1;", "kind=ONE_SHOT\nphaseVariation=true", "bodyPlan=HUMAN"})
            assertThrows(IOException.class, () -> read(BASE+extra+"\n"), extra);
        assertThrows(IOException.class, () -> read(BASE+"#".repeat(17000)));
    }
}

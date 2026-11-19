/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VertexClipTest {
    private static final String HASH = "ab".repeat(32);
    private VertexClip clip() { return VertexClip.of(1,new float[]{1f/30,2f/30},new float[]{0,0,0,1,2,3}); }
    private byte[] encode(VertexClip clip, VertexClip.Encoding encoding) throws Exception {
        var out=new ByteArrayOutputStream(); clip.write(out,encoding,HASH,16,0); return out.toByteArray();
    }
    @Test void packedDataOwnsInputsAndRetainsSourceOriginAndEndpointDwell() {
        float[] times={1f/30,2f/30}, vertices={0,0,0,1,2,3};
        var clip=VertexClip.of(1,times,vertices); Arrays.fill(times,99); Arrays.fill(vertices,99);
        float[] out=new float[3]; clip.sample(1.0/60,out);
        assertArrayEquals(new float[]{.5f,1,1.5f},out,1e-6f);
        clip.sample(50,out); assertArrayEquals(new float[]{1,2,3},out);
        clip.sample(-1,out); assertArrayEquals(new float[]{0,0,0},out);
        out[0]=88; clip.copyFrame(0,out); assertEquals(0,out[0]);
        assertEquals(32,clip.decodedBytes());
    }
    @Test void bothEncodingsRoundTripAndRespectQuantizationErrorBetweenKeys() throws Exception {
        for (var encoding : VertexClip.Encoding.values()) {
            byte[] bytes=encode(clip(),encoding);
            var decoded=VertexClip.read(new ByteArrayInputStream(bytes),HASH,16,0,32);
            float[] a=new float[3], b=new float[3];
            for (int i=0;i<=100;i++) {
                double time=i/3000.0; clip().sample(time,a); decoded.sample(time,b);
                assertArrayEquals(a,b,encoding==VertexClip.Encoding.FLOAT32 ? 0 : 5e-5f);
            }
        }
        assertTrue(encode(clip(),VertexClip.Encoding.UNORM16).length < encode(clip(),VertexClip.Encoding.FLOAT32).length);
    }
    @Test void corruptTruncatedWrongSourceAndOverBudgetDataFailClosed() throws Exception {
        byte[] bytes=encode(clip(),VertexClip.Encoding.FLOAT32);
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),"cd".repeat(32),16,0,1024));
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),HASH,15,0,1024));
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),HASH,16,1,1024));
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),HASH,16,0,31));
        bytes[60]^=1;
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),HASH,16,0,1024));
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(Arrays.copyOf(bytes,50)),HASH,16,0,1024));
    }
    @Test void checksummedMaliciousDimensionsCannotOverflowAllocationGuards() throws Exception {
        byte[] bytes=encode(clip(),VertexClip.Encoding.FLOAT32);
        java.nio.ByteBuffer.wrap(bytes).putInt(47,Integer.MAX_VALUE).putInt(51,Integer.MAX_VALUE);
        var digest=java.security.MessageDigest.getInstance("SHA-256"); digest.update(bytes,0,bytes.length-32);
        System.arraycopy(digest.digest(),0,bytes,bytes.length-32,32);
        assertThrows(IOException.class,()->VertexClip.read(new ByteArrayInputStream(bytes),HASH,16,0,1024));
    }
    @Test void nonfiniteCoordinatesAndUnsafeInterpolationRangesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->VertexClip.of(1,new float[]{0},new float[]{Float.NaN,0,0}));
        assertThrows(IllegalArgumentException.class,()->VertexClip.of(1,new float[]{0,0},new float[6]));
        assertThrows(IllegalArgumentException.class,()->VertexClip.of(1,new float[]{0,1},new float[]{-Float.MAX_VALUE,0,0,Float.MAX_VALUE,0,0}));
        assertThrows(IllegalArgumentException.class,()->clip().sample(Double.NaN,new float[3]));
    }
}

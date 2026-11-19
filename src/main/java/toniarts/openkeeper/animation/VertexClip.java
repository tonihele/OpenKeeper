/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.*;

/** Immutable packed source-key positions; optional derived format, never a replacement for the KMF decoder. */
public final class VertexClip {
    public enum Encoding { FLOAT32, UNORM16 }
    private static final int MAGIC = 0x4f4b5641, VERSION = 1;
    private final int vertices;
    private final float[] times, positions;

    private VertexClip(int vertices, float[] times, float[] positions) {
        if (vertices < 1 || vertices > Integer.MAX_VALUE/3 || times.length < 1 || (long)vertices * 3 * times.length != positions.length)
            throw new IllegalArgumentException("Invalid clip dimensions");
        float previous = Float.NEGATIVE_INFINITY;
        for (float t : times) {
            if (!Float.isFinite(t) || t <= previous) throw new IllegalArgumentException("Invalid key time");
            previous = t;
        }
        for (float value : positions) if (!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite position");
        this.vertices = vertices; this.times = times; this.positions = positions;
        float[] bound = bounds();
        for (int i=0;i<3;i++) if ((double)bound[i+3]-bound[i] > Float.MAX_VALUE)
            throw new IllegalArgumentException("Position range overflows reference float interpolation");
    }
    public static VertexClip of(int vertices, float[] times, float[] positions) {
        return new VertexClip(vertices, times.clone(), positions.clone());
    }
    public static VertexClip bake(PoseTrack track, int maxDecodedBytes) {
        int vertices = track.getTarget().getMesh().getVertexCount(), frames = track.getTimes().length;
        long components = 3L * vertices * frames;
        if (components > Integer.MAX_VALUE || 4L * (components + frames) > maxDecodedBytes)
            throw new IllegalArgumentException("Decoded clip budget exceeded");
        float[] packed = new float[(int)components], frame = new float[vertices * 3];
        for (int i=0;i<frames;i++) { track.copyFrame(i, frame); System.arraycopy(frame,0,packed,i*frame.length,frame.length); }
        return new VertexClip(vertices,track.getTimes().clone(),packed);
    }
    public int vertexCount() { return vertices; }
    public int keyCount() { return times.length; }
    public float keyTime(int key) { return times[key]; }
    public long decodedBytes() { return 4L * (times.length + positions.length); }
    public void copyFrame(int key, float[] destination) {
        if (key < 0 || key >= times.length || destination.length != vertices*3) throw new IllegalArgumentException("Invalid frame destination");
        System.arraycopy(positions,key*destination.length,destination,0,destination.length);
    }
    /** Same source-time origin and float arithmetic as the compatibility PoseTrack. */
    public void sample(double age, float[] destination) {
        if (!Double.isFinite(age) || destination.length != vertices*3) throw new IllegalArgumentException("Invalid sample");
        float time = (float)Math.clamp(age+times[0],times[0],times[times.length-1]);
        int index = Arrays.binarySearch(times,time);
        int a = index >= 0 ? index : Math.max(0,-index-2), b = index >= 0 ? index : Math.min(times.length-1,a+1);
        float weight = a == b ? 0 : (time-times[a])/(times[b]-times[a]);
        int size = destination.length;
        for (int i=0;i<size;i++) destination[i] = positions[a*size+i]+(positions[b*size+i]-positions[a*size+i])*weight;
    }
    private float[] bounds() {
        float[] result = {Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for (int i=0;i<positions.length;i++) { int axis=i%3; result[axis]=Math.min(result[axis],positions[i]); result[axis+3]=Math.max(result[axis+3],positions[i]); }
        return result;
    }
    private static byte[] fingerprint(String hash) {
        if (hash == null || !hash.matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException("Expected SHA256 source fingerprint");
        return HexFormat.of().parseHex(hash);
    }
    private static byte[] digest(byte[] bytes, int length) {
        try { var d=MessageDigest.getInstance("SHA-256"); d.update(bytes,0,length); return d.digest(); }
        catch (NoSuchAlgorithmException error) { throw new AssertionError(error); }
    }
    public void write(OutputStream output, Encoding encoding, String sourceHash, int converterVersion, int part) throws IOException {
        if (encoding == null || converterVersion < 1 || part < 0) throw new IllegalArgumentException("Invalid encoding/version/part");
        var bytes = new ByteArrayOutputStream(); var data = new DataOutputStream(bytes);
        data.writeInt(MAGIC); data.writeShort(VERSION); data.writeByte(encoding == Encoding.FLOAT32 ? 1 : 2);
        data.writeInt(converterVersion); data.write(fingerprint(sourceHash)); data.writeInt(part); data.writeInt(vertices); data.writeInt(times.length);
        for (float time : times) data.writeFloat(time);
        float[] bound = bounds(); for (float value : bound) data.writeFloat(value);
        for (int i=0;i<positions.length;i++) {
            if (encoding == Encoding.FLOAT32) data.writeFloat(positions[i]);
            else {
                int axis=i%3; double range=(double)bound[axis+3]-bound[axis];
                data.writeShort(range == 0 ? 0 : (int)Math.round(((double)positions[i]-bound[axis])/range*65535));
            }
        }
        byte[] payload=bytes.toByteArray(); output.write(payload); output.write(digest(payload,payload.length));
    }
    /** Budget limits decoded primitive payload; peak loading memory also includes encoded input and temporary copies. */
    public static VertexClip read(InputStream input, String expectedHash, int expectedConverter, int expectedPart, int maxDecodedBytes) throws IOException {
        if (maxDecodedBytes < 1 || maxDecodedBytes > Integer.MAX_VALUE-129) throw new IllegalArgumentException("Invalid clip budget");
        byte[] bytes=input.readNBytes(maxDecodedBytes+129);
        if (bytes.length > maxDecodedBytes+128 || bytes.length < 107) throw new IOException("Invalid clip size");
        int length=bytes.length-32;
        if (!MessageDigest.isEqual(digest(bytes,length),Arrays.copyOfRange(bytes,length,bytes.length))) throw new IOException("Clip checksum mismatch");
        try {
            ByteBuffer data=ByteBuffer.wrap(bytes,0,length);
            if (data.getInt()!=MAGIC || data.getShort()!=VERSION) throw new IllegalArgumentException("Unsupported clip schema");
            int encoding=data.get();
            if (encoding!=1 && encoding!=2) throw new IllegalArgumentException("Unsupported encoding");
            if (data.getInt()!=expectedConverter || expectedConverter < 1) throw new IllegalArgumentException("Converter mismatch");
            byte[] hash=new byte[32]; data.get(hash);
            if (!MessageDigest.isEqual(hash,fingerprint(expectedHash))) throw new IllegalArgumentException("Source mismatch");
            if (data.getInt() != expectedPart || expectedPart < 0) throw new IllegalArgumentException("Geometry part mismatch");
            int vertices=data.getInt(), frames=data.getInt();
            if (vertices < 1 || frames < 1 || vertices > maxDecodedBytes/12 || frames > maxDecodedBytes/4)
                throw new IllegalArgumentException("Dimensions exceed budget");
            long components=3L*vertices*frames;
            if (vertices < 1 || frames < 1 || components > Integer.MAX_VALUE || 4L*(components+frames)>maxDecodedBytes
                    || 4L*frames+24+components*(encoding==1?4:2)!=data.remaining()) throw new IllegalArgumentException("Invalid dimensions or budget");
            float[] times=new float[frames]; for (int i=0;i<frames;i++) times[i]=data.getFloat();
            float[] bound=new float[6]; for (int i=0;i<6;i++) { bound[i]=data.getFloat(); if (!Float.isFinite(bound[i])) throw new IllegalArgumentException("Nonfinite bound"); }
            for (int i=0;i<3;i++) if (bound[i]>bound[i+3]) throw new IllegalArgumentException("Invalid bound");
            float[] positions=new float[(int)components];
            for (int i=0;i<positions.length;i++) {
                int axis=i%3;
                positions[i]=encoding==1 ? data.getFloat() : (float)(bound[axis]+((double)bound[axis+3]-bound[axis])*(Short.toUnsignedInt(data.getShort())/65535.0));
                if (positions[i]<bound[axis] || positions[i]>bound[axis+3]) throw new IllegalArgumentException("Position outside bounds");
            }
            return new VertexClip(vertices,times,positions);
        } catch (IllegalArgumentException | java.nio.BufferUnderflowException error) { throw new IOException("Invalid vertex clip",error); }
    }
}

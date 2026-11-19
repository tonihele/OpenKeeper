/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import toniarts.openkeeper.tools.convert.*;
import toniarts.openkeeper.tools.convert.kmf.*;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/** Offline opt-in derived data bake/audit. Never writes the existing converted root. */
public final class VertexClipBake {
    static final int BUDGET = 64*1024*1024;
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || args.length==4 && !args[3].equals("--all-creatures"))
            throw new IllegalArgumentException("Expected DK2 root, converted root, isolated output directory and optional --all-creatures");
        Path converted=Path.of(args[1]).toAbsolutePath().normalize(), output=Path.of(args[2]).toAbsolutePath().normalize();
        if (output.startsWith(converted) || converted.startsWith(output)) throw new IllegalArgumentException("Derived output must be isolated from converted assets");
        Files.createDirectories(output);
        var assets=new DesktopAssetManager(true); assets.registerLocator(converted.toString(),FileLocator.class);
        var wad=new WadFile(Path.of(args[0],"Data","Meshes.WAD"));
        var names=new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        for (String entry : wad.getWadFileEntries()) names.put(Path.of(entry).getFileName().toString(),entry);
        Collection<String> resources=List.of("Imp_Run","Goblin-Walk","Mis_Walk","Knight-Walk","Warlock-Walk","Bile_Demon-Walk","Fir-Walk","Skeleton-Walk");
        if (args.length==4) {
            var catalog=KwdFile.load(args[0],Path.of(args[0],"Data","Editor","Maps","Alcatraz.kwd"));
            var all=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (var creature : catalog.getCreatureList()) for (var art : creature.getAnimations().values()) {
                if (art==null || art.getType()!=ArtResource.ArtResourceType.ANIMATING_MESH) continue;
                all.add(art.getName());
                if (art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) all.add(art.getName()+"Start");
                if (art.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) all.add(art.getName()+"End");
            }
            resources=all;
        }
        long full=0, quantized=0, sourceBytes=0, invalidIndices=0, lodLevels=0;
        double maxError=0, maxVelocityError=0; int parts=0, staticResources=0;
        var report=new ArrayList<String>(); report.add("resource,part,vertices,keys,float32_bytes,unorm16_bytes,max_local_error,source_sha256,max_local_velocity_error");
        for (String name : resources) {
            byte[] bytes=wad.getFileData(Objects.requireNonNull(names.get(name+".kmf"),name)); sourceBytes+=bytes.length;
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            var kmf=new KmfFile(bytes);
            if (kmf.getAnim()!=null) for (var sprite : kmf.getAnim().getSprites()) for (var level : sprite.getTriangles()) {
                lodLevels++;
                for (var triangle : level) for (byte index : triangle.getTriangle())
                    if (Byte.toUnsignedInt(index)>=sprite.getVertices().size()) invalidIndices++;
            }
            Spatial model=(Spatial)new KmfModelLoader().load(new KmfAssetInfo(assets,new AssetKey<>(name),kmf,false));
            var tracks=new ArrayList<PoseTrack>();
            model.depthFirstTraversal(node -> {
                var composer=node.getControl(AnimComposer.class);
                if (composer!=null) for (var clip : composer.getAnimClips()) for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) tracks.add(pose);
            });
            if (tracks.isEmpty()) { staticResources++; System.out.println("VERTEX_STATIC resource="+name); }
            for (int part=0;part<tracks.size();part++) {
                var track=tracks.get(part); var packed=VertexClip.bake(track,BUDGET);
                byte[][] encoded=new byte[2][]; double error=0, velocityError=0;
                for (var encoding : VertexClip.Encoding.values()) {
                    var buffer=new ByteArrayOutputStream(); int version=AssetsConverter.ConvertProcess.MODELS.getVersion();
                    packed.write(buffer,encoding,hash,version,part); encoded[encoding.ordinal()]=buffer.toByteArray();
                    var decoded=VertexClip.read(new ByteArrayInputStream(buffer.toByteArray()),hash,version,part,BUDGET);
                    float[] result=new float[packed.vertexCount()*3];
                    // Include exact source keys and fractions of every interval, not a uniform grid that can miss keys.
                    for (int sample=-1;sample<=packed.keyCount()*4;sample++) {
                        int key=Math.min(packed.keyCount()-1,Math.max(0,sample/4));
                        int next=Math.min(key+1,packed.keyCount()-1);
                        double time=sample<0 ? -1 : packed.keyTime(key)-packed.keyTime(0)
                                +(packed.keyTime(next)-packed.keyTime(key))*(sample%4/4.0);
                        if (sample==packed.keyCount()*4) time=track.getLength()+1;
                        track.getDataAtTime(time,null); decoded.sample(time,result);
                        FloatBuffer reference=(FloatBuffer)track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
                        for (int i=0;i<result.length;i++) {
                            double delta=Math.abs(reference.get(i)-result[i]);
                            if (!Double.isFinite(delta) || encoding==VertexClip.Encoding.FLOAT32 && delta!=0)
                                throw new AssertionError("Reference mismatch in "+name+" part "+part);
                            error=Math.max(error,delta);
                        }
                    }
                    if (encoding==VertexClip.Encoding.UNORM16) velocityError=velocityError(packed,decoded);
                    Path target=output.resolve(hash+"-"+part+"-"+encoding+".okva");
                    Path temporary=Files.createTempFile(output,"vertex-clip-",".tmp");
                    try { Files.write(temporary,buffer.toByteArray()); Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                    finally { Files.deleteIfExists(temporary); }
                }
                full+=encoded[0].length; quantized+=encoded[1].length; maxError=Math.max(maxError,error);
                maxVelocityError=Math.max(maxVelocityError,velocityError); parts++;
                report.add(name+","+part+","+packed.vertexCount()+","+packed.keyCount()+","+encoded[0].length+","+encoded[1].length+","+error+","+hash+","+velocityError);
            }
        }
        Files.write(output.resolve("report.csv"),report);
        System.out.printf(Locale.ROOT,"VERTEX_CLIPS resources=%d static_resources=%d parts=%d kmf_bytes=%d f32_bytes=%d unorm16_bytes=%d max_local_component_error=%.9g max_local_velocity_error=%.9g lod_lists=%d invalid_lod_indices=%d%n",
                resources.size(),staticResources,parts,sourceBytes,full,quantized,maxError,maxVelocityError,lodLevels,invalidIndices);
        if (invalidIndices!=0) throw new AssertionError("Invalid source LOD indices");
    }

    /** Maximum continuous-interval component velocity error; discontinuous wrap resets have no finite derivative. */
    static double velocityError(VertexClip reference, VertexClip decoded) {
        if (reference.vertexCount()!=decoded.vertexCount() || reference.keyCount()!=decoded.keyCount())
            throw new IllegalArgumentException("Mismatched clip layout");
        if (reference.keyTime(0)!=decoded.keyTime(0)) throw new IllegalArgumentException("Mismatched origin");
        int size=reference.vertexCount()*3;
        float[] a=new float[size], b=new float[size], qa=new float[size], qb=new float[size];
        reference.copyFrame(0,a); decoded.copyFrame(0,qa); double maximum=0;
        for (int key=1;key<reference.keyCount();key++) {
            if (reference.keyTime(key)!=decoded.keyTime(key)) throw new IllegalArgumentException("Mismatched time");
            reference.copyFrame(key,b); decoded.copyFrame(key,qb);
            double interval=(double)reference.keyTime(key)-reference.keyTime(key-1);
            for (int i=0;i<size;i++) maximum=Math.max(maximum,Math.abs(((double)qb[i]-qa[i])-((double)b[i]-a[i]))/interval);
            float[] swap=a; a=b; b=swap; swap=qa; qa=qb; qb=swap;
        }
        if (!Double.isFinite(maximum)) throw new AssertionError("Nonfinite velocity error");
        return maximum;
    }
}

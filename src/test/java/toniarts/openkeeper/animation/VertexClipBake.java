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

/** Offline opt-in derived data bake/audit. Never writes the existing converted root. */
public final class VertexClipBake {
    static final int BUDGET = 64*1024*1024;
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected DK2 root, converted root and isolated output directory");
        Path converted=Path.of(args[1]).toAbsolutePath().normalize(), output=Path.of(args[2]).toAbsolutePath().normalize();
        if (output.startsWith(converted) || converted.startsWith(output)) throw new IllegalArgumentException("Derived output must be isolated from converted assets");
        Files.createDirectories(output);
        var assets=new DesktopAssetManager(true); assets.registerLocator(converted.toString(),FileLocator.class);
        var wad=new WadFile(Path.of(args[0],"Data","Meshes.WAD"));
        var names=new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        for (String entry : wad.getWadFileEntries()) names.put(Path.of(entry).getFileName().toString(),entry);
        long full=0, quantized=0, sourceBytes=0, invalidIndices=0, lodLevels=0;
        double maxError=0; int parts=0;
        var report=new ArrayList<String>(); report.add("resource,part,vertices,keys,float32_bytes,unorm16_bytes,max_local_error");
        for (String name : List.of("Imp_Run","Goblin-Walk","Mis_Walk","Knight-Walk","Warlock-Walk","Bile_Demon-Walk","Fir-Walk","Skeleton-Walk")) {
            byte[] bytes=wad.getFileData(Objects.requireNonNull(names.get(name+".kmf"),name)); sourceBytes+=bytes.length;
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            var kmf=new KmfFile(bytes);
            for (var sprite : kmf.getAnim().getSprites()) for (var level : sprite.getTriangles()) {
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
            for (int part=0;part<tracks.size();part++) {
                var track=tracks.get(part); var packed=VertexClip.bake(track,BUDGET);
                byte[][] encoded=new byte[2][]; double error=0;
                for (var encoding : VertexClip.Encoding.values()) {
                    var buffer=new ByteArrayOutputStream(); int version=AssetsConverter.ConvertProcess.MODELS.getVersion();
                    packed.write(buffer,encoding,hash,version,part); encoded[encoding.ordinal()]=buffer.toByteArray();
                    var decoded=VertexClip.read(new ByteArrayInputStream(buffer.toByteArray()),hash,version,part,BUDGET);
                    float[] result=new float[packed.vertexCount()*3];
                    for (int sample=0;sample<=packed.keyCount()*4;sample++) {
                        double time=track.getLength()*sample/(packed.keyCount()*4.0);
                        track.getDataAtTime(time,null); decoded.sample(time,result);
                        FloatBuffer reference=(FloatBuffer)track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
                        for (int i=0;i<result.length;i++) {
                            double delta=Math.abs(reference.get(i)-result[i]);
                            if (!Double.isFinite(delta) || encoding==VertexClip.Encoding.FLOAT32 && delta!=0)
                                throw new AssertionError("Reference mismatch in "+name+" part "+part);
                            error=Math.max(error,delta);
                        }
                    }
                    Path target=output.resolve(hash+"-"+part+"-"+encoding+".okva");
                    Path temporary=Files.createTempFile(output,"vertex-clip-",".tmp");
                    try { Files.write(temporary,buffer.toByteArray()); Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                    finally { Files.deleteIfExists(temporary); }
                }
                full+=encoded[0].length; quantized+=encoded[1].length; maxError=Math.max(maxError,error); parts++;
                report.add(name+","+part+","+packed.vertexCount()+","+packed.keyCount()+","+encoded[0].length+","+encoded[1].length+","+error);
            }
        }
        Files.write(output.resolve("report.csv"),report);
        System.out.printf(Locale.ROOT,"VERTEX_CLIPS parts=%d kmf_bytes=%d f32_bytes=%d unorm16_bytes=%d max_local_component_error=%.9g lod_lists=%d invalid_lod_indices=%d%n",
                parts,sourceBytes,full,quantized,maxError,lodLevels,invalidIndices);
        if (invalidIndices!=0) throw new AssertionError("Invalid source LOD indices");
    }
}

/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import toniarts.openkeeper.tools.convert.map.*;

/** Necessary structural screen only: equality cannot prove semantic vertex correspondence. */
public final class AnimationTopologyAudit {
    record Layout(int parts, int vertices, int keys, String geometry, String transforms, String materials) { }
    private record Clip(Creature.AnimationType slot, String name, Layout layout) { }

    static Layout inspect(Spatial root) {
        var shape = new ByteArrayOutputStream(); var transforms = new ByteArrayOutputStream(); var materials = new ByteArrayOutputStream();
        var a = new DataOutputStream(shape); var b = new DataOutputStream(transforms); var c = new DataOutputStream(materials);
        int[] total = new int[3];
        root.depthFirstTraversal(node -> {
            try {
                if (node instanceof Geometry geometry) {
                    Mesh mesh = geometry.getMesh(); total[0]++; total[1] += mesh.getVertexCount();
                    a.writeInt(mesh.getVertexCount()); a.writeInt(mesh.getMode().ordinal());
                    var indices = mesh.getIndexBuffer(); a.writeInt(indices == null ? 0 : indices.size());
                    if (indices != null) for (int i=0;i<indices.size();i++) a.writeInt(indices.get(i));
                    var uv = mesh.getBuffer(VertexBuffer.Type.TexCoord);
                    a.writeInt(uv == null ? 0 : uv.getNumComponents());
                    a.writeInt(uv == null ? 0 : uv.getData().limit());
                    if (uv != null) {
                        if (!(uv.getData() instanceof FloatBuffer f)) throw new IllegalArgumentException("Expected float UVs");
                        for (int i=0;i<f.limit();i++) a.writeInt(Float.floatToIntBits(f.get(i)));
                    }
                    // Include every ancestor, without changing/updating the live scene's world transforms.
                    int depth = 0;
                    for (Spatial parent = geometry; parent != null; parent = parent.getParent()) depth++;
                    b.writeInt(depth);
                    for (Spatial parent = geometry; parent != null; parent = parent.getParent()) {
                        var t = parent.getLocalTranslation(); var r = parent.getLocalRotation(); var s = parent.getLocalScale();
                        for (float value : new float[]{t.x,t.y,t.z,r.getX(),r.getY(),r.getZ(),r.getW(),s.x,s.y,s.z}) b.writeFloat(value);
                    }
                    b.writeInt(0x454e4421);
                    var material = geometry.getMaterial();
                    c.writeUTF(material == null ? "NONE" : material.getMaterialDef().getName());
                    if (material != null) {
                        var texture = material.getTextureParam("DiffuseMap");
                        c.writeUTF(texture == null ? "NONE" : String.valueOf(texture.getTextureValue().getKey()));
                        c.writeUTF(material.getAdditionalRenderState().getFaceCullMode().name());
                        c.writeUTF(material.getAdditionalRenderState().getBlendMode().name());
                        c.writeBoolean(material.isTransparent());
                        for (String name : List.of("UseAlphaCoverage", "AlphaDiscardThreshold", "NumberOfTiles", "Speed")) {
                            var parameter = material.getParam(name);
                            c.writeUTF(name + "=" + (parameter == null ? "ABSENT" : parameter.getValue()));
                        }
                    }
                }
                var composer = node.getControl(AnimComposer.class);
                if (composer != null) for (var clip : composer.getAnimClips()) for (var track : clip.getTracks())
                    if (track instanceof PoseTrack pose) total[2] = Math.max(total[2], pose.getTimes().length);
            } catch (IOException error) { throw new UncheckedIOException(error); }
        });
        return new Layout(total[0],total[1],total[2],hash(shape.toByteArray()),hash(transforms.toByteArray()),hash(materials.toByteArray()));
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new AssertionError(error); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected DK2 root, converted root, output CSV");
        var assets = new DesktopAssetManager(true); assets.registerLocator(args[1], FileLocator.class);
        var files = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        try (var list = Files.list(Path.of(args[1],"Models"))) { list.forEach(p -> files.put(p.getFileName().toString(),p.getFileName().toString())); }
        var catalog = KwdFile.load(args[0],Path.of(args[0],"Data","Editor","Maps","Alcatraz.kwd"));
        var rows = new ArrayList<String>(); rows.add("creature,slot_a,slot_b,resource_a,resource_b,vertices_a,vertices_b,geometry_matches,transforms_match,material_descriptors_match");
        int clips = 0, missing = 0, matches = 0, walkPairs = 0, walkMatches = 0;
        for (String token : List.of("imp","goblin","mistress","knight","warlock","bile","firefly","skeleton")) {
            var creature = catalog.getCreatureList().stream().filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(token)).findFirst().orElseThrow();
            var inspected = new ArrayList<Clip>();
            for (var slot : new Creature.AnimationType[]{Creature.AnimationType.WALK,Creature.AnimationType.RUN,Creature.AnimationType.STEALTH_WALK,
                    Creature.AnimationType.STAND_STILL,Creature.AnimationType.IDLE_1,Creature.AnimationType.MELEE_ATTACK,Creature.AnimationType.DIE,Creature.AnimationType.DEATH_POSE}) {
                var art = creature.getAnimation(slot);
                if (art == null || art.getType() == ArtResource.ArtResourceType.NONE) { missing++; continue; }
                String file = files.get(art.getName()+".j3o");
                if (file == null) throw new IOException("Missing converted resource: "+art.getName());
                inspected.add(new Clip(slot,art.getName(),inspect(assets.loadModel("Models/"+file)))); clips++;
            }
            for (int i=0;i<inspected.size();i++) for (int j=i+1;j<inspected.size();j++) {
                Clip a = inspected.get(i), b = inspected.get(j); boolean same = a.layout.geometry.equals(b.layout.geometry);
                if (same) matches++;
                if (a.slot == Creature.AnimationType.WALK) { walkPairs++; if (same) walkMatches++; }
                rows.add(String.join(",",csv(creature.getName()),a.slot.name(),b.slot.name(),csv(a.name),csv(b.name),
                        ""+a.layout.vertices,""+b.layout.vertices,""+same,""+a.layout.transforms.equals(b.layout.transforms),""+a.layout.materials.equals(b.layout.materials)));
            }
        }
        Path output = Path.of(args[2]); Files.createDirectories(output.toAbsolutePath().getParent()); Files.write(output,rows);
        System.out.printf("TOPOLOGY clips=%d missing=%d pairs=%d structural_matches=%d walk_pairs=%d walk_matches=%d NO_SEMANTIC_CORRESPONDENCE_CLAIM%n",
                clips,missing,rows.size()-1,matches,walkPairs,walkMatches);
    }
    private static String csv(String value) { return '"'+value.replace("\"","\"\"")+'"'; }
}

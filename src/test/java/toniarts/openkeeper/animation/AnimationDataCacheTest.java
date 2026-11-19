/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.*;
import com.jme3.asset.*;
import com.jme3.scene.*;
import com.jme3.util.clone.Cloner;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AnimationDataCacheTest {
    static Node template(boolean attachTarget) {
        var pose=PoseTrackTest.track(1f/30); var clip=new AnimClip("test"); clip.setTracks(new PoseTrack[]{pose});
        var composer=new AnimComposer(); composer.addAnimClip(clip);
        var node=new Node(); node.addControl(composer); if (attachTarget) node.attachChild(pose.getTarget()); return node;
    }
    static PoseTrack pose(Spatial model) { return (PoseTrack)model.getControl(AnimComposer.class).getAnimClip("test").getTracks()[0]; }

    @Test void clonesShareImmutableDataAndKeepFractionalOutputIndependent() {
        var source=template(true); var reference=new Cloner().clone(source);
        var cache=new AnimationDataCache(1024); cache.prepare(source);
        var a=new Cloner().clone(source); var b=new Cloner().clone(source);
        assertSame(pose(a).packedData(),pose(b).packedData());
        assertEquals(pose(a).packedData().decodedBytes(),cache.getResidentPrimitiveBytes());
        for (int frame=-10;frame<220;frame++) {
            double time=frame/100.0; pose(a).getDataAtTime(time,null); pose(reference).getDataAtTime(time,null);
            var actual=(FloatBuffer)pose(a).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            var expected=(FloatBuffer)pose(reference).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            for (int i=0;i<actual.limit();i++) assertEquals(expected.get(i),actual.get(i),0);
        }
        pose(b).getDataAtTime(.5,null);
        assertNotSame(pose(a).getTarget().getMesh(),pose(b).getTarget().getMesh());
        assertNotEquals(((FloatBuffer)pose(a).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(0),
                ((FloatBuffer)pose(b).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(0));
    }
    @Test void budgetFallbackAndCrossCachePreparationNeverOvercommit() {
        var first=template(true); int bytes=(int)VertexClip.bake(pose(first),1024).decodedBytes();
        var cache=new AnimationDataCache(bytes); cache.prepare(first); cache.prepare(first);
        var second=template(true); cache.prepare(second);
        assertTrue(pose(first).isPackedSampling()); assertFalse(pose(second).isPackedSampling());
        assertEquals(bytes,cache.getResidentPrimitiveBytes());
        var smaller=new AnimationDataCache(bytes-1); smaller.prepare(first);
        assertFalse(pose(first).isPackedSampling()); assertEquals(0,smaller.getResidentPrimitiveBytes());
        assertThrows(IllegalArgumentException.class,()->new AnimationDataCache(-1));
    }
    @Test void retargetingAndRetimingDiscardSourceBoundPackedData() {
        var node=template(true); var track=pose(node); var cache=new AnimationDataCache(1024); cache.prepare(node);
        var target=(Geometry)track.getTarget().deepClone();
        ((FloatBuffer)target.getMesh().getBuffer(VertexBuffer.Type.BindPosePosition).getData()).put(3,50);
        track.setTarget(target); assertFalse(track.isPackedSampling()); track.getDataAtTime(.5,null);
        assertEquals(50,((FloatBuffer)target.getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(3));
        cache.prepare(node); assertTrue(track.isPackedSampling());
        track.setTimes(new float[]{0,2,4}); assertFalse(track.isPackedSampling());
    }
    @Test void utilityContextConfigurationReleaseAndManagerIsolationPreserveLiveClones() {
        class Assets extends DesktopAssetManager {
            Assets() { super(true); }
            @Override public Spatial loadModel(ModelKey key) { return template(false); }
        }
        var a=new Assets(); var b=new Assets(); AssetUtils.configureAnimationDataCache(a,1024);
        var live=AssetUtils.loadModel(a,"packed-context",null);
        assertTrue(pose(live).isPackedSampling()); assertTrue(AssetUtils.getAnimationDataPrimitiveBytes(a)>0);
        assertFalse(pose(AssetUtils.loadModel(b,"packed-context",null)).isPackedSampling());
        assertThrows(IllegalStateException.class,()->AssetUtils.configureAnimationDataCache(a,0));
        AssetUtils.releaseCaches(a); assertEquals(0,AssetUtils.getAnimationDataPrimitiveBytes(a));
        assertTrue(pose(live).isPackedSampling()); pose(live).getDataAtTime(.5,null);
    }

    @Test void packedDataIsNotSerializedAndLegacyClipDataRemainsExecutable() throws Exception {
        var source=template(true); new AnimationDataCache(1024).prepare(source);
        var bytes=new java.io.ByteArrayOutputStream();
        com.jme3.export.binary.BinaryExporter.getInstance().save(source,bytes);
        var imported=(Node)com.jme3.export.binary.BinaryImporter.getInstance().load(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        assertFalse(pose(imported).isPackedSampling());
        for (double time : new double[]{-1,0,.173,.777,1.99,10}) {
            pose(source).getDataAtTime(time,null); pose(imported).getDataAtTime(time,null);
            var a=(FloatBuffer)pose(source).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            var b=(FloatBuffer)pose(imported).getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            for (int i=0;i<a.limit();i++) assertEquals(a.get(i),b.get(i),0);
        }
    }
}

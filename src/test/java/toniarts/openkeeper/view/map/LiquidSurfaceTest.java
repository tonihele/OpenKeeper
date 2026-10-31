package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.image.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class LiquidSurfaceTest {
    @Test void hasTheNativeGridDensityAndIndependentTileUvs() {
        var instance = new EntityInstance<>(new Terrain());
        instance.addCoordinate(new Point(1,2));instance.addCoordinate(new Point(2,2));
        var mesh = Water.createMesh(List.of(instance));
        assertEquals(18,mesh.getVertexCount());assertEquals(16,mesh.getTriangleCount());
        var positions=mesh.getFloatBuffer(VertexBuffer.Type.Position);
        var uv=mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
        assertEquals(.5f,positions.get(0));assertEquals(1.5f,positions.get(8*3));
        assertEquals(1.5f,positions.get(9*3));
        assertEquals(1f,uv.get(8*2));assertEquals(0f,uv.get(9*2));
        assertEquals(LiquidWaves.LEVEL,positions.get(1));
    }

    @Test void flatHeightMapsGiveNeutralDisplacementInEveryFrame() {
        var dark = new BufferedImage(4,4,BufferedImage.TYPE_BYTE_GRAY);
        var bright = new BufferedImage(4,4,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)bright.getRaster().setSample(x,y,0,255);
        var texture=LiquidTextures.bumpAtlas(new BufferedImage[]{dark,bright});
        assertEquals(ColorSpace.Linear,texture.getImage().getColorSpace());
        assertEquals(8,texture.getImage().getWidth());
        var pixels=texture.getImage().getData(0);
        for(int p=0;p<pixels.limit();p+=4) {
            assertEquals(128,Byte.toUnsignedInt(pixels.get(p)));
            assertEquals(128,Byte.toUnsignedInt(pixels.get(p+1)));
        }
    }

    @Test void displacementWrapsWithinEachSourceFrame() {
        var frame=new BufferedImage(4,4,BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)frame.getRaster().setSample(x,y,0,x*16);
        var texture=LiquidTextures.bumpAtlas(new BufferedImage[]{frame});
        var pixels=texture.getImage().getData(0);
        assertEquals(160,Byte.toUnsignedInt(pixels.get(0))); // Wrapped left=48, right=16.
        assertEquals(96,Byte.toUnsignedInt(pixels.get(4))); // left=0, right=32.
        assertEquals(128,Byte.toUnsignedInt(pixels.get(5))); // No vertical gradient.
    }

    @Test void missingOriginalMapsFallBackAndAreNotRetriedEveryFrame(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) {
        var assets = new com.jme3.asset.DesktopAssetManager(false) {
            @Override public com.jme3.asset.AssetInfo locateAsset(com.jme3.asset.AssetKey key) { return null; }
        };
        String old = toniarts.openkeeper.utils.PathUtils.getDKIIFolder();
        try {
            toniarts.openkeeper.utils.PathUtils.setDKIIFolder(directory.toString());
            assertTrue(LiquidTextures.load(assets, false).isEmpty());
            assertTrue(LiquidTextures.load(assets, true).isEmpty());
            assertTrue(LiquidTextures.load(assets, false).isEmpty());
        } finally {
            toniarts.openkeeper.utils.PathUtils.setDKIIFolder(old);
        }
    }

    @Test void phaseRepeatsWithoutLongRunningAnimationDrift() {
        assertEquals(LiquidWaves.height(12.5,37.5,321,false), LiquidWaves.height(12.5,37.5,321+128,false));
    }
}

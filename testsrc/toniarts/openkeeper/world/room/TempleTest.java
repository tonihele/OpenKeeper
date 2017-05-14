package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import toniarts.openkeeper.utils.AssetUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.when;

/**
 * Created by wietse on 13/05/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( AssetUtils.class )
public class TempleTest {


    private AssetManager assetManager;

    @Test
    public void verifyThatAFullSquareResultsInAllTwos() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,true, true, true, true,true, true, true, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple13");
        expected.add("Temple13");
        expected.add("Temple13");
        expected.add("Temple13");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(-0.25f,0,0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,0.25f));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifyNorthAndEast() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,false, true, false, false,false, false, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple2");
        expected.add("Temple1");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(1.5707964f);
        rotationAnglesExpected.add(3.1415927f);
        rotationAnglesExpected.add(3.1415927f);
        rotationAnglesExpected.add(3.1415927f);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(-0.25f,0,0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,0.25f));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifyNorthAndEastAndNorthEast() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", true,true, true, false, false,false, false, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple2");
        expected.add("Temple1");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(1.5707964f);
        rotationAnglesExpected.add(-1.5707964f);
        rotationAnglesExpected.add(3.1415927f);
        rotationAnglesExpected.add(3.1415927f);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(-0.25f,0,0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,0.25f));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

    @Test
    public void verifySouthAndWest() {

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Float> captorFloat = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Vector3f> captorVector = ArgumentCaptor.forClass(Vector3f.class);
        final boolean[][] borderArea = {{false, false, false},{false, false, false},{false, false, false}};
        final Point localPoint = new Point(1,1);

        Node mockNode = Mockito.mock(Node.class);

        PowerMockito.mockStatic(AssetUtils.class);
        when(AssetUtils.loadModel(any(), captor.capture())).thenReturn(mockNode);
        when(mockNode.rotate(anyFloat(), captorFloat.capture(), anyFloat())).thenReturn(mockNode);
        when(mockNode.move(captorVector.capture())).thenReturn(mockNode);

        Temple.constructQuad(assetManager, "Temple", false,false, false, false, true,false, true, false, borderArea, localPoint, false);

        final List<String> expected = new ArrayList<>(4);
        expected.add("Temple0");
        expected.add("Temple1");
        expected.add("Temple2");
        expected.add("Temple0");

        final List<String> actual = captor.getAllValues();

        final List<Float> rotationAnglesActual = captorFloat.getAllValues();
        final List<Float> rotationAnglesExpected = new ArrayList<>(4);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(0.0f);
        rotationAnglesExpected.add(-1.5707964f);
        rotationAnglesExpected.add(-1.5707964f);

        final List<Vector3f> vectorsActual = captorVector.getAllValues();
        final List<Vector3f> vectorsExpected = new ArrayList<>(4);
        vectorsExpected.add(new Vector3f(-0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,-0.25f));
        vectorsExpected.add(new Vector3f(-0.25f,0,0.25f));
        vectorsExpected.add(new Vector3f(0.25f,0,0.25f));

        Assert.assertArrayEquals("Output not equal", expected.toArray(), actual.toArray());
        Assert.assertArrayEquals("Output not equal", rotationAnglesExpected.toArray(), rotationAnglesActual.toArray());
        Assert.assertArrayEquals("Output not equal", vectorsExpected.toArray(), vectorsActual.toArray());

    }

}

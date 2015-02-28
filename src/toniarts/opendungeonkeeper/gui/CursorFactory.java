/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.gui;

import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.regex.Matcher;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;

/**
 * Small utility for creating cursors from the original DK II assets
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CursorFactory {

    public enum Cursor {

        IDLE, POINTER;
    }
    private static HashMap<Cursor, JmeCursor> cursors;
    private static final Object lock = new Object();

    private CursorFactory() {
        // Nope
    }

    public static JmeCursor getCursor(Cursor cursor, AssetManager assetManager) {
        if (cursors == null) {
            synchronized (lock) {
                if (cursors == null) {
                    initializeCursors(assetManager);
                }
            }
        }
        return cursors.get(cursor);
    }

    private static void initializeCursors(AssetManager assetManager) {

        cursors = new HashMap<>(Cursor.values().length);

        //
        // IDLE, animated cursor (Point.png)
        //
        JmeCursor cursor = new JmeCursor();
        Texture tex = assetManager.loadTexture(AssetsConverter.MOUSE_CURSORS_FOLDER.concat(File.separator).concat("Point.png").replaceAll(Matcher.quoteReplacement(File.separator), "/"));
        Image img = tex.getImage();

        // Image data
        ByteBuffer data = img.getData(0);
        data.rewind();
        IntBuffer image = BufferUtils.createIntBuffer(img.getHeight() * 80);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int abgr = data.getInt();

                // Skip the first 2 columns, since the maximum size for a cursor seems to be 80
                if (x < 2) {
                    continue;
                }
                int argb = ((abgr & 255) << 24) | (abgr >> 8);
                image.put(argb);
            }
        }
        image.rewind();

        // Delays
        IntBuffer delays = BufferUtils.createIntBuffer(41);
        for (int i = 0; i < 41; i++) {
            delays.put(30);
        }

        cursor.setNumImages(41);
        cursor.setWidth(80);
        cursor.setHeight(53);
        cursor.setxHotSpot(6);
        cursor.setyHotSpot(cursor.getHeight() - 32);
        cursor.setImagesData(image);
        cursor.setImagesDelay((IntBuffer) delays.rewind());
        cursors.put(Cursor.IDLE, cursor);

        //
        // GUI point cursor (Idle.png)
        //
        cursor = new JmeCursor();
        tex = assetManager.loadTexture(AssetsConverter.MOUSE_CURSORS_FOLDER.concat(File.separator).concat("Idle.png").replaceAll(Matcher.quoteReplacement(File.separator), "/"));
        img = tex.getImage();

        // Image data
        data = img.getData(0);
        data.rewind();
        image = BufferUtils.createIntBuffer(img.getHeight() * 80);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int abgr = data.getInt();

                // Skip the first 3 columns, since the maximum size for a cursor seems to be 80
                if (x < 3) {
                    continue;
                }
                int argb = ((abgr & 255) << 24) | (abgr >> 8);
                image.put(argb);
            }
        }
        image.rewind();

        cursor.setNumImages(1);
        cursor.setWidth(80);
        cursor.setHeight(39);
        cursor.setxHotSpot(1);
        cursor.setyHotSpot(cursor.getHeight() - 4);
        cursor.setImagesData(image);
        cursors.put(Cursor.POINTER, cursor);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state;

import com.jme3.system.AppSettings;
import java.awt.DisplayMode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ArchDemon
 */
public class MyDisplayMode implements Comparable<MyDisplayMode> {

    protected final int height;
    protected final int width;
    protected final int bitDepth;
    protected final List<Integer> refreshRate = new ArrayList<>(10);

    public MyDisplayMode(DisplayMode dm) {
        height = dm.getHeight();
        width = dm.getWidth();
        bitDepth = dm.getBitDepth();
        refreshRate.add(dm.getRefreshRate());
    }

    MyDisplayMode(AppSettings settings) {
        height = settings.getHeight();
        width = settings.getWidth();
        bitDepth = settings.getBitsPerPixel();
        refreshRate.add(settings.getFrequency());
    }

    public void addRefreshRate(DisplayMode dm) {
        if (dm.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN && !refreshRate.contains(dm.getRefreshRate())) {
            refreshRate.add(dm.getRefreshRate());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MyDisplayMode other = (MyDisplayMode) obj;
        if (this.height != other.height) {
            return false;
        }
        if (this.width != other.width) {
            return false;
        }
        if (this.bitDepth != other.bitDepth) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.height;
        hash = 37 * hash + this.width;
        hash = 37 * hash + this.bitDepth;
        return hash;
    }

    @Override
    public String toString() {
        return width + " x " + height + (bitDepth != DisplayMode.BIT_DEPTH_MULTI ? " @" + bitDepth : "");
    }

    @Override
    public int compareTo(MyDisplayMode o) {
        int result = Integer.compare(bitDepth, o.bitDepth);
        if (result == 0) {
            result = Integer.compare(width, o.width);
        }
        if (result == 0) {
            result = Integer.compare(height, o.height);
        }
        return result;
    }
}
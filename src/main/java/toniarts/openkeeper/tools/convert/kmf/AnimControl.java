/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.kmf;

/**
 * KMF Anim Control wrapper, a marked vertex of the animation. DK II draws its
 * effects from these, e.g. the speed blur from the ids 1000 to 1999
 * (0x5A3AA0)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class AnimControl {

    private int sprite;
    private int vertex;
    private int id;

    /**
     * @return the index of the sprite the vertex is in
     */
    public int getSprite() {
        return sprite;
    }

    protected void setSprite(int sprite) {
        this.sprite = sprite;
    }

    /**
     * @return the index of the vertex in its sprite
     */
    public int getVertex() {
        return vertex;
    }

    protected void setVertex(int vertex) {
        this.vertex = vertex;
    }

    /**
     * @return the id of the point, its thousands tell what the point is for
     */
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }
}

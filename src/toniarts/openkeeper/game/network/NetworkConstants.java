/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.network;

import com.simsilica.ethereal.net.ObjectStateProtocol;
import com.simsilica.ethereal.zone.ZoneGrid;
import com.simsilica.mathd.Vec3i;
import com.simsilica.mathd.bits.QuatBits;
import com.simsilica.mathd.bits.Vec3Bits;

/**
 * Constants related to networkign. Derived from Paul Speed's examples.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class NetworkConstants {

    public final static int PROTOCOL_VERSION = 1;
    public final static String GAME_NAME = "OpenKeeper";

    public final static byte LOBBY_CHANNEL = 0;

    public final static byte GAME_CHANNEL = -1;

    /**
     * We add an extra channel on the client->server connection to send chat
     * related messages. This is its own separate TCP socket that avoids tying
     * up the main connection.
     */
    public final static byte CHAT_CHANNEL = 1;

    public static final int GRID_CELL_SIZE = 32;

    // To allow players to see farther in space, we'll use a larger grid
    // size for the zone manager.  We could have also used a wider zone radius
    // and we might use both.  The gridSize used in a real game is mostly a
    // balance between how likely it is that an object will fall into more than
    // one zone at a time, with how many objects are likely to be in a zone.
    // Also, while the zone radius can be increased to include more surrounding
    // zones in a player's view, there is considerably more management involved
    // with each new zone, more network messages, etc..  Finding the sweet spot
    // will depend largely on the game.
    private static final int gridSize = 64;

    /**
     * The 3D zone grid definition that defines how space is broken up into
     * network zones.
     */
    public static final ZoneGrid ZONE_GRID = new ZoneGrid(gridSize, gridSize, gridSize);

    public static final float MAX_OBJECT_RADIUS = 5;

    /**
     * Defines how many network message bits to encode the elements of position
     * fields. This will be a function of the grid size and resolution desired.
     * Keep in mind that objects can be in a zone even if their raw position is
     * not in that zone because their radius may overlap that zone. So the
     * proper range needs to account for this overlap or there will be odd
     * position clipping at the borders as objects cross zone boundaries.
     */
    public static final Vec3Bits POSITION_BITS = new Vec3Bits(-MAX_OBJECT_RADIUS,
            gridSize + MAX_OBJECT_RADIUS,
            16);

    /**
     * Defines how many network message bits to encode the elements of rotation
     * fields. Given that rotation Quaternion values are always between -1 and
     * 1, 12 bits seems sufficient based on ultimate resolution and visual
     * testing.
     */
    public static final QuatBits ROTATION_BITS = new QuatBits(12);

    /**
     * Defines the overall object protocol parameters for how many bits ar used
     * to encode the various parts of an object update message.
     *
     * <p>
     * The first parameter defines how many bits are used to encode zone IDs.
     * Zones are always defined relative to the player so this will be a
     * function of the zone radius. For example, a radius of (1, 1, 1) means a
     * 3D grid that's 3x3x3 or 27 different zones. At least 5 bits would be
     * necessary to encode those IDs. I use 8 here arbitrarily to give some
     * space for zone radius experimentation. 8 bits should support a zone
     * radius up to (3, 3, 2).</p>
     *
     * <p>
     * The second parameter is how many bits are associated with the real object
     * IDs. These IDs are not sent with every message so it's important that the
     * value properly encompass all potential IDs. For example, using a long ID
     * from an ES you would need 64 bits. If game sessions are short lived and
     * your object IDs are only ever 'int' then 32 bits is the proper value.
     * This should usually be 64 bits and I'm keeping it as such... even though
     * this example could get away with 32 bits. Since these values are not sent
     * with every message and people might cut/paste these settings, I'm going
     * with the safer choice.</p>
     *
     * <p>
     * The last two parameters are the Vec3 and Quat bit sizes defined
     * above.</p>
     */
    public static final ObjectStateProtocol OBJECT_PROTOCOL
            = new ObjectStateProtocol(8, 64, POSITION_BITS, ROTATION_BITS);

    /**
     * Defines the 3D zone radius around which updates will be sent. The player
     * is always considered to be in the 'center' and this radius defines how
     * many zones in each direction are included in the view. (A 2D game might
     * only define x,y and leave z as 0.) So a radius of 1 in x means that the
     * player can see one zone to either side of their current zone. A total
     * zone radius of (1, 1, 1) means the player can see a total of 27 zones
     * including the zone they are in.
     */
    public static final Vec3i ZONE_RADIUS = new Vec3i(1, 1, 1);
}

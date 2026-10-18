package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LooseGoldPileTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;
    private static final Point TILE = new Point(5, 7);

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void findsLooseGoldPileOnTile() {
        EntityId pile = addGold(TILE, PLAYER_ID, ObjectType.GOLD);

        assertEquals(pile, GameWorldController.findLooseGoldPile(entityData, TILE));
    }

    @Test
    void ignoresGoldOnOtherTiles() {
        addGold(new Point(6, 7), PLAYER_ID, ObjectType.GOLD);

        assertNull(GameWorldController.findLooseGoldPile(entityData, TILE));
    }

    @Test
    void ignoresGoldStoredInRooms() {
        EntityId pile = addGold(TILE, PLAYER_ID, ObjectType.GOLD);
        entityData.setComponent(pile, new Stored(entityData.createEntity(), ObjectType.GOLD));

        assertNull(GameWorldController.findLooseGoldPile(entityData, TILE));
    }

    @Test
    void findsGoldOfOtherPlayers() {

        // Loose gold belongs to the owner of the land, so the pile owner doesn't matter
        EntityId pile = addGold(TILE, OTHER_PLAYER_ID, ObjectType.GOLD);

        assertEquals(pile, GameWorldController.findLooseGoldPile(entityData, TILE));
    }

    @Test
    void ignoresGoldCarriedByCreatures() {

        // Creatures carry their gold as a component, they are no gold objects
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Gold(100, 300));
        entityData.setComponent(creature, new Owner(PLAYER_ID, PLAYER_ID));
        entityData.setComponent(creature, new Position(0, WorldUtils.pointToVector3f(TILE)));

        assertNull(GameWorldController.findLooseGoldPile(entityData, TILE));
    }

    private EntityId addGold(Point tile, short ownerId, ObjectType objectType) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new ObjectComponent((short) 1, objectType));
        entityData.setComponent(entityId, new Gold(250, 1000));
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        return entityId;
    }

}

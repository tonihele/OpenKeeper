/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.state.session;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.CampaignLevel;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.HeroPartyFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.TriggerFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Writes save games and reads them back
 */
class SaveGameTest {

    /**
     * The level script: a trigger that runs once and adds to flag 0, a
     * trigger that keeps adding to flag 1 from 2 seconds on, and an action of
     * the root that adds to flag 2 once
     */
    private static final IKwdFile LEVEL = stub(IKwdFile.class, Map.of("getTriggers", Map.of(
            1, TriggerFixtures.levelTime(1, 3, 2, (short) 1, 0),
            2, TriggerFixtures.addToFlag(2, 0, (short) 0, 1),
            3, TriggerFixtures.levelTime(3, 5, 4, (short) 255, 2),
            4, TriggerFixtures.addToFlag(4, 0, (short) 1, 1),
            5, TriggerFixtures.addToFlag(5, 0, (short) 2, 5))));

    @TempDir
    Path folder;

    @Test
    void testRoundTrip() throws Exception {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        EntityId lair = entityData.createEntity();
        EntityId tile = entityData.createEntity();
        entityData.setComponents(creature,
                new Position(0.5f, new Vector3f(1.5f, 0.25f, 3f)),
                new Owner((short) 3, (short) 3),
                new CreatureAi(12.5, CreatureState.WANDER, (short) 7, CreatureState.IDLE),
                new CreatureSleep(lair, 10, Double.NaN, 2));
        MapTile mapTile = new MapTile(2, (short) 9, Tile.BridgeTerrainType.LAVA, new Point(4, 5), 42);
        mapTile.selection = new HashMap<>(Map.of((short) 3, true));
        entityData.setComponent(tile, mapTile);
        EntityId removed = entityData.createEntity();
        entityData.removeEntity(removed);

        Keeper keeper = new Keeper(false, (short) 3);
        keeper.setGoldMined(1500);
        keeper.setDungeonHeartLocation(new Point(10, 12));

        Path file = folder.resolve("saves").resolve("Level6a.json.gz");
        new SaveGame("Level6a", null, 1234.5, levelInfo(keeper), List.of(), List.of(), List.of(), List.of(), entityData).write(file);
        SaveGame saveGame = SaveGame.read(file);
        EntityData loaded = saveGame.createEntityData();

        JsonObject json = readJson(file);
        assertEquals(SaveGame.VERSION, json.get("version").getAsInt());
        assertEquals("WANDER", json.getAsJsonObject("components").getAsJsonObject("CreatureAi")
                .getAsJsonObject(Long.toString(creature.getId())).get("creatureState").getAsString(), "States are saved by name");

        assertEquals("Level6a", saveGame.getLevel());
        assertEquals(1234.5, saveGame.getGameTime());
        assertEquals(1, saveGame.getPlayers().size());
        assertEquals(1500, saveGame.getPlayers().get(0).getGoldMined());
        assertEquals(new Point(10, 12), saveGame.getPlayers().get(0).getDungeonHeartLocation());

        assertEquals(new Vector3f(1.5f, 0.25f, 3f), loaded.getComponent(creature, Position.class).position);
        assertEquals(CreatureState.WANDER, loaded.getComponent(creature, CreatureAi.class).getCreatureState());
        assertEquals(CreatureState.IDLE, loaded.getComponent(creature, CreatureAi.class).getTargetState());
        assertEquals(lair, loaded.getComponent(creature, CreatureSleep.class).lairObjectId);
        assertEquals(Double.NEGATIVE_INFINITY, loaded.getComponent(creature, CreatureSleep.class).lastSleepAttemptTime);
        assertTrue(Double.isNaN(loaded.getComponent(creature, CreatureSleep.class).sleepStartTime));
        assertEquals(Map.of((short) 3, true), loaded.getComponent(tile, MapTile.class).selection);
        assertNull(loaded.getComponent(tile, MapTile.class).room);
        assertTrue(loaded.createEntity().getId() > removed.getId(), "New entities must not reuse saved ids");
    }

    @Test
    void testEveryComponentRoundTrips() throws Exception {
        String componentPackage = Position.class.getPackageName();
        Path classes = Paths.get(Position.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assumeTrue(Files.isDirectory(classes), "Components are not in a folder");

        // One entity per component, with every field set
        EntityData entityData = new DefaultEntityData();
        int componentTypes = 0;
        try (Stream<Path> files = Files.list(classes.resolve(componentPackage.replace('.', '/')))) {
            for (Path classFile : files.toList()) {
                String fileName = classFile.getFileName().toString();
                if (!fileName.endsWith(".class") || fileName.contains("$")) {
                    continue;
                }
                Class<?> type = Class.forName(componentPackage + "." + fileName.substring(0, fileName.length() - 6));
                if (EntityComponent.class.isAssignableFrom(type) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                    entityData.setComponent(entityData.createEntity(), (EntityComponent) sample(type, type, 0));
                    componentTypes++;
                }
            }
        }

        // Save, load and save again, the files must match
        Path file = folder.resolve("first.json.gz");
        new SaveGame("Test", null, 1, levelInfo(new Keeper(false, (short) 3)), List.of(), List.of(), List.of(), List.of(), entityData).write(file);
        SaveGame saveGame = SaveGame.read(file);
        Path secondFile = folder.resolve("second.json.gz");
        ILevelInfo levelInfo = levelInfo(saveGame.getPlayers().toArray(Keeper[]::new));
        saveGame.restoreLevelInfo(levelInfo);
        new SaveGame(saveGame.getLevel(), saveGame.getCampaignLevel(), saveGame.getGameTime(), levelInfo, List.of(), List.of(), List.of(), List.of(), saveGame.createEntityData()).write(secondFile);

        JsonObject first = readJson(file);
        JsonObject second = readJson(secondFile);
        assertEquals(componentTypes, first.getAsJsonObject("components").size(), "Every component is saved");
        assertEquals(first, second);
    }

    @Test
    void testLevelInfoRoundTrip() throws Exception {
        ILevelInfo levelInfo = levelInfo(new Keeper(false, (short) 3));
        levelInfo.setFlag(5, 7);
        levelInfo.setFlag(ILevelInfo.FLAG_COUNT - 1, -3);
        levelInfo.getTimer(2).initialize();
        levelInfo.getTimer(2).update(12.5f);
        levelInfo.getTimer(ILevelInfo.TIMER_COUNT - 1).set(40, false);
        levelInfo.setTimeLimit(600);
        levelInfo.setLevelScore(42);

        Path file = folder.resolve("level.json.gz");
        new SaveGame("Test", null, 1, levelInfo, List.of(), List.of(), List.of(), List.of(), new DefaultEntityData()).write(file);
        ILevelInfo loaded = levelInfo();
        SaveGame.read(file).restoreLevelInfo(loaded);

        assertEquals(7, loaded.getFlag(5));
        assertEquals(-3, loaded.getFlag(ILevelInfo.FLAG_COUNT - 1));
        assertEquals(0, loaded.getFlag(6));
        assertTrue(loaded.getTimer(2).isActive());
        assertEquals(12.5f, loaded.getTimer(2).getTime());
        assertFalse(loaded.getTimer(ILevelInfo.TIMER_COUNT - 1).isActive());
        assertEquals(40, loaded.getTimer(ILevelInfo.TIMER_COUNT - 1).getTime());
        assertFalse(loaded.getTimer(3).isActive());
        assertEquals(600, loaded.getTimeLimit());
        assertEquals(42, loaded.getLevelScore());
    }

    @Test
    void testNoTimeLimitStaysOff() throws Exception {
        Path file = folder.resolve("level.json.gz");
        new SaveGame("Test", null, 1, levelInfo(), List.of(), List.of(), List.of(), List.of(), new DefaultEntityData()).write(file);
        ILevelInfo loaded = levelInfo();
        SaveGame.read(file).restoreLevelInfo(loaded);

        assertNull(loaded.getTimeLimit());
    }

    @Test
    void testCampaignLevel() throws Exception {
        Path campaign = folder.resolve("campaign.json.gz");
        new SaveGame("Level6a", new CampaignLevel(CampaignLevel.LevelType.Level, 6, "a"), 1, levelInfo(), List.of(), List.of(), List.of(),
                List.of(), new DefaultEntityData()).write(campaign);
        CampaignLevel level = SaveGame.read(campaign).getCampaignLevel();
        assertEquals(CampaignLevel.LevelType.Level, level.getType());
        assertEquals(6, level.getLevel());
        assertEquals("a", level.getVariation());
        assertEquals("Level6a", SaveGame.readLevel(campaign));

        Path skirmish = folder.resolve("skirmish.json.gz");
        new SaveGame("Level6a", null, 1, levelInfo(), List.of(), List.of(), List.of(), List.of(), new DefaultEntityData()).write(skirmish);
        assertNull(SaveGame.read(skirmish).getCampaignLevel());
    }

    @Test
    void testTriggersRoundTrip() throws Exception {
        double[] time = {0};
        ILevelInfo levelInfo = levelInfo();
        TriggerControl triggerControl = new TriggerControl(null, levelInfo, () -> time[0], null, null, 1);

        // Part way through: the actions that run once have run, the repeating trigger is next
        for (int tick = 0; tick < 5; tick++) {
            triggerControl.update(0.5f);
            time[0] += 0.5;
        }
        assertEquals(1, levelInfo.getFlag(0));
        assertEquals(0, levelInfo.getFlag(1));
        assertEquals(5, levelInfo.getFlag(2));

        Path file = folder.resolve("triggers.json.gz");
        new SaveGame("Test", null, time[0], levelInfo, List.of(), List.of(triggerControl), List.of(), List.of(), new DefaultEntityData()).write(file);
        SaveGame saveGame = SaveGame.read(file);
        ILevelInfo loadedLevelInfo = levelInfo();
        TriggerControl loaded = new TriggerControl(null, loadedLevelInfo, () -> time[0], null, null, 1);
        saveGame.restoreLevelInfo(loadedLevelInfo);
        saveGame.restoreTriggers(List.of(loaded));

        // Both carry on the same way, the once only actions don't run again
        for (int tick = 0; tick < 6; tick++) {
            triggerControl.update(0.5f);
            loaded.update(0.5f);
            time[0] += 0.5;
        }
        for (int flag = 0; flag < 3; flag++) {
            assertEquals(levelInfo.getFlag(flag), loadedLevelInfo.getFlag(flag), "Flag " + flag);
        }
        assertEquals(2, loadedLevelInfo.getFlag(1));

        Path first = folder.resolve("first.json.gz");
        new SaveGame("Test", null, time[0], levelInfo, List.of(), List.of(triggerControl), List.of(), List.of(), new DefaultEntityData()).write(first);
        Path second = folder.resolve("second.json.gz");
        new SaveGame("Test", null, time[0], loadedLevelInfo, List.of(), List.of(loaded), List.of(), List.of(), new DefaultEntityData()).write(second);
        assertEquals(readJson(first).get("triggers"), readJson(second).get("triggers"));
    }

    @Test
    void testPlayerStatsRoundTrip() throws Exception {
        Creature imp = ShotFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        Creature goblin = ShotFixtures.creature((short) 4, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        PlayerStatsControl stats = new PlayerStatsControl();
        stats.creatureSlapped(imp);
        stats.creatureSlapped(imp);
        stats.creaturePickedUp(goblin);
        Keeper keeper = new Keeper(false, (short) 3);
        IPlayerController player = stub(IPlayerController.class, Map.of("getKeeper", keeper, "getStatsControl", stats));

        Path file = folder.resolve("stats.json.gz");
        new SaveGame("Test", null, 1, levelInfo(keeper), List.of(player), List.of(), List.of(), List.of(), new DefaultEntityData()).write(file);
        SaveGame saveGame = SaveGame.read(file);
        PlayerStatsControl loaded = new PlayerStatsControl();
        saveGame.restorePlayerStats(List.of(stub(IPlayerController.class, Map.of("getKeeper", keeper, "getStatsControl", loaded))));

        assertTrue(loaded.hasSlapped(imp));
        assertFalse(loaded.hasSlapped(goblin));
        assertTrue(loaded.hasPickedUp(goblin));
        assertFalse(loaded.hasDropped());

        // The counts too
        Path secondFile = folder.resolve("second.json.gz");
        IPlayerController loadedPlayer = stub(IPlayerController.class, Map.of("getKeeper", keeper, "getStatsControl", loaded));
        new SaveGame("Test", null, 1, levelInfo(keeper), List.of(loadedPlayer), List.of(), List.of(), List.of(), new DefaultEntityData()).write(secondFile);
        assertEquals(readJson(file).get("playerStats"), readJson(secondFile).get("playerStats"));
    }

    @Test
    void testPartiesRoundTrip() throws Exception {
        Thing.HeroParty heroParty = HeroPartyFixtures.heroParty((short) 2, HeroPartyFixtures.member(),
                HeroPartyFixtures.member(Thing.Creature.CreatureFlag.LEADER), HeroPartyFixtures.member());
        PartyController party = new PartyController(heroParty);
        party.setType(PartyType.INVASION_PARTY);
        party.create();
        List<Thing.GoodCreature> members = heroParty.getHeroPartyMembers();
        for (int i = 0; i < members.size(); i++) {
            party.addMemberInstance(members.get(i), creature(new EntityId(10 + i)));
        }

        Path file = folder.resolve("parties.json.gz");
        new SaveGame("Test", null, 1, levelInfo(), List.of(), List.of(), List.of(party), List.of(), new DefaultEntityData()).write(file);
        List<PartyController> loaded = SaveGame.read(file).getParties(Map.of(heroParty.getId(), heroParty), (entityId, member) -> creature(entityId));

        assertEquals(1, loaded.size());
        PartyController loadedParty = loaded.get(0);
        assertTrue(loadedParty.isCreated());
        assertEquals(party.getId(), loadedParty.getId());
        assertEquals(heroParty.getId(), loadedParty.getPartyId());
        assertEquals(PartyType.INVASION_PARTY, loadedParty.getType());
        assertEquals(new EntityId(11), loadedParty.getLeader().getEntityId());
        assertEquals(List.of(new EntityId(10), new EntityId(11), new EntityId(12)),
                loadedParty.getActualMembers().stream().map(ICreatureController::getEntityId).toList());

        PartyController newParty = new PartyController(heroParty);
        newParty.create();
        assertTrue(newParty.getId() > loadedParty.getId(), "New parties must not reuse saved ids");
    }

    private static ICreatureController creature(EntityId entityId) {
        return stub(ICreatureController.class, Map.of("getEntityId", entityId));
    }

    /**
     * A level info with the given players, the script variables are kept in
     * arrays like {@code GameController} does
     */
    private static ILevelInfo levelInfo(Keeper... players) {
        Map<Short, Keeper> playersById = new TreeMap<>();
        for (Keeper player : players) {
            playersById.put(player.getId(), player);
        }
        int[] flags = new int[ILevelInfo.FLAG_COUNT];
        GameTimer[] timers = new GameTimer[ILevelInfo.TIMER_COUNT];
        Arrays.setAll(timers, i -> new GameTimer());
        Object[] timeLimitAndScore = {null, 0};
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getLevelData" -> {
                    return LEVEL;
                }
                case "getPlayers" -> {
                    return playersById;
                }
                case "getFlag" -> {
                    return flags[(int) args[0]];
                }
                case "setFlag" ->
                    flags[(int) args[0]] = (int) args[1];
                case "getTimer" -> {
                    return timers[(int) args[0]];
                }
                case "getTimeLimit" -> {
                    return timeLimitAndScore[0];
                }
                case "setTimeLimit" ->
                    timeLimitAndScore[0] = args[0];
                case "getLevelScore" -> {
                    return timeLimitAndScore[1];
                }
                case "setLevelScore" ->
                    timeLimitAndScore[1] = args[0];
                default ->
                    throw new UnsupportedOperationException(method.getName());
            }
            return null;
        });
    }

    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> values.get(method.getName())));
    }

    private static JsonObject readJson(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Makes a value of the type with every field set, so a field that doesn't
     * survive the save shows up as a difference
     */
    private static Object sample(Class<?> type, Type genericType, int depth) throws ReflectiveOperationException {
        if (type == boolean.class || type == Boolean.class) {
            return true;
        } else if (type == byte.class || type == Byte.class) {
            return (byte) 3;
        } else if (type == short.class || type == Short.class) {
            return (short) 3;
        } else if (type == int.class || type == Integer.class) {
            return 3;
        } else if (type == long.class || type == Long.class) {
            return 3L;
        } else if (type == float.class || type == Float.class) {
            return 1.5f;
        } else if (type == double.class || type == Double.class) {
            return 2.5;
        } else if (type == char.class || type == Character.class) {
            return 'k';
        } else if (type == String.class) {
            return "text";
        } else if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length > 0 ? constants[constants.length - 1] : null;
        } else if (type == EntityId.class) {
            return new EntityId(42);
        } else if (type.isArray()) {
            Object array = Array.newInstance(type.getComponentType(), 2);
            for (int i = 0; i < 2; i++) {
                Array.set(array, i, sample(type.getComponentType(), type.getComponentType(), depth + 1));
            }
            return array;
        } else if (Collection.class.isAssignableFrom(type)) {
            Collection<Object> collection = Set.class.isAssignableFrom(type) ? new HashSet<>() : new ArrayList<>();
            Type elementType = getTypeArgument(genericType, 0);
            collection.add(sample(getRawType(elementType), elementType, depth + 1));
            return collection;
        } else if (Map.class.isAssignableFrom(type)) {
            Type keyType = getTypeArgument(genericType, 0);
            Type valueType = getTypeArgument(genericType, 1);
            Map<Object, Object> map = new HashMap<>();
            map.put(sample(getRawType(keyType), keyType, depth + 1), sample(getRawType(valueType), valueType, depth + 1));
            return map;
        } else if (type.isInterface() || Modifier.isAbstract(type.getModifiers()) || type == Object.class || depth > 3) {
            return null;
        }

        Constructor<?> constructor = Stream.of(type.getDeclaredConstructors())
                .min(Comparator.comparingInt(Constructor::getParameterCount)).orElseThrow();
        constructor.setAccessible(true);
        Object[] arguments = new Object[constructor.getParameterCount()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = sample(constructor.getParameterTypes()[i], constructor.getGenericParameterTypes()[i], depth + 1);
        }
        Object value = constructor.newInstance(arguments);
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !field.isSynthetic()) {
                    field.setAccessible(true);
                    field.set(value, sample(field.getType(), field.getGenericType(), depth + 1));
                }
            }
        }
        return value;
    }

    private static Type getTypeArgument(Type type, int index) {
        return type instanceof ParameterizedType parameterized ? parameterized.getActualTypeArguments()[index] : Object.class;
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        } else if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        } else if (type instanceof WildcardType wildcard) {
            return getRawType(wildcard.getUpperBounds()[0]);
        }
        return Object.class;
    }

}

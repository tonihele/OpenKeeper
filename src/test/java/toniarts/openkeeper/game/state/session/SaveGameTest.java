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
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Writes save games and reads them back
 */
class SaveGameTest {

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
        keeper.setGold(1500);
        keeper.setDungeonHeartLocation(new Point(10, 12));

        Path file = folder.resolve("saves").resolve("Level6a.json.gz");
        new SaveGame("Level6a", 1234.5, List.of(keeper), entityData).write(file);
        SaveGame saveGame = SaveGame.read(file);
        EntityData loaded = saveGame.createEntityData();

        JsonObject json = readJson(file);
        assertEquals(SaveGame.VERSION, json.get("version").getAsInt());
        assertEquals("WANDER", json.getAsJsonObject("components").getAsJsonObject("CreatureAi")
                .getAsJsonObject(Long.toString(creature.getId())).get("creatureState").getAsString(), "States are saved by name");

        assertEquals("Level6a", saveGame.getLevel());
        assertEquals(1234.5, saveGame.getGameTime());
        assertEquals(1, saveGame.getPlayers().size());
        assertEquals(1500, saveGame.getPlayers().get(0).getGold());
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
        new SaveGame("Test", 1, List.of(new Keeper(false, (short) 3)), entityData).write(file);
        SaveGame saveGame = SaveGame.read(file);
        Path secondFile = folder.resolve("second.json.gz");
        new SaveGame(saveGame.getLevel(), saveGame.getGameTime(), saveGame.getPlayers(), saveGame.createEntityData()).write(secondFile);

        JsonObject first = readJson(file);
        JsonObject second = readJson(secondFile);
        assertEquals(componentTypes, first.getAsJsonObject("components").size(), "Every component is saved");
        assertEquals(first, second);
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

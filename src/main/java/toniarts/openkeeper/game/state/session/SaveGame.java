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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.base.DefaultEntityIdGenerator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.chicken.ChickenState;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;

/**
 * A saved game: the level, the game time, the players and every entity
 * component, written as gzipped JSON. The entity ids are kept as they are, so
 * the ids inside the components stay valid. Rooms, indexes, tasks and the
 * state machines are not saved, they are built again from the components on
 * load.
 */
public final class SaveGame {

    /**
     * Version of the file layout, increase it when the layout changes
     */
    public static final int VERSION = 1;

    private static final String COMPONENT_PACKAGE = Position.class.getPackageName();
    private static final Map<String, Class<? extends EntityComponent>> COMPONENT_TYPES = findComponentTypes();
    private static final Type PLAYERS_TYPE = new TypeToken<List<Keeper>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapterFactory(new EnumNames<>(CreatureAi.class, CreatureState.class, "creatureState", "targetState"))
            .registerTypeAdapterFactory(new EnumNames<>(ChickenAi.class, ChickenState.class, "chickenState"))
            .create();

    private final JsonObject json;

    /**
     * Takes a snapshot of the game. Nothing may change the entities or the
     * players while this runs. Some systems change component values in place
     * (the position vector), so the snapshot is turned into JSON right away,
     * and the file can then be written on another thread. One entity id is
     * used up to find the next free id.
     *
     * @param level the level name
     * @param gameTime the game time in seconds
     * @param players the players
     * @param entityData the entities
     */
    public SaveGame(String level, double gameTime, Collection<Keeper> players, EntityData entityData) {
        json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("level", level);
        json.addProperty("gameTime", gameTime);
        json.addProperty("nextEntityId", entityData.createEntity().getId());
        json.add("players", GSON.toJsonTree(players, PLAYERS_TYPE));

        JsonObject components = new JsonObject();
        for (Map.Entry<String, Class<? extends EntityComponent>> type : COMPONENT_TYPES.entrySet()) {
            JsonObject entities = new JsonObject();
            entityData.findEntities(null, type.getValue()).stream().sorted(Comparator.comparingLong(EntityId::getId)).forEach(entityId
                    -> entities.add(Long.toString(entityId.getId()), GSON.toJsonTree(entityData.getComponent(entityId, type.getValue()), type.getValue())));
            if (entities.size() > 0) {
                components.add(type.getKey(), entities);
            }
        }
        json.add("components", components);
    }

    private SaveGame(JsonObject json) {
        this.json = json;
    }

    /**
     * Reads a save game
     *
     * @param file the save file
     * @return the save game
     * @throws IOException if the file can't be read, is from a newer version
     * or has an unknown component
     */
    public static SaveGame read(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.get("version").getAsInt();
            if (version > VERSION) {
                throw new IOException("Save game version " + version + " is newer than " + VERSION + "!");
            }
            for (String type : json.getAsJsonObject("components").keySet()) {
                if (!COMPONENT_TYPES.containsKey(type)) {
                    throw new IOException("Unknown component " + type + "!");
                }
            }

            return new SaveGame(json);
        }
    }

    /**
     * Writes the save game. The file is first written beside the target and
     * then moved over it, so a crash while saving doesn't break an earlier
     * save.
     *
     * @param file the save file
     * @throws IOException if the file can't be written
     */
    public void write(Path file) throws IOException {
        Path folder = file.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path tempFile = folder.resolve(file.getFileName() + ".tmp");
        try (JsonWriter out = GSON.newJsonWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(tempFile)), StandardCharsets.UTF_8))) {
            GSON.toJson(json, out);
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Creates new entity data with the saved entities. New entities get ids
     * after the saved ones.
     *
     * @return the entity data
     */
    public EntityData createEntityData() {
        EntityData entityData = new DefaultEntityData(new DefaultEntityIdGenerator(json.get("nextEntityId").getAsLong()));
        for (Map.Entry<String, JsonElement> type : json.getAsJsonObject("components").entrySet()) {
            Class<? extends EntityComponent> componentType = COMPONENT_TYPES.get(type.getKey());
            for (Map.Entry<String, JsonElement> entity : type.getValue().getAsJsonObject().entrySet()) {
                entityData.setComponent(new EntityId(Long.parseLong(entity.getKey())), GSON.fromJson(entity.getValue(), componentType));
            }
        }
        return entityData;
    }

    /**
     * Gets the level name
     *
     * @return the level name
     */
    public String getLevel() {
        return json.get("level").getAsString();
    }

    /**
     * Gets the game time
     *
     * @return the game time in seconds
     */
    public double getGameTime() {
        return json.get("gameTime").getAsDouble();
    }

    /**
     * Gets the players
     *
     * @return new copies of the players
     */
    public List<Keeper> getPlayers() {
        return GSON.fromJson(json.get("players"), PLAYERS_TYPE);
    }

    /**
     * Finds the entity components, every class in the component package
     *
     * @return the component types by their simple names
     */
    private static Map<String, Class<? extends EntityComponent>> findComponentTypes() {
        try {
            Path classes = Paths.get(Position.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (FileSystem jar = Files.isDirectory(classes) ? null : FileSystems.newFileSystem(classes);
                    Stream<Path> files = Files.list((jar != null ? jar.getPath("/") : classes).resolve(COMPONENT_PACKAGE.replace('.', '/')))) {
                Map<String, Class<? extends EntityComponent>> types = new TreeMap<>();
                for (Path file : files.toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".class") && !fileName.contains("$")) {
                        Class<?> type = Class.forName(COMPONENT_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length()));
                        if (EntityComponent.class.isAssignableFrom(type) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                            types.put(type.getSimpleName(), type.asSubclass(EntityComponent.class));
                        }
                    }
                }
                return types;
            }
        } catch (IOException | URISyntaxException | ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to find the entity components!", ex);
        }
    }

    /**
     * Writes enum ordinals of a component by name, so the save survives
     * changes to the enum. The components keep the ordinals for the network
     * serializer.
     */
    private static final class EnumNames<C, E extends Enum<E>> implements TypeAdapterFactory {

        private final Class<C> componentType;
        private final Class<E> enumType;
        private final String[] fields;

        public EnumNames(Class<C> componentType, Class<E> enumType, String... fields) {
            this.componentType = componentType;
            this.enumType = enumType;
            this.fields = fields;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != componentType) {
                return null;
            }

            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
            E[] constants = enumType.getEnumConstants();
            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonObject json = delegate.toJsonTree(value).getAsJsonObject();
                    for (String field : fields) {
                        int ordinal = json.get(field).getAsInt();
                        if (ordinal >= 0 && ordinal < constants.length) {
                            json.addProperty(field, constants[ordinal].name());
                        }
                    }
                    elements.write(out, json);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonObject json = elements.read(in).getAsJsonObject();
                    for (String field : fields) {
                        JsonElement value = json.get(field);
                        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            json.addProperty(field, Enum.valueOf(enumType, value.getAsString()).ordinal());
                        }
                    }
                    return delegate.fromJsonTree(json);
                }
            }.nullSafe();
        }
    }

}

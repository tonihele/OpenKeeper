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
package toniarts.openkeeper.game.network;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks the serializer registrations of {@link NetworkServer}
 */
class NetworkServerTest {

    @Test
    void testEveryComponentIsRegistered() throws Exception {
        // Touching the Serializer first initializes it before FieldSerializer,
        // the other way round its default registrations fail
        Serializer.getExactSerializerRegistration(EntityId.class);
        NetworkServer.initialize();
        String componentPackage = Position.class.getPackageName();
        Path classes = Paths.get(Position.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assumeTrue(Files.isDirectory(classes), "Components are not in a folder");

        Set<String> registered = Serializer.getSerializerRegistrations().stream()
                .map(SerializerRegistration::getType).map(Class::getName).collect(Collectors.toSet());
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.list(classes.resolve(componentPackage.replace('.', '/')))) {
            for (Path classFile : files.toList()) {
                String fileName = classFile.getFileName().toString();
                if (!fileName.endsWith(".class") || fileName.contains("$")) {
                    continue;
                }
                Class<?> type = Class.forName(componentPackage + "." + fileName.substring(0, fileName.length() - 6));
                if (EntityComponent.class.isAssignableFrom(type) && !type.isInterface()
                        && !Modifier.isAbstract(type.getModifiers()) && !registered.contains(type.getName())) {
                    missing.add(type.getSimpleName());
                }
            }
        }

        assertTrue(missing.isEmpty(), "Components not registered in NetworkServer: " + missing);
    }
}

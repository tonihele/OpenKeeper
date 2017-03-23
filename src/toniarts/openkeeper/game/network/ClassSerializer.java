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
package toniarts.openkeeper.game.network;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.network.message.*;

/**
 *
 * @author ArchDemon
 */
public class ClassSerializer {

    private static final Logger logger = Logger.getLogger(ClassSerializer.class.getName());

    private static final Class[] classes = {
        MessageTime.class,
        MessagePlayerInfo.class,
        MessageServerInfo.class
    };

    private static final Class[] entities = {};

    public static void initialize() {

        try {
            Serializer.registerClasses(classes);
        } catch (Exception ex) {
        }

        // Register these manually since Spider Monkey currently
        // requires them all to have @Serializable but we already know
        // which serializer we want to use.  Eventually I will fix SM
        // but for now I'll do this here.
        Serializer fieldSerializer = new FieldSerializer();
        boolean error = false;
        for (Class c : entities) {
            try {
                Serializer.registerClass(c, fieldSerializer);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error registering class:" + c, e);
                error = true;
            }
        }
        if (error) {
            throw new RuntimeException("Some classes failed to register");
        }
    }
}

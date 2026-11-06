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
package toniarts.openkeeper.benchmark;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkJsonTest {

    @Test
    void serializesNestedMapsAndArrays() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 1);
        root.put("name", "openkeeper");
        root.put("enabled", true);
        root.put("nested", Map.of("avgFps", 60.5));
        root.put("list", List.of("a", 2));

        String json = BenchmarkJson.toJson(root);
        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"name\": \"openkeeper\""));
        assertTrue(json.contains("\"enabled\": true"));
        assertTrue(json.contains("\"avgFps\": 60.5"));
        assertTrue(json.contains("\"a\""));
    }
}

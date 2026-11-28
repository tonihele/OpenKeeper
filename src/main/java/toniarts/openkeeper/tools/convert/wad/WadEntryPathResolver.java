/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.wad;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Resolves archive entry names while keeping extraction inside its destination.
 */
final class WadEntryPathResolver {

    private WadEntryPathResolver() {
    }

    static Path resolve(Path destination, String entryName) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        Path resolved = destination.resolve(entryName).normalize();
        Path absoluteResolved = resolved.toAbsolutePath().normalize();
        if (absoluteResolved.equals(root) || !absoluteResolved.startsWith(root)) {
            throw new IOException("WAD entry path escapes destination: " + entryName);
        }
        return resolved;
    }
}

/*
 * Copyright (C) 2014-2021 OpenKeeper
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
package toniarts.openkeeper.tools.convert;

import java.io.IOException;

/**
 *
 * @author archdemon
 */
public interface ISeekableResourceReader extends IResourceReader {

    void seek(long pos) throws IOException;

    long getFilePointer() throws IOException;

    /**
     * End of file
     *
     * @return true if file pointer >= length of file
     * @throws IOException
     */
    boolean isEof() throws IOException;

    long length() throws IOException;

    
}

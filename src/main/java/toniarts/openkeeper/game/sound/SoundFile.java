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
package toniarts.openkeeper.game.sound;

/**
 *
 * @author archdemon
 */
public final class SoundFile implements Comparable<SoundFile> {

    private final int id;
    private final SoundGroup group;
    private final String filename;
    private final int pcmBytes;
    private final int weight;

    public SoundFile(SoundGroup action, int id, String filename) {
        this(action, id, filename, 0, 0);
    }

    /**
     * @param action the group
     * @param id 0-based entry id in the SDT file
     * @param filename the converted file name
     * @param pcmBytes length of the sound as 16 bit PCM in bytes, 0 if
     * unknown
     * @param weight cumulative selection weight within the owning entry
     */
    public SoundFile(SoundGroup action, int id, String filename, int pcmBytes, int weight) {
        this.group = action;
        this.id = id;
        this.filename = filename;
        this.pcmBytes = pcmBytes;
        this.weight = weight;
    }

    public SoundGroup getGroup() {
        return group;
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Length of the decoded sound in bytes. The MPEG data decodes into slightly
     * more samples (padded to whole frames). Sequenced sounds such as the music
     * must be cut to exactly this length to join seamlessly.
     *
     * @return length as 16 bit PCM in bytes, 0 if unknown
     */
    public int getPcmBytes() {
        return pcmBytes;
    }

    /**
     * @return cumulative selection weight (0-65535) within the owning entry
     */
    public int getWeight() {
        return weight;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.id;
        hash = 97 * hash + this.group.getId();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SoundFile other = (SoundFile) obj;
        if (this.id != other.id) {
            return false;
        }
        if (this.group.getId() != other.group.getId()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        //return String.format("%s", filename);
        return String.format("%d %d %s", group.getId(), id, filename);
    }

    @Override
    public int compareTo(SoundFile o) {
        if (group.getId() == o.group.getId()) {
            return (id - o.id);
        }
        return (group.getId() - o.group.getId());
    }
}

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
package toniarts.openkeeper.game.sound;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Legacy view of a {@link SoundEvent} of a category, flattening the event's
 * primary group and every group reachable through property links into a single
 * list of playable files.
 */
public final class SoundGroup {

    private final SoundCategory category;
    private final SoundEvent event;
    private final List<SoundFile> files = new ArrayList<>();

    public SoundGroup(SoundCategory category, SoundEvent event) {
        this.category = category;
        this.event = event;

        parseFiles();
    }

    public int getId() {
        return event.getId();
    }

    public SoundCategory getCategory() {
        return category;
    }

    public SoundEvent getEvent() {
        return event;
    }

    public List<SoundFile> getFiles() {
        return files;
    }

    public void addFile(SoundFile file) {
        files.add(file);
    }

    /**
     * The group that is played when no property is supplied.
     */
    @Nullable
    public SoundEventGroup getPrimaryGroup() {
        return event.getGroup(1);
    }

    private void parseFiles() {
        for (SoundEventGroup eventGroup : collectGroups()) {
            for (SoundSample sample : eventGroup.getSamples()) {
                String filename = sample.getArchive() == null ? null : sample.getArchive().getClipAssetPathNative(
                        sample.getClip());
                if (filename == null) {
                    continue;
                }
                files.add(new SoundFile(this, sample.getClip(), filename, eventGroup, sample));
            }
        }
    }

    private List<SoundEventGroup> collectGroups() {
        SoundEventGroup primary = getPrimaryGroup();
        if (primary == null) {
            return Collections.emptyList();
        }
        if (!event.hasPropertyLinks()) {
            return primary.getSamples().isEmpty() ? Collections.emptyList() : Collections.singletonList(primary);
        }
        return primary.resolve();
    }

    @Override
    public String toString() {
        return "SoundGroup{" + "typeId=" + event.getId() + '}';
    }
}

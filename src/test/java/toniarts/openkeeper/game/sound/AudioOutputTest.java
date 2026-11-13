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

import com.jme3.system.AppSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.SOFTHRTF.*;
import static org.lwjgl.openal.SOFTOutputMode.*;
import toniarts.openkeeper.game.data.Settings;

class AudioOutputTest {
    @ParameterizedTest
    @EnumSource(AudioOutputMode.class)
    void preferenceSurvivesAppSettingsSerialization(AudioOutputMode mode) throws Exception {
        AppSettings saved = new AppSettings(false);
        saved.putString(Settings.Setting.AUDIO_OUTPUT_MODE.getKey(), mode.name());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        saved.save(bytes);
        AppSettings loaded = new AppSettings(false);
        loaded.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(mode, AudioOutputMode.fromSetting(loaded.get(Settings.Setting.AUDIO_OUTPUT_MODE.getKey())));
    }

    @Test
    void missingOrUnknownPreferencesUseAuto() {
        assertEquals(AudioOutputMode.AUTO, AudioOutputMode.fromSetting(null));
        assertEquals(AudioOutputMode.AUTO, AudioOutputMode.fromSetting("INVALID"));
        assertEquals(AudioOutputMode.AUTO, AudioOutputMode.fromSetting(5));
    }

    @Test
    void switchingAwayFromHeadphonesRestoresAutomaticChannelLayout() {
        assertArrayEquals(new int[]{ALC_HRTF_SOFT, ALC_TRUE, ALC_OUTPUT_MODE_SOFT, ALC_STEREO_HRTF_SOFT, 0},
                AudioOutput.attributes(AudioOutputMode.HEADPHONES, true));
        assertArrayEquals(new int[]{ALC_HRTF_SOFT, ALC_FALSE, ALC_OUTPUT_MODE_SOFT, ALC_ANY_SOFT, 0},
                AudioOutput.attributes(AudioOutputMode.SPEAKERS, true));
        assertArrayEquals(new int[]{ALC_HRTF_SOFT, ALC_DONT_CARE_SOFT, ALC_OUTPUT_MODE_SOFT, ALC_ANY_SOFT, 0},
                AudioOutput.attributes(AudioOutputMode.AUTO, true));
        assertArrayEquals(new int[]{ALC_HRTF_SOFT, ALC_TRUE, 0},
                AudioOutput.attributes(AudioOutputMode.HEADPHONES, false));
    }

    @Test
    void unsupportedBackendKeepsWorkingAndDoesNotCallMissingExtensions() {
        FakeDevice device = new FakeDevice(); device.supported = false;
        assertTrue(AudioOutput.apply(AudioOutputMode.AUTO, device).applied());
        assertFalse(AudioOutput.apply(AudioOutputMode.HEADPHONES, device).applied());
        assertFalse(AudioOutput.apply(AudioOutputMode.SPEAKERS, device).applied());
        assertEquals(0, device.resets);
    }

    @Test
    void requestedHrtfMustActuallyBeEnabledToReportSuccess() {
        FakeDevice device = new FakeDevice();
        assertFalse(AudioOutput.apply(AudioOutputMode.HEADPHONES, device).applied());
        device.enabled = true;
        assertTrue(AudioOutput.apply(AudioOutputMode.HEADPHONES, device).applied());
        assertEquals(AudioOutputMode.HEADPHONES, device.mode);
    }

    @Test
    void deviceRequiringHrtfReportsThatSpeakerRequestWasNotHonored() {
        FakeDevice device = new FakeDevice(); device.enabled = true;
        assertFalse(AudioOutput.apply(AudioOutputMode.SPEAKERS, device).applied());
        assertTrue(AudioOutput.apply(AudioOutputMode.AUTO, device).applied());
        device.enabled = false;
        assertTrue(AudioOutput.apply(AudioOutputMode.SPEAKERS, device).applied());
    }

    @Test
    void failedResetDoesNotPretendToApplyPreference() {
        FakeDevice device = new FakeDevice(); device.resetSucceeded = false;
        assertFalse(AudioOutput.apply(AudioOutputMode.HEADPHONES, device).applied());
        assertEquals(0, device.queries);
    }

    private static final class FakeDevice implements AudioOutput.Device {
        boolean supported = true, resetSucceeded = true, enabled;
        int resets, queries;
        AudioOutputMode mode;
        public boolean supportsHrtf() { return supported; }
        public boolean reset(AudioOutputMode mode) { resets++; this.mode = mode; return resetSucceeded; }
        public boolean hrtfEnabled() { queries++; return enabled; }
    }
}

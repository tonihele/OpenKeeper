package toniarts.openkeeper.game.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmbmSettingsTest {

    @Test
    void embmIsEnabledForNewSettings() {
        assertEquals(Boolean.TRUE, Settings.Setting.EMBM_ENABLED.getDefaultValue());
    }
}

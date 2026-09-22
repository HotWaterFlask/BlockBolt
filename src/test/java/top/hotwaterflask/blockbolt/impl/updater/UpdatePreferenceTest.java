package top.hotwaterflask.blockbolt.impl.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class UpdatePreferenceTest {

    @Test
    public void testParseNullAndEmpty() {
        assertFalse(UpdatePreference.parse(null).isPresent(), "Null string should return empty Optional without NPE");
        assertFalse(UpdatePreference.parse("").isPresent(), "Empty string should return empty Optional");
        assertFalse(UpdatePreference.parse("   ").isPresent(), "Whitespace-only string should return empty Optional");
    }

    @Test
    public void testParseValid() {
        assertEquals(Optional.of(UpdatePreference.AUTO_INSTALL), UpdatePreference.parse("auto_install"));
        assertEquals(Optional.of(UpdatePreference.AUTO_INSTALL), UpdatePreference.parse("AUTO INSTALL"));
        assertEquals(Optional.of(UpdatePreference.DISABLED), UpdatePreference.parse("disabled"));
        assertEquals(Optional.of(UpdatePreference.JUST_NOTIFY), UpdatePreference.parse("just notify"));
        assertEquals(Optional.of(UpdatePreference.JUST_NOTIFY), UpdatePreference.parse("JUST_NOTIFY"));
    }

    @Test
    public void testParseInvalid() {
        assertFalse(UpdatePreference.parse("unknown_option").isPresent());
    }
}

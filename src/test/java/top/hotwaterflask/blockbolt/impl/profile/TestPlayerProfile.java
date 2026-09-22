package top.hotwaterflask.blockbolt.impl.profile;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import top.hotwaterflask.blockbolt.ProfileFactory;
import top.hotwaterflask.blockbolt.Translator.Translation;
import top.hotwaterflask.blockbolt.group.CombinedGroupSystem;
import top.hotwaterflask.blockbolt.impl.JsonSecretSignEntry;
import top.hotwaterflask.blockbolt.profile.PlayerProfile;
import top.hotwaterflask.blockbolt.profile.Profile;

public class TestPlayerProfile {

    private ProfileFactoryImpl getProfileFactory() {
        return new ProfileFactoryImpl(new CombinedGroupSystem(), new NullTranslator());
    }

    @Test
    public void testIncludes() {
        ProfileFactoryImpl factory = getProfileFactory();
        UUID bobId = UUID.randomUUID();
        String everyoneTag = "[" + new NullTranslator().getWithoutColor(Translation.TAG_EVERYONE) + "]";

        Profile bob = factory.fromNameAndUniqueId("Bob", Optional.of(bobId));
        Profile bobRenamed = factory.fromNameAndUniqueId("Bob2", Optional.of(bobId));
        Profile jane = factory.fromNameAndUniqueId("Jane", Optional.of(UUID.randomUUID()));
        Profile janeWithoutId = factory.fromDisplayText("jane");
        Profile everyone = factory.fromDisplayText(everyoneTag);

        assertTrue(bob.includes(bobRenamed), "Same id");
        assertTrue(bobRenamed.includes(bob), "Same id");
        assertFalse(jane.includes(janeWithoutId), "Known id, not present in other");
        assertTrue(janeWithoutId.includes(jane), "Unknown id, same name");
        assertFalse(bob.includes(jane), "Different id and name");
        assertFalse(bob.includes(janeWithoutId), "Different id and name");

        // Everyone includes everyone, but is never included
        assertTrue(everyone.includes(bob));
        assertTrue(everyone.includes(jane));
        assertTrue(everyone.includes(janeWithoutId));
        assertFalse(bob.includes(everyone));
        assertFalse(jane.includes(everyone));
        assertFalse(janeWithoutId.includes(everyone));
    }

    @Test
    public void testNameAndId() {
        String name = "test";
        UUID uuid = UUID.randomUUID();
        ProfileFactory factory = getProfileFactory();
        Profile profile = factory.fromNameAndUniqueId(name, Optional.of(uuid));

        // Test object properties
        assertEquals(name, profile.getDisplayName());
        assertEquals(uuid, ((PlayerProfile) profile).getUniqueId().get());
    }

    @Test
    public void testNameAndIdJson() {
        String name = "test";
        UUID uuid = UUID.randomUUID();
        ProfileFactory factory = getProfileFactory();
        Profile profile = factory.fromNameAndUniqueId(name, Optional.of(uuid));
        JsonSecretSignEntry object = new JsonSecretSignEntry(new JsonObject());
        profile.getSaveObject(object);

        assertEquals(name, object.getString(PlayerProfileImpl.NAME_KEY).get());
        assertEquals(uuid, object.getUniqueId(PlayerProfileImpl.UUID_KEY).get());
    }

    @Test
    public void testPlayerProfileRoundtrip() {
        String name = "test";
        UUID uuid = UUID.randomUUID();
        ProfileFactoryImpl factory = getProfileFactory();
        Profile profile = factory.fromNameAndUniqueId(name, Optional.of(uuid));

        testRoundtrip(factory, profile);
    }

    private void testRoundtrip(ProfileFactoryImpl factory, Profile profile) {
        JsonSecretSignEntry object = new JsonSecretSignEntry(new JsonObject());
        profile.getSaveObject(object);
        Profile newProfile = factory.fromSavedObject(object).get();
        assertEquals(profile, newProfile);
    }

    @Test
    public void testWithoutId() {
        String name = "test";
        ProfileFactoryImpl factory = getProfileFactory();
        Profile profile = factory.fromDisplayText(name);

        assertEquals(name, profile.getDisplayName());
        assertFalse(((PlayerProfile) profile).getUniqueId().isPresent());
    }

    @Test
    public void testWithoutIdJson() {
        String name = "test";
        ProfileFactoryImpl factory = getProfileFactory();
        Profile profile = factory.fromDisplayText(name);
        JsonSecretSignEntry object = new JsonSecretSignEntry(new JsonObject());
        profile.getSaveObject(object);

        assertEquals(name, object.getString(PlayerProfileImpl.NAME_KEY).get());
        assertFalse(object.getUniqueId(PlayerProfileImpl.UUID_KEY).isPresent());
    }

    @Test
    public void testTimerProfileParsing() {
        ProfileFactoryImpl factory = getProfileFactory();
        String timerKey = new NullTranslator().getWithoutColor(Translation.TAG_TIMER);

        Profile timerSingle = factory.fromDisplayText("[" + timerKey + ":3]");
        assertTrue(timerSingle instanceof top.hotwaterflask.blockbolt.profile.TimerProfile);
        assertEquals(3, ((top.hotwaterflask.blockbolt.profile.TimerProfile) timerSingle).getOpenSeconds());

        Profile timerMulti = factory.fromDisplayText("[" + timerKey + ":15]");
        assertTrue(timerMulti instanceof top.hotwaterflask.blockbolt.profile.TimerProfile);
        assertEquals(15, ((top.hotwaterflask.blockbolt.profile.TimerProfile) timerMulti).getOpenSeconds());

        Profile timerWithSpace = factory.fromDisplayText("[" + timerKey + ": 30]");
        assertTrue(timerWithSpace instanceof top.hotwaterflask.blockbolt.profile.TimerProfile);
        assertEquals(30, ((top.hotwaterflask.blockbolt.profile.TimerProfile) timerWithSpace).getOpenSeconds());

        Profile timerInvalid = factory.fromDisplayText("[" + timerKey + ":abc]");
        assertTrue(timerInvalid instanceof top.hotwaterflask.blockbolt.profile.TimerProfile);
        assertEquals(-1, ((top.hotwaterflask.blockbolt.profile.TimerProfile) timerInvalid).getOpenSeconds());
    }
}

package top.hotwaterflask.blockbolt.impl.profile;

import java.util.Date;

import top.hotwaterflask.blockbolt.SecretSignEntry;
import top.hotwaterflask.blockbolt.profile.Profile;
import top.hotwaterflask.blockbolt.profile.TimerProfile;

class TimerProfileImpl implements TimerProfile {

    static final String TIME_KEY = "t";

    private final int seconds;
    private final String timerTag;

    TimerProfileImpl(String timerTag, int secondsOpen) {
        this.timerTag = timerTag;
        this.seconds = Math.max(-1, secondsOpen);
    }

    @Override
    public String getDisplayName() {
        return "[" + timerTag + ":" + seconds + "]";
    }

    @Override
    public int getOpenSeconds() {
        return seconds;
    }

    @Override
    public void getSaveObject(SecretSignEntry entry) {
        entry.setInteger(TIME_KEY, seconds);
    }

    @Override
    public boolean includes(Profile other) {
        // Includes nobody
        return false;
    }

    @Override
    public boolean isExpired(Date cutoffDate) {
        // These never expire
        return false;
    }

}

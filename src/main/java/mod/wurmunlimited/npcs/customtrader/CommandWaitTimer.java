package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.TimeConstants;

class CommandWaitTimer {
    private final long waitTime;
    long lastTimeUsed = 0;

    CommandWaitTimer(long waitTime) {
        this.waitTime = waitTime;
    }

    String timeRemaining() {
        long nextTime = lastTimeUsed + waitTime;
        long currentTime = System.currentTimeMillis();
        if (nextTime < currentTime) {
            return "";
        }

        long seconds = (nextTime - currentTime) / TimeConstants.SECOND_MILLIS;

        if (seconds <= 2) {
            return "about 1 second";
        }

        return seconds + " seconds";
    }

    void reset() {
        lastTimeUsed = System.currentTimeMillis();
    }
}

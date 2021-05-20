package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.TimeConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static mod.wurmunlimited.GenericAsserts.nearlyEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandWaitTimerTests {
    private

    @Test
    void testWaitTimeInitialEmpty() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);

        assertTrue(timer.timeRemaining().isEmpty());
    }
    @Test
    void testWaitTimeReset() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);

        assert timer.lastTimeUsed == 0;
        timer.reset();
        assertThat(System.currentTimeMillis(), nearlyEquals(timer.lastTimeUsed));
    }

    @Test
    void testWaitTimeNotElapsed() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);
        timer.reset();

        String[] result = timer.timeRemaining().split(" ");
        assertEquals(2, result.length, Arrays.toString(result));
        assertThat(Long.parseLong(result[0]), nearlyEquals(59L));
        assertEquals("seconds", result[1]);
    }

    @Test
    void testWaitTimePartiallyElapsed() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);
        timer.lastTimeUsed = System.currentTimeMillis() - (TimeConstants.MINUTE_MILLIS / 2);

        String[] result = timer.timeRemaining().split(" ");
        assertEquals(2, result.length, timer.timeRemaining());
        assertThat(Long.parseLong(result[0]), nearlyEquals(29L));
        assertEquals("seconds", result[1]);
    }

    @Test
    void testWaitTimeAboutOneSecondLeft() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);
        timer.lastTimeUsed = System.currentTimeMillis() - (TimeConstants.MINUTE_MILLIS - 1000);

        assertEquals("about 1 second", timer.timeRemaining());
    }

    @Test
    void testWaitTimeElapsed() {
        CommandWaitTimer timer = new CommandWaitTimer(TimeConstants.MINUTE_MILLIS);
        timer.lastTimeUsed = System.currentTimeMillis() - (TimeConstants.MINUTE_MILLIS + 1);

        assertEquals("", timer.timeRemaining());
    }
}

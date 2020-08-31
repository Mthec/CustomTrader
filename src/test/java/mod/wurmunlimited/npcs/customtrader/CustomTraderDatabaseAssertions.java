package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CustomTraderDatabaseAssertions {
    private static void execute(CustomTraderDatabase.Execute execute) {
        try {
            ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("execute", CustomTraderDatabase.Execute.class), execute);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class IsInDb extends TypeSafeMatcher<Creature> {
        private Creature trader;
        private int answer;

        protected boolean matchesSafely(Creature trader) {
            this.trader = trader;

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT COUNT(*) FROM traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                answer = rs.getInt(1);
            });

            return answer == 1;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" trader with id " + trader.getWurmId());
        }

        @Override
        public void describeMismatchSafely(Creature trader, Description description) {
            description.appendText(" received " + answer + " entries");
        }
    }

    public static Matcher<Creature> isInDb() {
        return new IsInDb();
    }

    public static class HasTag extends TypeSafeMatcher<Creature> {
        private Creature trader;
        private String tag;
        private String answer = "?";

        HasTag(String tag) {
            this.tag = tag;
        }

        protected boolean matchesSafely(Creature trader) {
            this.trader = trader;

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT tag FROM traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                answer = rs.getString(1);
            });

            return answer.equals(tag);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" " + trader.getName() + " with tag \"" + tag + "\"");
        }

        @Override
        public void describeMismatchSafely(Creature trader, Description description) {
            description.appendText(" had tag \"" + answer + "\"");
        }
    }

    public static Matcher<Creature> hasTag(String tag) {
        return new HasTag(tag);
    }

    public static class DoesNotHaveTag extends TypeSafeMatcher<Creature> {
        private Creature trader;
        private String tag;
        private String answer = "?";

        DoesNotHaveTag(String tag) {
            this.tag = tag;
        }

        protected boolean matchesSafely(Creature trader) {
            this.trader = trader;

            execute(db -> {
                PreparedStatement ps = db.prepareStatement("SELECT tag FROM traders WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                answer = rs.getString(1);
            });

            return !answer.equals(tag);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" " + trader.getName() + " with tag other than \"" + tag + "\"");
        }

        @Override
        public void describeMismatchSafely(Creature trader, Description description) {
            description.appendText(" had tag \"" + answer + "\"");
        }
    }

    public static Matcher<Creature> doesNotHaveTag(String tag) {
        return new DoesNotHaveTag(tag);
    }
}

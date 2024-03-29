package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.creatures.Creature;
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
        private final int required;
        private Creature trader;
        private int answer;

        IsInDb(int required) {
            this.required = required;
        }

        protected boolean matchesSafely(Creature trader) {
            this.trader = trader;

            String table;
            if (CustomTraderTemplate.isCustomTrader(trader)) {
                table = "traders";
            } else if (CurrencyTraderTemplate.isCurrencyTrader(trader)) {
                table = "currency_traders";
            } else if (StatTraderTemplate.is(trader)) {
                table = "stat_traders";
            } else {
                throw new RuntimeException("A non-trader was passed.");
            }

            execute(db -> {
                //noinspection SqlResolve
                PreparedStatement ps = db.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE id=?");
                ps.setLong(1, trader.getWurmId());
                ResultSet rs = ps.executeQuery();

                answer = rs.getInt(1);
            });

            return answer == required;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" " + (required == 0 ? "no" : "a") + " trader with id " + trader.getWurmId());
        }

        @Override
        public void describeMismatchSafely(Creature trader, Description description) {
            description.appendText(" received " + answer + " entries");
        }
    }

    public static Matcher<Creature> isInDb() {
        return new IsInDb(1);
    }

    public static Matcher<Creature> isNotInDb() {
        return new IsInDb(0);
    }

    public static class HasTag extends TypeSafeMatcher<Creature> {
        private Creature trader;
        private final String tag;
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
        private final String tag;
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

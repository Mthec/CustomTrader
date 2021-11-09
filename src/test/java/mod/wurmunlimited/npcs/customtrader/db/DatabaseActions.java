package mod.wurmunlimited.npcs.customtrader.db;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseActions {
    static Object[] select(String table, String column, String value) {
        try {
            AtomicReference<Object[]> o = new AtomicReference<>();

            CustomTraderDatabase.Execute execute = db -> {
                PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE " + column + "=" + value);
                ResultSet rs = ps.executeQuery();

                List<Object> objects = new ArrayList<>();

                while (rs.next()) {
                    objects.add(rs.getObject(column));
                }

                o.set(objects.toArray(new Object[0]));
            };

            ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("execute", CustomTraderDatabase.Execute.class), execute);

            return o.get();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static Object select(String table, String column, String value, String getColumn) {
        try {
            AtomicReference<Integer> o = new AtomicReference<>();

            CustomTraderDatabase.Execute execute = db -> {
                PreparedStatement ps = db.prepareStatement("SELECT " + getColumn + " FROM " + table + " WHERE " + column + "=" + value);
                ResultSet rs = ps.executeQuery();

                o.set(rs.getFetchSize());
            };

            ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("execute", CustomTraderDatabase.Execute.class), execute);

            return o.get();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static int count(String table, String column, String value) {
        try {
            AtomicReference<Integer> o = new AtomicReference<>();

            CustomTraderDatabase.Execute execute = db -> {
                PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE " + column + "=" + value);
                ResultSet rs = ps.executeQuery();

                o.set(rs.getFetchSize());
            };

            ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("execute", CustomTraderDatabase.Execute.class), execute);

            return o.get();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

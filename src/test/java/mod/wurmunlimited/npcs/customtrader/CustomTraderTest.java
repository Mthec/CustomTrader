package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Constants;
import com.wurmonline.server.behaviours.PlaceCurrencyTraderAction;
import com.wurmonline.server.behaviours.PlaceCustomTraderAction;
import com.wurmonline.server.behaviours.PlaceNpcMenu;
import com.wurmonline.server.behaviours.PlaceStatTraderAction;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public abstract class CustomTraderTest {
    protected CustomTraderObjectsFactory factory;
    private static boolean init = false;
    protected static PlaceNpcMenu menu;

    @BeforeEach
    protected void setUp() throws Throwable {
        factory = new CustomTraderObjectsFactory();

        if (!init) {
            new PlaceCustomTraderAction();
            new PlaceCurrencyTraderAction();
            new PlaceStatTraderAction();
            menu = PlaceNpcMenu.register();
            init = true;
        }
    }

    private static void cleanUp() {
        File file = new File("sqlite/customtrader.db");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        file = new File("sqlite/tags.db");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        try {
            ReflectionUtil.setPrivateField(null, CustomTraderDatabase.class.getDeclaredField("created"), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void reset() {
        cleanUp();
        Constants.dbHost = ".";
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    protected static void execute(CustomTraderDatabase.Execute execute) {
        try {
            ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("execute", CustomTraderDatabase.Execute.class), execute);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected Stat create(String name, float ratio) {
        return Objects.requireNonNull(Stat.getFactoryByName(name)).create(ratio);
    }
}

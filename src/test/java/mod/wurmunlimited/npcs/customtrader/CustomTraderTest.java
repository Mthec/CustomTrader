package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Constants;
import com.wurmonline.server.behaviours.PlaceCurrencyTraderAction;
import com.wurmonline.server.behaviours.PlaceCustomTraderAction;
import com.wurmonline.server.behaviours.PlaceNpcMenu;
import com.wurmonline.server.behaviours.PlaceStatTraderAction;
import com.wurmonline.server.creatures.CustomTraderTradeHandler;
import com.wurmonline.server.questions.ModelOption;
import mod.wurmunlimited.npcs.FaceSetter;
import mod.wurmunlimited.npcs.ModelSetter;
import mod.wurmunlimited.npcs.TradeSetup;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import mod.wurmunlimited.npcs.customtrader.stats.Stat;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public abstract class CustomTraderTest {
    protected static final ModelOption[] modelOptions = new ModelOption[] { ModelOption.TRADER, ModelOption.HUMAN, ModelOption.CUSTOM };
    protected CustomTraderObjectsFactory factory;
    private static boolean init = false;
    protected static PlaceNpcMenu menu;
    private static final String dbString = "jdbc:sqlite:file::memory:?cache=shared";
    private static Connection db;

    @BeforeEach
    protected void setUp() throws Exception {
        factory = new CustomTraderObjectsFactory();

        if (!init) {
            new PlaceCustomTraderAction();
            new PlaceCurrencyTraderAction();
            new PlaceStatTraderAction();
            menu = PlaceNpcMenu.register();
            TradeSetup.addTrader(new CustomTraderMod()::isSpecialTrader, CustomTraderTradeHandler::create);
            init = true;
        }

        ReflectionUtil.setPrivateField(null, CustomTraderDatabase.class.getDeclaredField("dbString"), dbString);
        db = DriverManager.getConnection(dbString);
        ReflectionUtil.callPrivateMethod(null, CustomTraderDatabase.class.getDeclaredMethod("init"));

        ReflectionUtil.<List<FaceSetter>>getPrivateField(null, FaceSetter.class.getDeclaredField("faceSetters")).clear();
        ReflectionUtil.<List<ModelSetter>>getPrivateField(null, ModelSetter.class.getDeclaredField("modelSetters")).clear();

        CustomTraderMod mod = new CustomTraderMod();

        mod.faceSetter = new FaceSetter(mod::isSpecialTrader, "customtrader.db");
        mod.modelSetter = new ModelSetter(mod::isSpecialTrader, "customtrader.db");
    }

    private static void cleanUp() {
        try {
            if (db != null) {
                db.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

            //noinspection ResultOfMethodCallIgnored
            Files.walk(Paths.get(".")).filter(it -> it.getFileName().toString().startsWith("othertrader") && it.getFileName().toString().endsWith("log"))
                    .forEach(it -> it.toFile().delete());
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

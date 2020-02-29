package mod.wurmunlimited.npcs.customtrader;

import com.wurmonline.server.Constants;
import mod.wurmunlimited.npcs.customtrader.db.CustomTraderDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

public abstract class CustomTraderTest {
    protected CustomTraderObjectsFactory factory;

    @BeforeEach
    protected void setUp() throws Throwable {
        factory = new CustomTraderObjectsFactory();
    }

    private static void cleanUp() {
        File file = new File("sqlite/customtrader.db");
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
}

package mapgen;

import java.io.File;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class Main {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static String getParam(String varName) throws ParamException {
        String var = System.getenv(varName);
        if (var == null) {
            throw new ParamException("Missing env var: " + varName);
        }
        return var;
    }

    private static int getIntParam(String varName) throws ParamException, NumberFormatException {
        String var = Main.getParam(varName);
        return Integer.parseInt(var);
    }

    public static void main(String[] args) {
        try {
            String chunkDir = Main.getParam("CHUNK_DIR");
            int maxN = Main.getIntParam("MAX_N");

            var server = ServerMain.startServer(args);

            if (server == null) {
                System.err.println("Failed to start server!");
                return;
            }

            // Give enough time for server to init
            Thread.sleep(1000);
            var app = new App(server, new File(chunkDir), 10);
            app.generateSuperchunks(maxN);
            app.shutdown();
        } catch (Exception e) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", e);
        }
    }
}

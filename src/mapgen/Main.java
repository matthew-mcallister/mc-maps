package mapgen;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public class Main {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void main(String[] args) {
        try {
            var server = ServerMain.startServer(args);

            if (server == null) {
                System.err.println("Failed to start server!");
                return;
            }

            // Give enough time for server to init
            Thread.sleep(1000);
            var app = new App(server);
            app.generateSuperchunk(0, 0);
        } catch (Exception e) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", e);
        }
    }
}
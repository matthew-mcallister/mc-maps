package mapgen;

import java.io.File;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public class Main {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void main(String[] args) {
        try {
            String chunkDir = System.getenv("CHUNK_DIR");
            if (chunkDir == null) {
                System.err.println("Missing environment variable: CHUNK_DIR");
                System.exit(-1);
            }

            var server = ServerMain.startServer(args);

            if (server == null) {
                System.err.println("Failed to start server!");
                return;
            }

            // Give enough time for server to init
            Thread.sleep(1000);
            var app = new App(server, new File(chunkDir));
            app.generateSuperchunks();
        } catch (Exception e) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", e);
        }
    }
}

package mapgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.server.dedicated.DedicatedServer;

public class App extends Object {
    private DedicatedServer server;
    private File chunkDir;
    private ExecutorService executor;

    public App(DedicatedServer server, File chunkDir, int numThreads) {
        this.server = server;
        this.chunkDir = chunkDir;
        this.chunkDir.mkdirs();
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    public void generateSuperchunks(int maxN) throws IOException {
        for (int n = 0; n <= maxN; n++) {
            for (int k = 0; k < n; k++) {
                this.generateSuperchunk(n, k);
                this.generateSuperchunk(k, n);
            }
            this.generateSuperchunk(n, n);
        }
    }

    private void generateSuperchunk(int x, int z) throws IOException {
        String path = String.format("%s/%d,%d.dat", this.chunkDir.getPath(), x, z);
        File file = new File(path);
        if (file.exists()) {
            return;
        }

        this.executor.execute(() -> {
            var schunk = new Superchunk();
            schunk.load(this.server.overworld(), x, z);
            try (var stream = new FileOutputStream(file)) {
                schunk.serialize(stream);
            } catch (Exception e) {
                System.err.println(
                    String.format("Failed to generate %d,%d: %s", x, z, e.toString())
                );
            }
        });
    }

    public void shutdown() {
        this.executor.shutdown();
    }
}

package mapgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.minecraft.server.dedicated.DedicatedServer;

public class App extends Object {
    private DedicatedServer server;
    private File chunkDir;

    public App(DedicatedServer server, File chunkDir) {
        this.server = server;
        this.chunkDir = chunkDir;
        this.chunkDir.mkdirs();
    }

    public void generateSuperchunks() throws IOException {
        for (int n = 0;; n++) {
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

        var schunk = new Superchunk();
        schunk.load(this.server.overworld(), x, z);
        try (var stream = new FileOutputStream(file)) {
            schunk.serialize(stream);
        }
    }
}

package mapgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;

public class App {
    private static final int SUPERCHUNK_SIZE = 384;
    private static final int CHUNK_MIN_Y = -64;
    private static final int CHUNK_MAX_Y = 320;

    private DedicatedServer server;

    public App(DedicatedServer server) {
        this.server = server;
    }

    public void generateSuperchunk(int superChunkX, int superChunkZ) {
        var level = server.overworld();
        this.generateChunks(
                level,
                SUPERCHUNK_SIZE * superChunkX,
                CHUNK_MIN_Y,
                SUPERCHUNK_SIZE * superChunkZ,
                SUPERCHUNK_SIZE * (superChunkX + 1),
                CHUNK_MAX_Y,
                SUPERCHUNK_SIZE * (superChunkZ + 1));
    }

    private void generateChunks(ServerLevel level, int minx, int miny, int minz, int maxx, int maxy, int maxz) {
        assert level != null;
        for (int y = miny; y < maxy; y++) {
            for (int x = minx; x < maxx; x++) {
                for (int z = minz; z < maxz; z++) {
                    // TODO: Switch to getBlockStates?
                    var blockState = level.getBlockState(new BlockPos(x, y, z));
                    var block = blockState.getBlock();
                    var key = BuiltInRegistries.BLOCK.getKey(block);
                }
            }
        }
    }
}

package mapgen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

public class Superchunk {
    public static final int LENGTH = 224;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 160;

    short blockIds[][][];
    short blockParams[][][];

    public Superchunk() {
        this.blockIds = new short[LENGTH][MAX_Y - MIN_Y][LENGTH];
        this.blockParams = new short[LENGTH][MAX_Y - MIN_Y][LENGTH];
    }

    public void load(ServerLevel level, int cx, int cz) {
        System.out.printf("Loading superchunk %d, %d%n", cx, cz);
        var map = new BlockIdMap();
        int minx = Superchunk.LENGTH * cx, maxx = Superchunk.LENGTH * (cx + 1);
        int miny = Superchunk.MIN_Y, maxy = Superchunk.MAX_Y;
        int minz = Superchunk.LENGTH * cz, maxz = Superchunk.LENGTH * (cz + 1);
        for (int x = minx; x < maxx; x++) {
            for (int z = minz; z < maxz; z++) {
                for (int y = miny; y < maxy; y++) {
                    // TODO: Switch to getBlockStates?
                    var blockState = level.getBlockState(new BlockPos(x, y, z));
                    var block = blockState.getBlock();
                    var key = BuiltInRegistries.BLOCK.getKey(block);
                    this.blockIds[x - minx][y - Superchunk.MIN_Y][z - minz] = map.getId(key);
                }
            }
            System.out.printf("%f%%%n", 100.0 * (float)(x - minx) / Superchunk.LENGTH);
        }
    }

    public void serialize(OutputStream output) throws IOException {
        for (int i = 0; i < this.blockIds.length; i++) {
            for (int j = 0; j < this.blockIds[i].length; j++) {
                var buf = ByteBuffer.allocate(2 * this.blockIds[i][j].length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                for (int k = 0; k < this.blockIds[i][j].length; k++) {
                    buf.putShort(this.blockIds[i][j][k]);
                }
                output.write(buf.array());
            }
        }
    }
}

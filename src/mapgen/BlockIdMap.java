package mapgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Maps block names (as strings) to stable int IDs.
 */
public class BlockIdMap {
    private HashMap<ResourceLocation, Short> names;

    public BlockIdMap() {
        this.names = new HashMap<>();
        this.generate();
    }

    private void generate() {
        var keys = new ArrayList<>(BuiltInRegistries.BLOCK.keySet());
        Collections.sort(keys);
        for (short i = 0; i < keys.size(); i++) {
            this.names.put(keys.get(i), i);
        }
    }

    public short getId(ResourceLocation name) {
        return this.names.get(name).shortValue();
    }
}

package com.dfsek.terra.minestom.biome;

import java.util.HashMap;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.minestom.api.BiomeFactory;


public class MinestomUserDefinedBiomePool {
    private final HashMap<String, UserDefinedBiome> biomes = new HashMap<>();
    private final BiomeFactory factory;
    private final ConfigPack configPack;

    public MinestomUserDefinedBiomePool(ConfigPack configPack, BiomeFactory factory) {
        this.configPack = configPack;
        this.factory = factory;
    }

    public UserDefinedBiome getBiome(Biome source) {
        UserDefinedBiome userDefinedBiome = biomes.get(source.getID());
        if(userDefinedBiome != null) return userDefinedBiome;
        userDefinedBiome = factory.create(configPack, source);
        biomes.put(source.getID(), userDefinedBiome);
        return userDefinedBiome;
    }

    public void preloadBiomes(Iterable<Biome> biomesToLoad) {
        biomesToLoad
            .forEach(biome -> {
                if(!this.biomes.containsKey(biome.getID())) {
                    this.biomes.put(biome.getID(), factory.create(configPack, biome));
                }
            });
    }

    public void invalidate() {
        biomes.clear();
    }
}

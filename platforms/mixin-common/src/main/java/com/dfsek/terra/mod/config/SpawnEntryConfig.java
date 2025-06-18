package com.dfsek.terra.mod.config;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;

import com.dfsek.terra.api.util.range.Range;


public class SpawnEntryConfig implements ObjectTemplate<SpawnEntry> {
    @Value("type")
    @Default
    private EntityType<?> type = null;

    @Value("weight")
    @Default
    private Integer weight = null;

    @Value("group-size")
    @Default
    private Range groupSize = null;

    public Integer getWeight() {
        return weight;
    }

    @Override
    public SpawnEntry get() {
        return new SpawnEntry(type, groupSize.getMin(), groupSize.getMax());
    }
}

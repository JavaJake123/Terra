/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.biome.pipeline.config.source;

import com.dfsek.seismic.type.sampler.Sampler;
import com.dfsek.tectonic.api.config.template.annotations.Description;
import com.dfsek.tectonic.api.config.template.annotations.Value;

import com.dfsek.terra.addons.biome.pipeline.api.Source;
import com.dfsek.terra.addons.biome.pipeline.api.biome.PipelineBiome;
import com.dfsek.terra.addons.biome.pipeline.source.SamplerSource;
import com.dfsek.terra.api.config.meta.Meta;
import com.dfsek.terra.api.util.collection.ProbabilityCollection;


public class SamplerSourceTemplate extends SourceTemplate {
    @Value("sampler")
    @Description("The sampler used to distribute biomes.")
    private @Meta Sampler noise;

    @Value("biomes")
    @Description("The biomes to be distributed.")
    private @Meta ProbabilityCollection<@Meta PipelineBiome> biomes;

    @Override
    public Source get() {
        return new SamplerSource(biomes, noise);
    }
}

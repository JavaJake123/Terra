/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.biome.pipeline.config.stage;

import com.dfsek.seismic.type.sampler.Sampler;
import com.dfsek.tectonic.api.config.template.annotations.Description;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;

import com.dfsek.terra.addons.biome.pipeline.api.Stage;
import com.dfsek.terra.api.config.meta.Meta;


public abstract class StageTemplate implements ObjectTemplate<Stage> {
    @Value("sampler")
    @Description("Sampler to use for stage distribution.")
    protected @Meta Sampler noise;
}

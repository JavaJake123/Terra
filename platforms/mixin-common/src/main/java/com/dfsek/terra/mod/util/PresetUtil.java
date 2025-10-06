package com.dfsek.terra.mod.util;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.MetaPack;
import com.dfsek.terra.api.util.generic.pair.Pair;
import com.dfsek.terra.api.util.range.ConstantRange;
import com.dfsek.terra.mod.ModPlatform;
import com.dfsek.terra.mod.config.VanillaWorldProperties;
import com.dfsek.terra.mod.generation.GenerationSettings;
import com.dfsek.terra.mod.generation.MinecraftChunkGeneratorWrapper;
import com.dfsek.terra.mod.generation.TerraBiomeSource;


public class PresetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PresetUtil.class);
    private static final List<Pair<Identifier, Boolean>> PRESETS = new ArrayList<>();

    public static Pair<Identifier, WorldPreset> createDefault(ConfigPack pack, ModPlatform platform, boolean extended,
                                                              boolean packInMetapack) {
        Registry<DimensionType> dimensionTypeRegistry = platform.dimensionTypeRegistry();
        Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry = platform.chunkGeneratorSettingsRegistry();
        Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists =
            platform.multiNoiseBiomeSourceParameterListRegistry();


        Identifier generatorID = Identifier.tryParse(
            "terra:" + pack.getID().toLowerCase(Locale.ROOT) + "/" + pack.getNamespace().toLowerCase(
                Locale.ROOT));

        PRESETS.add(Pair.of(generatorID, extended));

        HashMap<RegistryKey<DimensionOptions>, DimensionOptions> dimensionMap = new HashMap<>();

        insertCustom(platform, "minecraft:overworld", pack, dimensionTypeRegistry, chunkGeneratorSettingsRegistry, dimensionMap,
            packInMetapack);

        insertDefaults(dimensionTypeRegistry, chunkGeneratorSettingsRegistry, multiNoiseBiomeSourceParameterLists, platform.biomeRegistry(),
            dimensionMap);

        WorldPreset preset = new WorldPreset(dimensionMap);
        LOGGER.info("Created world type \"{}\"", generatorID);
        return Pair.of(generatorID, preset);
    }

    public static Pair<Identifier, WorldPreset> createMetaPackPreset(MetaPack metaPack, ModPlatform platform, boolean extended) {
        Registry<DimensionType> dimensionTypeRegistry = platform.dimensionTypeRegistry();
        Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry = platform.chunkGeneratorSettingsRegistry();
        Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists =
            platform.multiNoiseBiomeSourceParameterListRegistry();

        Identifier generatorID = Identifier.of("terra",
            metaPack.getID().toLowerCase(Locale.ROOT) + "/" + metaPack.getNamespace().toLowerCase(
                Locale.ROOT));

        PRESETS.add(Pair.of(generatorID, extended));

        HashMap<RegistryKey<DimensionOptions>, DimensionOptions> dimensionMap = new HashMap<>();

        metaPack.packs().forEach((key, pack) -> {
            insertCustom(platform, key, pack, dimensionTypeRegistry, chunkGeneratorSettingsRegistry, dimensionMap, false);
        });

        insertDefaults(dimensionTypeRegistry, chunkGeneratorSettingsRegistry, multiNoiseBiomeSourceParameterLists, platform.biomeRegistry(),
            dimensionMap);

        WorldPreset preset = new WorldPreset(dimensionMap);
        LOGGER.info("Created world type \"{}\"", generatorID);
        return Pair.of(generatorID, preset);
    }

    private static void insertCustom(ModPlatform platform, String key, ConfigPack pack, Registry<DimensionType> dimensionTypeRegistry,
                                     Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry,
                                     HashMap<RegistryKey<DimensionOptions>, DimensionOptions> dimensionMap, boolean packInMetapack) {
        Identifier demensionIdentifier = Identifier.of(key);

        VanillaWorldProperties vanillaWorldProperties;

        if(pack.getContext().has(VanillaWorldProperties.class)) {
            vanillaWorldProperties = pack.getContext().get(VanillaWorldProperties.class);
        } else {
            vanillaWorldProperties = new VanillaWorldProperties();
        }

        DimensionType defaultDimension = dimensionTypeRegistry.get(Identifier.of(vanillaWorldProperties.getVanillaDimension()));

        assert defaultDimension != null;

        Identifier dimensionTypeID = Identifier.of("terra", pack.getID().toLowerCase(Locale.ROOT));

        DimensionType dimensionType;
        if(!packInMetapack) {
            dimensionType = DimensionUtil.createDimension(vanillaWorldProperties, defaultDimension, platform);
            RegistryKey<DimensionType> dimensionTypeRegistryKey = MinecraftUtil.registerDimensionTypeKey(
                dimensionTypeID);

            Registry.registerReference(dimensionTypeRegistry, dimensionTypeRegistryKey, dimensionType);
        } else {
            dimensionType = dimensionTypeRegistry.get(dimensionTypeID);
        }

        RegistryEntry<DimensionType> dimensionTypeRegistryEntry = dimensionTypeRegistry.getEntry(dimensionType);

        TerraBiomeSource biomeSource = new TerraBiomeSource(pack);

        RegistryEntry<ChunkGeneratorSettings> defaultGeneratorSettings = chunkGeneratorSettingsRegistry.getEntry(
            chunkGeneratorSettingsRegistry.get(Identifier.of(vanillaWorldProperties.getVanillaGeneration())));

        GenerationSettings generatorSettings = new GenerationSettings(
            vanillaWorldProperties.getHeight() == null ? new ConstantRange(
                defaultGeneratorSettings.value().generationShapeConfig().minimumY(),
                defaultGeneratorSettings.value().generationShapeConfig().height()) : vanillaWorldProperties.getHeight(),
            vanillaWorldProperties.getSealevel() == null
            ? defaultGeneratorSettings.value().seaLevel()
            : vanillaWorldProperties.getSealevel(),
            vanillaWorldProperties.getMobGeneration() == null
            ? !defaultGeneratorSettings.value().mobGenerationDisabled()
            : vanillaWorldProperties.getMobGeneration(),
            vanillaWorldProperties.getSpawnHeight());

        ChunkGenerator generator = new MinecraftChunkGeneratorWrapper(biomeSource, pack, generatorSettings);

        DimensionOptions dimensionOptions = new DimensionOptions(dimensionTypeRegistryEntry, generator);
        RegistryKey<DimensionOptions> dimensionOptionsRegistryKey = RegistryKey.of(RegistryKeys.DIMENSION, demensionIdentifier);
        dimensionMap.put(dimensionOptionsRegistryKey, dimensionOptions);
    }

    private static void insertDefaults(Registry<DimensionType> dimensionTypeRegistry,
                                       Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry,
                                       Registry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists,
                                       Registry<Biome> biomeRegistry, HashMap<RegistryKey<DimensionOptions>, DimensionOptions> map) {
        if(!map.containsKey(DimensionOptions.OVERWORLD)) {
            RegistryEntry<DimensionType> overworldDimensionType = dimensionTypeRegistry.getEntry(
                dimensionTypeRegistry.get(DimensionTypes.OVERWORLD));

            RegistryEntry<MultiNoiseBiomeSourceParameterList> overworldChunkBiomeReference =
                multiNoiseBiomeSourceParameterLists.getEntry(multiNoiseBiomeSourceParameterLists.get(
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD));

            RegistryEntry<ChunkGeneratorSettings> overworldChunkGeneratorSettings = chunkGeneratorSettingsRegistry.getEntry(
                chunkGeneratorSettingsRegistry.get(
                    ChunkGeneratorSettings.OVERWORLD));


            DimensionOptions overworldDimensionOptions = new DimensionOptions(overworldDimensionType,
                (new NoiseChunkGenerator(MultiNoiseBiomeSource.create(overworldChunkBiomeReference), overworldChunkGeneratorSettings)));
            map.put(DimensionOptions.OVERWORLD, overworldDimensionOptions);
        }
        if(!map.containsKey(DimensionOptions.NETHER)) {
            RegistryEntry<DimensionType> netherDimensionType = dimensionTypeRegistry.getEntry(
                dimensionTypeRegistry.get(DimensionTypes.THE_NETHER));

            RegistryEntry<MultiNoiseBiomeSourceParameterList> netherChunkBiomeReference =
                multiNoiseBiomeSourceParameterLists.getEntry(multiNoiseBiomeSourceParameterLists.get(
                    MultiNoiseBiomeSourceParameterLists.NETHER));

            RegistryEntry<ChunkGeneratorSettings> netherChunkGeneratorSettings = chunkGeneratorSettingsRegistry.getEntry(
                chunkGeneratorSettingsRegistry.get(
                    ChunkGeneratorSettings.NETHER));

            DimensionOptions overworldDimensionOptions = new DimensionOptions(netherDimensionType,
                (new NoiseChunkGenerator(MultiNoiseBiomeSource.create(netherChunkBiomeReference), netherChunkGeneratorSettings)));
            map.put(DimensionOptions.NETHER, overworldDimensionOptions);
        }
        if(!map.containsKey(DimensionOptions.END)) {
            RegistryEntry<DimensionType> endDimensionType = dimensionTypeRegistry.getEntry(
                dimensionTypeRegistry.get(DimensionTypes.THE_END));

            RegistryEntry<ChunkGeneratorSettings> endChunkGeneratorSettings = chunkGeneratorSettingsRegistry.getEntry(
                chunkGeneratorSettingsRegistry.get(
                    ChunkGeneratorSettings.END));


            DimensionOptions overworldDimensionOptions = new DimensionOptions(endDimensionType,
                (new NoiseChunkGenerator(TheEndBiomeSource.createVanilla(biomeRegistry), endChunkGeneratorSettings)));
            map.put(DimensionOptions.END, overworldDimensionOptions);
        }
    }

    public static List<Pair<Identifier, Boolean>> getPresets() {
        return PRESETS;
    }
}

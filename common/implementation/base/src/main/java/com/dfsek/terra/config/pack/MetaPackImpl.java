package com.dfsek.terra.config.pack;

import ca.solostudios.strata.version.Version;
import com.dfsek.tectonic.api.TypeRegistry;
import com.dfsek.tectonic.api.config.Configuration;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;
import com.dfsek.tectonic.api.loader.AbstractConfigLoader;
import com.dfsek.tectonic.api.loader.ConfigLoader;
import com.dfsek.tectonic.api.loader.type.TypeLoader;
import com.dfsek.tectonic.yaml.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.MetaPack;
import com.dfsek.terra.api.properties.Context;
import com.dfsek.terra.api.registry.CheckedRegistry;
import com.dfsek.terra.api.registry.OpenRegistry;
import com.dfsek.terra.api.registry.Registry;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.util.reflection.ReflectionUtil;
import com.dfsek.terra.api.util.reflection.TypeKey;
import com.dfsek.terra.config.loaders.GenericTemplateSupplierLoader;
import com.dfsek.terra.registry.CheckedRegistryImpl;
import com.dfsek.terra.registry.OpenRegistryImpl;
import com.dfsek.terra.registry.master.ConfigRegistry;


public class MetaPackImpl implements MetaPack {

    private static final Pattern PATTERN = Pattern.compile(", ");
    private static final Logger logger = LoggerFactory.getLogger(MetaPackImpl.class);
    private final MetaPackTemplate template = new MetaPackTemplate();
    private final Platform platform;
    private final Path rootPath;
    private final Map<String, ConfigPack> packs = new HashMap<>();
    private final ConfigLoader selfLoader = new ConfigLoader();
    private final Context context = new Context();
    private final RegistryKey key;
    private final Map<Type, CheckedRegistryImpl<?>> registryMap = new HashMap<>();
    private final AbstractConfigLoader abstractConfigLoader = new AbstractConfigLoader();
    private final String author;

    public MetaPackImpl(Path path, Platform platform, ConfigRegistry configRegistry) throws IOException {
        long start = System.nanoTime();

        if(Files.notExists(path)) throw new FileNotFoundException("Could not load metapack, " + path + " does not exist");

        if(Files.isDirectory(path)) {
            this.rootPath = path;
        } else if(Files.isRegularFile(path)) {
            if(!path.getFileName().toString().endsWith(".zip")) {
                throw new IOException("Could not load metapack, file " + path + " is not a zip");
            }
            FileSystem zipfs = FileSystems.newFileSystem(path);
            this.rootPath = zipfs.getPath("/");
        } else {
            throw new IOException("Could not load metapack from " + path);
        }

        Path packManifestPath = rootPath.resolve("metapack.yml");
        if(Files.notExists(packManifestPath)) throw new IOException("No metapack.yml found in " + path);
        Configuration packManifest = new YamlConfiguration(Files.newInputStream(packManifestPath),
            packManifestPath.getFileName().toString());

        this.platform = platform;

        register(selfLoader);
        platform.register(selfLoader);

        register(abstractConfigLoader);
        platform.register(abstractConfigLoader);

        selfLoader.load(template, packManifest);

        String namespace;
        String id;
        if(template.getID().contains(":")) {
            namespace = template.getID().substring(0, template.getID().indexOf(":"));
            id = template.getID().substring(template.getID().indexOf(":") + 1);
        } else {
            id = template.getID();
            namespace = template.getID();
        }

        this.key = RegistryKey.of(namespace, id);

        logger.info("Loading metapack \"{}:{}\"", id, namespace);

        template.getPacks().forEach((k, v) -> {
            RegistryKey registryKey = RegistryKey.parse(v);
            if(configRegistry.contains(registryKey)) {
                packs.put(k, configRegistry.get(registryKey).get());
                logger.info("Linked config pack \"{}\" to metapack \"{}:{}\".", v, namespace, id);
            } else {
                logger.warn("Failed to link config pack \"{}\" to metapack \"{}:{}\".", v, namespace, id);
            }
        });

        HashSet<String> authors = new HashSet<>();
        packs.forEach((k, v) -> {
            authors.addAll(Arrays.asList(PATTERN.split(v.getAuthor())));
        });
        authors.addAll(Arrays.asList(PATTERN.split(template.getAuthor())));

        this.author = String.join(", ", authors);

        logger.info("Loaded metapack \"{}:{}\" v{} by {} in {}ms.",
            namespace, id, getVersion().getFormatted(), author, (System.nanoTime() - start) / 1000000.0D);
    }


    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public Version getVersion() {
        return template.getVersion();
    }

    @Override
    public Map<String, ConfigPack> packs() {
        return packs;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public RegistryKey getRegistryKey() {
        return key;
    }

    @Override
    public <T> CheckedRegistry<T> getRegistry(Type type) {
        return (CheckedRegistry<T>) registryMap.get(type);
    }

    @Override
    public <T> CheckedRegistry<T> getCheckedRegistry(Type type) throws IllegalStateException {
        return (CheckedRegistry<T>) registryMap.get(type);
    }

    @Override
    public <T> CheckedRegistry<T> getOrCreateRegistry(TypeKey<T> typeKey) {
        return (CheckedRegistry<T>) registryMap.computeIfAbsent(typeKey.getType(), c -> {
            OpenRegistry<T> registry = new OpenRegistryImpl<>(typeKey);
            selfLoader.registerLoader(c, registry);
            abstractConfigLoader.registerLoader(c, registry);
            logger.debug("Registered loader for registry of class {}", ReflectionUtil.typeToString(c));

            if(typeKey.getType() instanceof ParameterizedType param) {
                Type base = param.getRawType();
                if(base instanceof Class  // should always be true but we'll check anyways
                   && Supplier.class.isAssignableFrom((Class<?>) base)) { // If it's a supplier
                    Type supplied = param.getActualTypeArguments()[0]; // Grab the supplied type
                    if(supplied instanceof ParameterizedType suppliedParam) {
                        Type suppliedBase = suppliedParam.getRawType();
                        if(suppliedBase instanceof Class // should always be true but we'll check anyways
                           && ObjectTemplate.class.isAssignableFrom((Class<?>) suppliedBase)) {
                            Type templateType = suppliedParam.getActualTypeArguments()[0];
                            GenericTemplateSupplierLoader<?> loader = new GenericTemplateSupplierLoader<>(
                                (Registry<Supplier<ObjectTemplate<Supplier<ObjectTemplate<?>>>>>) registry);
                            selfLoader.registerLoader(templateType, loader);
                            abstractConfigLoader.registerLoader(templateType, loader);
                            logger.debug("Registered template loader for registry of class {}", ReflectionUtil.typeToString(templateType));
                        }
                    }
                }
            }

            return new CheckedRegistryImpl<>(registry);
        });
    }

    @Override
    public <T> MetaPackImpl applyLoader(Type type, TypeLoader<T> loader) {
        abstractConfigLoader.registerLoader(type, loader);
        selfLoader.registerLoader(type, loader);
        return this;
    }

    @Override
    public <T> MetaPackImpl applyLoader(Type type, Supplier<ObjectTemplate<T>> loader) {
        abstractConfigLoader.registerLoader(type, loader);
        selfLoader.registerLoader(type, loader);
        return this;
    }

    @Override
    public void register(TypeRegistry registry) {
        registryMap.forEach(registry::registerLoader);
    }

}

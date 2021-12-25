package com.dfsek.terra.api.registry.key;

public interface Namespaced {
    String getNamespace();
    
    default RegistryKey getKey(String id) {
        return RegistryKey.of(getNamespace(), id);
    }
}

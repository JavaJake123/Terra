version = version("1.0.0")

dependencies {
    compileOnlyApi(project(":common:addons:manifest-addon-loader"))
    compileOnlyApi(project(":common:addons:biome-provider-pipeline"))
    compileOnlyApi(project(":common:addons:library-image"))
}

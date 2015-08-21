package com.almuradev.almura.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.Map;

public class AlmuraCorePlugin implements IFMLLoadingPlugin {

    public AlmuraCorePlugin() {
        MixinEnvironment.getDefaultEnvironment().addConfiguration("mixins.almura.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

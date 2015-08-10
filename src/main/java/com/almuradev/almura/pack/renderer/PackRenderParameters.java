package com.almuradev.almura.pack.renderer;

import net.malisis.core.renderer.Parameter;
import net.malisis.core.renderer.RenderParameters;

public class PackRenderParameters extends RenderParameters {
    // face -> textureid
    public final Parameter<Integer> textureId = new Parameter<>(0);
    public final Parameter<Boolean> mirrorFace = new Parameter<>(false);

    public PackRenderParameters() {
        this.listParams.add(textureId);
    }

    public PackRenderParameters(RenderParameters parameters) {
        super(parameters);
    }
}

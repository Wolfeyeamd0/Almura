package com.almuradev.almura.pack.renderer;

import net.malisis.core.renderer.Parameter;
import net.malisis.core.renderer.RenderParameters;

public class PackRenderParameters extends RenderParameters {
    // face -> textureid
    public final Parameter<Integer> textureId = new Parameter<>(0);

    public PackRenderParameters() {
        this.listParams.add(textureId);
    }
}

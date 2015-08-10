/**
 * This file is part of Almura, All Rights Reserved.
 *
 * Copyright (c) 2014 AlmuraDev <http://github.com/AlmuraDev/>
 */
package com.almuradev.almura.pack.model;

import com.google.common.base.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.malisis.core.renderer.element.Face;
import net.malisis.core.renderer.element.Shape;

import java.util.List;

public class PackModelContainer {

    private final String identifier;
    private final PackPhysics physics;
    private Optional<IModel> model;

    @SuppressWarnings("unchecked")
    public PackModelContainer(String identifier, PackPhysics physics) {
        this.identifier = identifier;
        this.physics = physics;
    }

    public String getIdentifier() {
        return identifier;
    }

    public PackPhysics getPhysics() {
        return physics;
    }

    @SideOnly(Side.CLIENT)
    public Optional<IModel> getModel() {
        return model;
    }

    @SideOnly(Side.CLIENT)
    public void setModel(IModel model) {
        this.model = Optional.fromNullable(model);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && identifier.equals(((PackModelContainer) o).identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "PackModel {identifier= " + identifier + ", model= " + model + "}";
    }

    @SideOnly(Side.CLIENT)
    public static final class PackShape extends Shape implements IModel {
        public PackShape(Face[] faces) {
            super(faces);
        }

        public PackShape(List<Face> faces) {
            super(faces);
        }

        public PackShape(Shape shape) {
            super(shape);
        }

        public PackShape addFace(Face face) {
            addFaces(new Face[]{face});
            return this;
        }
    }

}

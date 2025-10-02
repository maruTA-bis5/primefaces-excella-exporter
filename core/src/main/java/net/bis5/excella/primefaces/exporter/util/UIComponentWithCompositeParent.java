package net.bis5.excella.primefaces.exporter.util;

import javax.faces.component.UIComponent;

/**
 * A wrapper of UIComponent to keep its composite component parent if exists.
 */
public class UIComponentWithCompositeParent {
    private final UIComponent component;
    private final UIComponent compositeParent;

    public UIComponent getComponent() {
        return component;
    }

    public UIComponent getCompositeParent() {
        return compositeParent;
    }

    public UIComponentWithCompositeParent(UIComponent component, UIComponent compositeParent) {
        this.component = component;
        this.compositeParent = compositeParent;
    }
    public UIComponentWithCompositeParent(UIComponent component) {
        this(component, null);
    }

    public boolean isInCompositeComponent() {
        return compositeParent != null;
    }

    public boolean isRendered() {
        return (compositeParent == null || compositeParent.isRendered()) && component.isRendered();
    }
}

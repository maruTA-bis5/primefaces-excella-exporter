package org.primefaces.showcase.components;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIOutput;

import net.bis5.excella.primefaces.exporter.component.ExportableComponent;

@FacesComponent(createTag = true, tagName = "exportableValueHolder", namespace = "primefaces-excella-exporter", value = "primefaces-excella-exporter.EditableValueHolder")
public class ExportableValueHolder extends UIOutput implements ExportableComponent {

    public void setExportValue(Object value) {
        getStateHelper().put("exportValue", value);
    }

    @Override
    public Object getExportValue() {
        return getStateHelper().eval("exportValue");
    }

}

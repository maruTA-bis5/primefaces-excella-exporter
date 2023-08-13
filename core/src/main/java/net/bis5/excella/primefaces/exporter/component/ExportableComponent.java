package net.bis5.excella.primefaces.exporter.component;

/**
 * The interface indicates the component has an exportable value.
 * @since 3.2.0
 */
public interface ExportableComponent {

    /**
     * @return An exportable value. It may not be the same as {@link javax.faces.component.ValueHolder#getValue()}.
     */
    Object getExportValue();

}

package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;

import org.junit.jupiter.api.Test;
import org.primefaces.component.api.UIColumn;

import net.bis5.excella.primefaces.exporter.util.UIComponentWithCompositeParent;

class ColumnValueSeparatorTest {

    @Test
    void joinsDataTableComponentValuesWithConfiguredSeparator() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();
        assertColumnValueSeparators(new TestDataTableExcellaExporter(options), options);
    }

    @Test
    void joinsTreeTableComponentValuesWithConfiguredSeparator() {
        ExCellaExporterOptions options = new ExCellaExporterOptions();
        assertColumnValueSeparators(new TestTreeTableExcellaExporter(options), options);
    }

    private void assertColumnValueSeparators(ExCellaExporter<?> exporter, ExCellaExporterOptions options) {
        UIColumn column = createColumn();
        List<UIComponentWithCompositeParent> components = Arrays.asList(new TestComponent("first"),
                new TestComponent("second"));

        options.setColumnValueSeparator(", ");
        assertEquals("first, second",
                exporter.getExCellaColumnValue(null, null, column, components, true).getValue());

        options.setColumnValueSeparator("");
        assertEquals("firstsecond",
                exporter.getExCellaColumnValue(null, null, column, components, true).getValue());
    }

    private UIColumn createColumn() {
        return (UIColumn) Proxy.newProxyInstance(UIColumn.class.getClassLoader(), new Class<?>[] {UIColumn.class},
                (proxy, method, arguments) -> null);
    }

    private static class TestDataTableExcellaExporter extends DataTableExcellaExporter {
        private final ExCellaExporterOptions options;

        private TestDataTableExcellaExporter(ExCellaExporterOptions options) {
            this.options = options;
        }

        @Override
        public ExCellaExporterOptions getExporterOptions() {
            return options;
        }

        @Override
        public Object exportObjectValue(FacesContext context, UIComponentWithCompositeParent component) {
            return ((TestComponent) component).value;
        }
    }

    private static class TestTreeTableExcellaExporter extends TreeTableExcellaExporter {
        private final ExCellaExporterOptions options;

        private TestTreeTableExcellaExporter(ExCellaExporterOptions options) {
            this.options = options;
        }

        @Override
        public ExCellaExporterOptions getExporterOptions() {
            return options;
        }

        @Override
        public Object exportObjectValue(FacesContext context, UIComponentWithCompositeParent component) {
            return ((TestComponent) component).value;
        }
    }

    private static class TestComponent extends UIComponentWithCompositeParent {
        private final String value;

        private TestComponent(String value) {
            super(null);
            this.value = value;
        }

        @Override
        public boolean isRendered() {
            return true;
        }
    }
}

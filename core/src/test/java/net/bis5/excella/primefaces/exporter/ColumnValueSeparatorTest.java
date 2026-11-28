package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.junit.jupiter.api.Test;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.util.EditableValueHolderState;

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

    // NullAway: null context/table are never dereferenced on the stub column's export path.
    @SuppressWarnings("NullAway")
    private void assertColumnValueSeparators(ExCellaExporter<?> exporter, ExCellaExporterOptions options) {
        UIColumn column = createColumn();
        List<UIComponentWithCompositeParent> components = Arrays.asList(new TestComponent("first"),
                new TestComponent("second"));

        assertEquals("first second",
                exporter.getExCellaColumnValue(null, null, column, components, true).getValue());
        assertEquals("first",
                exporter.getExCellaColumnValue(null, null, column, components, false).getValue());

        options.setColumnValueSeparator(", ");
        assertEquals("first, second",
                exporter.getExCellaColumnValue(null, null, column, components, true).getValue());

        options.setColumnValueSeparator("");
        assertEquals("firstsecond",
                exporter.getExCellaColumnValue(null, null, column, components, true).getValue());
    }

    private UIColumn createColumn() {
        return new TestColumn();
    }

    // NullAway: TestColumnはUIColumnのテスト用スタブであり、各メソッドは未使用のダミー値(null/0/false)を返すのみ。
    @SuppressWarnings("NullAway")
    private static class TestColumn implements UIColumn {
        @Override
        public String resolveField(FacesContext context, ValueExpression valueExpression) {
            return null;
        }

        @Override
        public ValueExpression getValueExpression(String name) {
            return null;
        }

        @Override
        public String getContainerClientId(FacesContext context) {
            return null;
        }

        @Override
        public String getColumnKey() {
            return null;
        }

        @Override
        public String getColumnKey(UIComponent component, int index) {
            return null;
        }

        @Override
        public String getColumnKey(UIComponent component, String rowKey) {
            return null;
        }

        @Override
        public String getClientId() {
            return null;
        }

        @Override
        public String getClientId(FacesContext context) {
            return null;
        }

        @Override
        public boolean isSelectionBox() {
            return false;
        }

        @Override
        public boolean isResizable() {
            return false;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public String getStyleClass() {
            return null;
        }

        @Override
        public int getRowspan() {
            return 0;
        }

        @Override
        public int getColspan() {
            return 0;
        }

        @Override
        public String getFilterPosition() {
            return null;
        }

        @Override
        public String getFilterPlaceholder() {
            return null;
        }

        @Override
        public UIComponent getFacet(String name) {
            return null;
        }

        @Override
        public Object getFilterBy() {
            return null;
        }

        @Override
        public Object getFilterValue() {
            return null;
        }

        @Override
        public String getHeaderText() {
            return null;
        }

        @Override
        public String getFooterText() {
            return null;
        }

        @Override
        public String getFilterStyleClass() {
            return null;
        }

        @Override
        public String getFilterStyle() {
            return null;
        }

        @Override
        public String getFilterMatchMode() {
            return null;
        }

        @Override
        public int getFilterMaxLength() {
            return 0;
        }

        @Override
        public CellEditor getCellEditor() {
            return null;
        }

        @Override
        public boolean isDynamic() {
            return false;
        }

        @Override
        public MethodExpression getSortFunction() {
            return null;
        }

        @Override
        public Object getSortBy() {
            return null;
        }

        @Override
        public List<UIComponent> getChildren() {
            return null;
        }

        @Override
        public boolean isExportable() {
            return false;
        }

        @Override
        public boolean isRendered() {
            return false;
        }

        @Override
        public void encodeAll(FacesContext context) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renderChildren(FacesContext context) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWidth() {
            return null;
        }

        @Override
        public boolean isToggleable() {
            return false;
        }

        @Override
        public boolean isDraggable() {
            return false;
        }

        @Override
        public MethodExpression getFilterFunction() {
            return null;
        }

        @Override
        public String getField() {
            return null;
        }

        @Override
        public int getResponsivePriority() {
            return 0;
        }

        @Override
        public boolean isSortable() {
            return false;
        }

        @Override
        public boolean isFilterable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public boolean isSelectRow() {
            return false;
        }

        @Override
        public String getAriaHeaderText() {
            return null;
        }

        @Override
        public MethodExpression getExportFunction() {
            return null;
        }

        @Override
        public Object getExportValue() {
            return null;
        }

        @Override
        public int getExportRowspan() {
            return 0;
        }

        @Override
        public int getExportColspan() {
            return 0;
        }

        @Override
        public boolean isGroupRow() {
            return false;
        }

        @Override
        public Object getExportHeaderValue() {
            return null;
        }

        @Override
        public Object getExportFooterValue() {
            return null;
        }

        @Override
        public String getExportTag() {
            return null;
        }

        @Override
        public String getSortOrder() {
            return null;
        }

        @Override
        public int getSortPriority() {
            return 0;
        }

        @Override
        public int getNullSortOrder() {
            return 0;
        }

        @Override
        public boolean isCaseSensitiveSort() {
            return false;
        }

        @Override
        public int getDisplayPriority() {
            return 0;
        }

        @Override
        public Object getConverter() {
            return null;
        }

        @Override
        public EditableValueHolderState getFilterValueHolder(FacesContext context) {
            return null;
        }

        @Override
        public void setFilterValueToValueHolder(FacesContext context, Object filterValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UIComponent asUIComponent() {
            return null;
        }
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

    // NullAway: TestComponent is a stub holding the export value; the wrapped UIComponent is an unused dummy (null).
    @SuppressWarnings("NullAway")
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

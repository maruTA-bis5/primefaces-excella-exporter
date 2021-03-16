package net.bis5.excella.primefaces.exporter;

public enum TemplateType {
        XLS(".xls", "application/octet-stream"),
        XLSX(".xlsx", "application/octet-stream");
        TemplateType(String suffix, String contentType) {
            this.suffix = suffix;
            this.contentType = contentType;
        }

        private String suffix;

        private String contentType;

        public String getSuffix() {
            return suffix;
        }

        public String getContentType() {
            return contentType;
        }
}

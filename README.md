# PrimeFaces ExCella Exporter

ExCella Reports (https://github.com/excella-core/excella-reports) を利用してExcelファイルを出力する、PrimeFacesのExporter実装

[![build](https://github.com/maruTA-bis5/primefaces-excella-exporter/actions/workflows/build.yml/badge.svg)](https://github.com/maruTA-bis5/primefaces-excella-exporter/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.bis5.excella/primefaces-excella-exporter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.bis5.excella/primefaces-excella-exporter)
[![Javadocs](http://javadoc.io/badge/net.bis5.excella/primefaces-excella-exporter.svg)](http://javadoc.io/doc/net.bis5.excella/primefaces-excella-exporter)

## Usage
1. Add primefaces-excella-exporter to your project.
    ```xml
    <dependency>
      <groupId>net.bis5.excella</groupId>
      <artifactId>primefaces-excella-exporter</artifactId>
      <version>${VERSION}</version>
    </dependency>
    ```
1. Create backing bean provides exporter instance.
    ```java
    @Model
    public class Exporters {
        public Exporter<DataTable> getDataTableExporter() {
            return new DataTableExcellaExporter();
        }
    }
    ```
1. Set `exporter` attribute to `<p:dataExporter>`.
    ```xml
    <p:dataExporter
        type="excella"
        target="table"
        exporter="#{exporters.dataTableExporter}" />
    ```

### Version matrix

|primefaces-excella-exporter version|Compatible PrimeFaces version|
|---|---|
|1.x|8.x|
|2.x (Plan)|10.x|

## Development
- build
    ```shellscript
    mvn clean package
    ```
- test (WIP)
    - Clone https://github.com/maruTA-bis5/primefaces-excella-exporter-test.git
    - Run `mvn clean verify`

## Contribution
1. Fork it
1. Create your feature branch (git checkout -b my-new-feature)
1. Commit your changes (git commit -am 'Add some feature')
1. Push to the branch (git push origin my-new-feature)
1. Create new Pull Request

## License
MIT

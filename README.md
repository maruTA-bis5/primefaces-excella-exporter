# PrimeFaces ExCella Exporter

[ExCella Reports](https://github.com/excella-core/excella-reports) を利用してExcelファイルを出力する、PrimeFacesのExporter実装
This library provides the implementation of PrimeFaces' Exporter using [ExCella Reports](https://github.com/excella-core/excella-reports).


[![build](https://github.com/maruTA-bis5/primefaces-excella-exporter/actions/workflows/build.yml/badge.svg)](https://github.com/maruTA-bis5/primefaces-excella-exporter/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.bis5.excella/primefaces-excella-exporter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.bis5.excella/primefaces-excella-exporter)
[![Javadocs](http://javadoc.io/badge/net.bis5.excella/primefaces-excella-exporter.svg)](http://javadoc.io/doc/net.bis5.excella/primefaces-excella-exporter)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=maruTA-bis5_primefaces-excella-exporter&metric=alert_status)](https://sonarcloud.io/dashboard?id=maruTA-bis5_primefaces-excella-exporter)

## Usage
1. Add primefaces-excella-exporter and excella-reports to your project.
    ```xml
    <dependency>
      <groupId>net.bis5.excella</groupId>
      <artifactId>primefaces-excella-exporter</artifactId>
      <version>${primefaces-excella-exporter.version}</version>
    </dependency>
    <dependency>
      <groupId>org.bbreak.excella</groupId>
      <artifactId>excella-reports</artifactId>
      <version>${excella-reports.version}</version>
    </dependency>
    ```
1. Add excella's repository
    ```xml
    <repositories>
      <repository>
        <id>excella.bbreak.org</id>
        <name>bBreak Systems Excella</name>
        <url>https://excella-core.github.io/maven2/</url>    
      </repository>
    </repositories>
    ````
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
|2.x|11.x|

## Development
- build
    ```shellscript
    mvn clean package
    ```
- test
    ```shellscript
    cd integration-test
    chmod 777 docker-compose/downloads
    HOST_IP=<docker-host-ip-addr> docker-compose up -d
    mvn clean verify
    ```

## Contribution
1. Fork it
1. Create your feature branch (git checkout -b my-new-feature)
1. Commit your changes (git commit -am 'Add some feature')
1. Push to the branch (git push origin my-new-feature)
1. Create new Pull Request

## Contact
- Create GitHub Issue (https://github.com/maruTA-bis5/primefaces-excella-exporter/issues/new)
- or Start new Discussion (https://github.com/maruTA-bis5/primefaces-excella-exporter/discussions/new)

## License
MIT

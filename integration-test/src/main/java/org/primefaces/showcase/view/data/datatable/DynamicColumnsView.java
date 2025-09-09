package org.primefaces.showcase.view.data.datatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("dtDynamicColumnsView")
@ViewScoped
public class DynamicColumnsView implements Serializable {

    private final List<String> rows = new ArrayList<>();

    @PostConstruct
    public void init() {
        rows.add("data");
    }

    public List<String> getRows() {
        return rows;
    }

}

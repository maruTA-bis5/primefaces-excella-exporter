/*
 * Copyright 2009-2014 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.showcase.view.data.treetable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

@Named("ttBasicView")
@ViewScoped
public class BasicView implements Serializable {

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    private TreeNode root = new DefaultTreeNode();

    @PostConstruct
    public void initialize() {
        TreeNode parent = new DefaultTreeNode(new DataTypeCheck(), root);
        new DefaultTreeNode(new DataTypeCheck("Ch-"), parent);
        parent.setExpanded(true);
    }

    public static class DataTypeCheck implements Serializable {

        public DataTypeCheck() {
            this("");
        }
        public DataTypeCheck(String prefix) {
            stringProperty = prefix + "STRING value";
        }
        private final String stringProperty;
        private final YearMonth yearMonthProperty = YearMonth.of(2021, 4);
        private final Date dateProperty = newDate();
        private final Date dateTimeProperty = newDateTime();
        private final LocalDate localDateProperty = LocalDate.of(2021, 3 ,23);
        private final LocalDateTime localDateTimeProperty = LocalDateTime.of(2021, 3,23, 21, 49, 0);
        private final int intProperty = 123;
        private final BigDecimal bigDecimalIntProperty = new BigDecimal("321.00");
        private final double doubleProperty = 102.4;
        private final BigDecimal bigDecimalDecimalProperty = new BigDecimal("204.89");

        private static Date newDate() {
            return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        public String getStringProperty() {
            return stringProperty;
        }
        public YearMonth getYearMonthProperty() {
            return yearMonthProperty;
        }
        public Date getDateProperty() {
            return dateProperty;
        }
        public Date getDateTimeProperty() {
            return dateTimeProperty;
        }
        public LocalDate getLocalDateProperty() {
            return localDateProperty;
        }
        public LocalDateTime getLocalDateTimeProperty() {
            return localDateTimeProperty;
        }
        public int getIntProperty() {
            return intProperty;
        }
        public BigDecimal getBigDecimalIntProperty() {
            return bigDecimalIntProperty;
        }
        public double getDoubleProperty() {
            return doubleProperty;
        }
        public BigDecimal getBigDecimalDecimalProperty() {
            return bigDecimalDecimalProperty;
        }
        private static Date newDateTime() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2020);
            cal.set(Calendar.MONTH, Calendar.MARCH);
            cal.set(Calendar.DAY_OF_MONTH, 13);
            cal.set(Calendar.HOUR_OF_DAY, 22);
            cal.set(Calendar.MINUTE, 15);
            cal.set(Calendar.SECOND, 34);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }
    }

}

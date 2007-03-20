/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.errorhandling;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiType;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.BaseInspection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class BadExceptionThrownInspection extends BaseInspection {

    /**@noinspection PublicField*/
    public String exceptionCheckString =
      "java.lang.Throwable" + ',' +
      "java.lang.Exception" + ',' +
      "java.lang.Error" + ',' +
      "java.lang.RuntimeException" + ',' +
      "java.lang.NullPointerException" + ',' +
      "java.lang.ClassCastException" + ',' +
      "java.lang.ArrayIndexOutOfBoundsException";

    final List<String> exceptionsList = new ArrayList<String>(32);
    final Object lock = new Object();

    public BadExceptionThrownInspection(){
        parseCallCheckString();
    }

    public void readSettings(Element element) throws InvalidDataException{
        super.readSettings(element);
        parseCallCheckString();
    }

    private void parseCallCheckString(){
        final String[] strings = exceptionCheckString.split(",");
        synchronized(lock){
            exceptionsList.clear();
            for(String string : strings){
                exceptionsList.add(string);
            }
        }
    }

    public void writeSettings(Element element) throws WriteExternalException{
        formatCallCheckString();
        super.writeSettings(element);
    }

    private void formatCallCheckString(){
        final StringBuilder buffer = new StringBuilder();
        synchronized(lock){
            boolean first=true;
            for(String exceptionName : exceptionsList){
                if(first){
                    first = false;
                } else{
                    buffer.append(',');
                }
                buffer.append(exceptionName);
            }
        }
        exceptionCheckString = buffer.toString();
    }

    public String getID(){
        return "ProhibitedExceptionThrown";
    }

    public String getDisplayName(){
        return InspectionGadgetsBundle.message("bad.exception.thrown.display.name");
    }

    public JComponent createOptionsPanel(){
        final Form form = new Form();
        return form.getContentPanel();
    }

    @NotNull
    public String buildErrorString(Object... infos){
        final PsiType type = (PsiType)infos[0];
        final String exceptionName = type.getPresentableText();
        return InspectionGadgetsBundle.message(
                "bad.exception.thrown.problem.descriptor", exceptionName);
    }

    public BaseInspectionVisitor buildVisitor(){
        return new BadExceptionThrownVisitor();
    }

    private class BadExceptionThrownVisitor extends BaseInspectionVisitor{

        public void visitThrowStatement(PsiThrowStatement statement){
            super.visitThrowStatement(statement);
            final PsiExpression exception = statement.getException();
            if(exception == null){
                return;
            }
            final PsiType type = exception.getType();
            if(type == null){
                return;
            }
            final String text = type.getCanonicalText();
            final Set<String> exceptionListCopy;
            synchronized(lock){
                exceptionListCopy = new HashSet<String>(exceptionsList);
            }
            if(exceptionListCopy.contains(text)){
                registerStatementError(statement, type);
            }
        }
    }

    private class Form{
        
        JPanel contentPanel;
        JButton addButton;
        JButton deleteButton;
        JTable table;

        Form(){
            super();
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.setRowSelectionAllowed(true);
            table.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            final ReturnCheckSpecificationTableModel model =
                    new ReturnCheckSpecificationTableModel();
            table.setModel(model);
            addButton.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e){
                    final int listSize;
                    synchronized(lock){
                        listSize = exceptionsList.size();
                        exceptionsList.add("");
                    }
                    model.fireTableStructureChanged();
                    EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            final Rectangle rect = table.getCellRect(listSize,
                                    0, true);
                            table.scrollRectToVisible(rect);
                            table.editCellAt(listSize, 0);
                            final TableCellEditor editor = table.getCellEditor();
                            final Component component =
                                    editor.getTableCellEditorComponent(table,
                                            null, true, listSize, 0);
                            component.requestFocus();
                        }
                    });
                }
            });
            deleteButton.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e){
                    final int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length == 0) {
                        return;
                    }
                    final int row = selectedRows[selectedRows.length - 1] - 1;
                    Arrays.sort(selectedRows);
                    synchronized(lock){
                        for(int i = selectedRows.length - 1; i >= 0; i--){
                            exceptionsList.remove(selectedRows[i]);
                        }
                    }
                    model.fireTableStructureChanged();
                    final int count = table.getRowCount();
                    if (count <= row) {
                        table.setRowSelectionInterval(count - 1, count - 1);
                    } else if (row < 0) {
                        if (count > 0) {
                            table.setRowSelectionInterval(0, 0);
                        }
                    } else {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            });
        }

        public JComponent getContentPanel(){
            return contentPanel;
        }
    }

    private class ReturnCheckSpecificationTableModel extends AbstractTableModel{
        public int getRowCount(){
            synchronized(lock){
                return exceptionsList.size();
            }
        }

        public int getColumnCount(){
            return 1;
        }

        public String getColumnName(int columnIndex){
            return InspectionGadgetsBundle.message(
                    "exception.class.column.name");
        }

        public Class<?> getColumnClass(int columnIndex){
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex){
            return true;
        }

        public Object getValueAt(int rowIndex, int columnIndex){
            synchronized(lock){
                return exceptionsList.get(rowIndex);
            }
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex){
            synchronized(lock){
                exceptionsList.set(rowIndex, (String) aValue);
            }
        }
    }
}

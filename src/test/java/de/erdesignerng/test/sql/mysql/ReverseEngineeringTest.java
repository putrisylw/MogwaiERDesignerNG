/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.test.sql.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import de.erdesignerng.dialect.Dialect;
import de.erdesignerng.dialect.ReverseEngineeringOptions;
import de.erdesignerng.dialect.ReverseEngineeringStrategy;
import de.erdesignerng.dialect.TableNamingEnum;
import de.erdesignerng.dialect.mysql.MySQLDialect;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.IndexExpression;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.Table;
import de.erdesignerng.model.View;
import de.erdesignerng.modificationtracker.HistoryModificationTracker;
import de.erdesignerng.test.sql.AbstractReverseEngineeringTest;

/**
 * Test for XML based model io.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-11-16 17:48:26 $
 */
public class ReverseEngineeringTest extends AbstractReverseEngineeringTest {

    public void testReverseEngineerMySQL() throws Exception {

        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Connection theConnection = null;
        try {
            theConnection = DriverManager.getConnection("jdbc:mysql://127.0.0.1/mysql", "root", "root");
            
            Statement theStatement = theConnection.createStatement();
            try {
                theStatement.execute("DROP USER mogwai");
                theStatement.execute("DROP DATABASE mogwai");
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            try {
                theStatement.execute("CREATE DATABASE MOGWAI");
                theStatement.execute("CREATE USER mogwai IDENTIFIED BY 'mogwai'");
                theStatement.execute("GRANT ALL ON MOGWAI.* TO mogwai");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            theConnection.close();
            
            theConnection = DriverManager.getConnection("jdbc:mysql://127.0.0.1/mogwai", "mogwai", "mogwai");

            loadSQL(theConnection, "db.sql");

            Dialect theDialect = new MySQLDialect();
            ReverseEngineeringStrategy<MySQLDialect> theST = theDialect.getReverseEngineeringStrategy();

            Model theModel = new Model();
            theModel.setDialect(theDialect);
            theModel.setModificationTracker(new HistoryModificationTracker(theModel));

            ReverseEngineeringOptions theOptions = new ReverseEngineeringOptions();
            theOptions.setTableNaming(TableNamingEnum.STANDARD);
            theOptions.getTableEntries().addAll(
                    theST.getTablesForSchemas(theConnection, theST.getSchemaEntries(theConnection)));

            theST.updateModelFromConnection(theModel, new EmptyWorldConnector(), theConnection, theOptions,
                    new EmptyReverseEngineeringNotifier());

            // Implement Unit Tests here
            Table theTable = theModel.getTables().findByName("table1");
            assertTrue(theTable != null);
            Attribute theAttribute = theTable.getAttributes().findByName("tb2_1");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable() == false);
            assertTrue(theAttribute.getDatatype().getName().equals("VARCHAR"));
            assertTrue(theAttribute.getSize() == 20);
            theAttribute = theTable.getAttributes().findByName("tb2_2");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable());
            assertTrue(theAttribute.getDatatype().getName().equals("VARCHAR"));
            assertTrue(theAttribute.getSize() == 100);
            theAttribute = theTable.getAttributes().findByName("tb2_3");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable() == false);
            assertTrue(theAttribute.getDatatype().getName().equals("DECIMAL"));
            assertTrue(theAttribute.getSize() == 20);
            assertTrue(theAttribute.getFraction() == 5);

            theTable = theModel.getTables().findByName("table2");
            assertTrue(theTable != null);
            theAttribute = theTable.getAttributes().findByName("tb3_1");
            assertTrue(theAttribute != null);
            theAttribute = theTable.getAttributes().findByName("tb3_2");
            assertTrue(theAttribute != null);
            theAttribute = theTable.getAttributes().findByName("tb3_3");
            assertTrue(theAttribute != null);
            
            Index thePK = theTable.getPrimarykey();
            assertTrue(thePK != null);
            assertTrue(thePK.getExpressions().findByAttributeName("tb3_1") != null);

            View theView = theModel.getViews().findByName("view1");
            assertTrue(theView != null);
            
            Relation theRelation = theModel.getRelations().findByName("FK1");
            assertTrue(theRelation != null);
            assertTrue("table1".equals(theRelation.getImportingTable().getName()));
            assertTrue("table2".equals(theRelation.getExportingTable().getName()));

            assertTrue(theRelation.getMapping().size() == 1);
            Map.Entry<IndexExpression, Attribute> theEntry = theRelation.getMapping().entrySet().iterator().next();
            assertTrue("tb2_1".equals(theEntry.getValue().getName()));
            assertTrue("tb3_1".equals(theEntry.getKey().getAttributeRef().getName()));


        } finally {
            if (theConnection != null) {

                theConnection.close();
            }
        }
    }
}
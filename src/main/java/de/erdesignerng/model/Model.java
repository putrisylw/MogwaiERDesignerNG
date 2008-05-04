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
package de.erdesignerng.model;

import java.sql.Connection;
import java.sql.SQLException;

import de.erdesignerng.dialect.Dialect;
import de.erdesignerng.exception.CannotDeleteException;
import de.erdesignerng.exception.ElementAlreadyExistsException;
import de.erdesignerng.exception.ElementInvalidNameException;
import de.erdesignerng.modificationtracker.EmptyModelModificationTracker;
import de.erdesignerng.modificationtracker.ModelModificationTracker;
import de.erdesignerng.modificationtracker.VetoException;
import de.erdesignerng.util.ApplicationPreferences;
import de.erdesignerng.util.RecentlyUsedConnection;

/**
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-05-04 17:24:43 $
 */
public class Model implements OwnedModelItemVerifier {

    public static final String PROPERTY_DRIVER = "DRIVER";

    public static final String PROPERTY_URL = "URL";

    public static final String PROPERTY_USER = "USER";

    public static final String PROPERTY_PASSWORD = "PASSWORD";

    private TableList tables = new TableList();

    private RelationList relations = new RelationList();
    
    private SubjectAreaList subjectAreas = new SubjectAreaList();

    private Dialect dialect;

    private ModelProperties properties = new ModelProperties();
    
    private ModelModificationTracker modificationTracker = new EmptyModelModificationTracker();

    /**
     * Add a table to the database model.
     * 
     * @param aTable
     *            the table
     * @throws ElementAlreadyExistsException is thrown in case of an error
     * @throws ElementInvalidNameException is thrown in case of an error
     * @throws VetoException if there is a veto for doing this
     */
    public void addTable(Table aTable) throws ElementAlreadyExistsException, ElementInvalidNameException , VetoException {

        modificationTracker.addTable(aTable);
        
        ModelUtilities.checkNameAndExistance(tables, aTable, dialect);

        for (Attribute theAttribute : aTable.getAttributes()) {
            theAttribute.setName(dialect.checkName(theAttribute.getName()));
        }

        aTable.setOwner(this);
        tables.add(aTable);
    }

    /**
     * Add a relation to the database model.
     * 
     * @param aRelation
     *            the table
     * @throws ElementAlreadyExistsException is thrown in case of an error
     * @throws ElementInvalidNameException is thrown in case of an error
     * @throws VetoException is thrown in case of an error
     */
    public void addRelation(Relation aRelation) throws ElementAlreadyExistsException, ElementInvalidNameException , VetoException {

        ModelUtilities.checkNameAndExistance(relations, aRelation, dialect);

        aRelation.setOwner(this);        
        modificationTracker.addRelation(aRelation);

        relations.add(aRelation);
    }

    public void checkNameAlreadyExists(ModelItem aSender, String aName) throws ElementAlreadyExistsException {
        if (aSender instanceof Table) {
            ModelUtilities.checkExistance(tables, aName, dialect);
        }
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect aDialect) {
        dialect = aDialect;
    }

    public void delete(ModelItem aSender) throws CannotDeleteException {
        if (aSender instanceof Table) {

            Table theTable = (Table) aSender;

            if (relations.isTableInUse(theTable)) {
                throw new CannotDeleteException("Table is used by relations!");
            }

            tables.remove(theTable);

            return;
        }

        if (aSender instanceof Relation) {

            Relation theRelation = (Relation) aSender;

            relations.remove(theRelation);

            return;
        }

        throw new UnsupportedOperationException("Unknown element " + aSender);
    }

    public String checkName(String aName) throws ElementInvalidNameException {
        return dialect.checkName(aName);
    }

    public RelationList getRelations() {
        return relations;
    }

    public TableList getTables() {
        return tables;
    }

    public ModelProperties getProperties() {
        return properties;
    }

    public Connection createConnection(ApplicationPreferences aPreferences) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, SQLException {
        Connection theConnection = getDialect().createConnection(aPreferences.createDriverClassLoader(),
                properties.getProperty(Model.PROPERTY_DRIVER), properties.getProperty(Model.PROPERTY_URL),
                properties.getProperty(Model.PROPERTY_USER), properties.getProperty(Model.PROPERTY_PASSWORD));
        return theConnection;
    }

    public boolean checkIfUsedAsForeignKey(Table aTable, Attribute aAttribute) {
        Attribute theRealAttribute = aTable.getAttributes().findBySystemId(aAttribute.getSystemId());
        return getRelations().isForeignKeyAttribute(theRealAttribute);
    }

    /**
     * Remove a table from the model.
     * 
     * @param aTable the table
     * @throws VetoException will be thrown if the modificationtracker has a veto for completing this operation
     */
    public void removeTable(Table aTable) throws VetoException {
        
        modificationTracker.removeTable(aTable);
        
        tables.remove(aTable);
        relations.removeByTable(aTable);
        
        subjectAreas.removeTable(aTable);
    }

    /**
     * Remove a relation from the model.
     * 
     * @param aRelation the relation
     * @throws VetoException will be thrown if the modificationtracker has a veto for completing this operation
     */
    public void removeRelation(Relation aRelation) throws VetoException {
        
        modificationTracker.removeRelation(aRelation);
        relations.remove(aRelation);
    }

    public void removeAttributeFromTable(Table aTable, Attribute aAttribute) throws VetoException {
        
        modificationTracker.removeAttributeFromTable(aTable, aAttribute);
        aTable.getAttributes().removeById(aAttribute.getSystemId());
    }

    public void removeIndex(Table aTable, Index aIndex) throws VetoException {
        
        if (IndexType.PRIMARYKEY.equals(aIndex.getIndexType())) {
            modificationTracker.removePrimaryKeyFromTable(aTable, aIndex);
        } else {
            modificationTracker.removeIndexFromTable(aTable, aIndex);
        }
        aTable.getIndexes().removeById(aIndex.getSystemId());
    }

    public void addAttributeToTable(Table aTable, Attribute aAttribute) throws VetoException, ElementAlreadyExistsException, ElementInvalidNameException {
        
        modificationTracker.addAttributeToTable(aTable, aAttribute);
        aTable.addAttribute(this, aAttribute);
    }

    public void changeAttribute(Attribute aExistantAttribute, Attribute aNewAttribute) throws Exception {
        
        modificationTracker.changeAttribute(aExistantAttribute, aNewAttribute);
        
        aExistantAttribute.restoreFrom(aNewAttribute);
    }

    public void addIndexToTable(Table aTable, Index aIndex) throws VetoException, ElementAlreadyExistsException, ElementInvalidNameException {
        
        if (IndexType.PRIMARYKEY.equals(aIndex.getIndexType())) {
            modificationTracker.addPrimaryKeyToTable(aTable, aIndex);
        } else {
            modificationTracker.addIndexToTable(aTable, aIndex);
        }
        
        aTable.addIndex(this, aIndex);
    }

    public void changeIndex(Index aExistantIndex, Index aNewIndex) throws Exception {

        modificationTracker.changeIndex(aExistantIndex, aNewIndex);
        
        aExistantIndex.restoreFrom(aNewIndex);        
    }

    public void renameTable(Table aTable, String aNewName) throws VetoException {
        
        modificationTracker.renameTable(aTable, aNewName);
        
        aTable.setName(aNewName);
    }

    public void changeTableComment(Table aTable, String aNewComment) throws VetoException {
        
        modificationTracker.changeTableComment(aTable, aNewComment);
        
        aTable.setComment(aNewComment);
    }

    public void renameAttribute(Attribute aExistantAttribute, String aNewName) throws VetoException {
        modificationTracker.renameAttribute(aExistantAttribute, aNewName);
        
        aExistantAttribute.setName(aNewName);
    }

    public void changeRelation(Relation aRelation, Relation aTempRelation) throws Exception {
        
        modificationTracker.changeRelation(aRelation, aTempRelation);
        aRelation.restoreFrom(aTempRelation);
    }

    /**
     * @return the modificationTracker
     */
    public ModelModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    /**
     * @param modificationTracker the modificationTracker to set
     */
    public void setModificationTracker(ModelModificationTracker modificationTracker) {
        this.modificationTracker = modificationTracker;
    }
    
    /**
     * Add a new subject area.
     * 
     * @param aArea the area
     */
    public void addSubjectArea(SubjectArea aArea) {
        subjectAreas.add(aArea);
    }

    /**
     * Remove a subject area.
     *  
     * @param aArea the area
     */
    public void removeSubjectArea(SubjectArea aArea) {
        subjectAreas.remove(aArea);
    }

    /**
     * @return the subjectAreas
     */
    public SubjectAreaList getSubjectAreas() {
        return subjectAreas;
    }

    /**
     * Create a connection history entry for the current loaded connection.
     * 
     * @return the history entry
     */
    public RecentlyUsedConnection createConnectionHistoryEntry() {
        return new RecentlyUsedConnection(dialect.getUniqueName(), getProperties().getProperty(PROPERTY_URL), getProperties().getProperty(PROPERTY_USER));
    }
}
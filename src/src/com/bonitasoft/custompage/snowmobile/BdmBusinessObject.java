package com.bonitasoft.custompage.snowmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;

import com.bonitasoft.custompage.snowmobile.BdmField.enumRelationType;

public class BdmBusinessObject {

    /**
     * the business Object is referenced in another businessObject. The key is the name (qualified name).
     */
    private final HashMap<String, BdmBusinessObject> setBusinessFather = new HashMap<String, BdmBusinessObject>();

    // this class is use to store a Index or a Constraints
    public class BdmListOfFields {

        public String name;
        public boolean isIndex;
        private final Set<String> setFieldsName = new HashSet<String>();

        BdmBusinessObject businessObject;

        public BdmListOfFields(final BdmBusinessObject businessObject, final boolean isIndex) {
            this.businessObject = businessObject;
            this.isIndex = isIndex;
        };

        public String getLogId() {
            return name;
        }

        public String getSqlName() {
            return name.toLowerCase();
        };

        public void addField(final String fieldName) {
            setFieldsName.add(fieldName);
        }

        public Set<String> getListFields() {
            return setFieldsName;
        }

        public BdmBusinessObject getBusinessObject() {
            return businessObject;
        }
    }

    public String name;
    /** table name, in lower case */
    public String tableName;

    public List<BdmField> listFields;
    public List<BdmListOfFields> listConstraints = new ArrayList<BdmListOfFields>();
    public List<BdmListOfFields> listIndexes = new ArrayList<BdmListOfFields>();

    public String getLogId() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public void addReferencedIn(final BdmBusinessObject bdmBusinessObjectFather) {
        setBusinessFather.put(bdmBusinessObjectFather.getName(), bdmBusinessObjectFather);
    }

    /**
     * return all the father business for this businessObject
     *
     * @return
     */
    public HashMap<String, BdmBusinessObject> getBusinessFather() {
        return setBusinessFather;
    }

    /**
     * read the BDM from an XML contains
     *
     * @param oneBusinessObjectNode
     * @return
     */
    public void readFromXml(final Node oneBusinessObjectNode, final OperationStatus operationStatus) {
        name = XmlToolbox.getXmlAttribute(oneBusinessObjectNode, "qualifiedName");
        tableName = getTableNameFromBonitaName(name);

        Node childBusinessNode = XmlToolbox.getNextChildElement(oneBusinessObjectNode.getFirstChild());
        while (childBusinessNode != null) {
            if (childBusinessNode.getNodeName().equals("fields")) {
                readFields(childBusinessNode, operationStatus);
            } else if (childBusinessNode.getNodeName().equals("uniqueConstraints")) {
                readUniqueConstraints(childBusinessNode, operationStatus);
            } else if (childBusinessNode.getNodeName().equals("indexes")) {
                readIndexes(childBusinessNode, operationStatus);
            }
            childBusinessNode = XmlToolbox.getNextChildElement(childBusinessNode.getNextSibling());
        }
        return;
    }

    private String getTableNameFromBonitaName(final String name) {
        final int posIndex = name.lastIndexOf(".");
        if (posIndex != -1) {
            return name.substring(posIndex + 1).toLowerCase();
        } else {
            return name.toLowerCase();
        }

    }

    /**
     * @return
     */
    public HashMap<String, Object> getJsonDescription() {
        final HashMap<String, Object> description = new HashMap<String, Object>();
        description.put("name", name);
        // fields
        final ArrayList<HashMap<String, Object>> listJsonField = new ArrayList<HashMap<String, Object>>();
        description.put("fields", listJsonField);
        for (final BdmField field : listFields) {
            final HashMap<String, Object> jsonField = new HashMap<String, Object>();
            jsonField.put("name", field.name);
            jsonField.put("sqlcolname", field.getSqlColName());
            jsonField.put("nullable", Boolean.valueOf(field.nullable));
            jsonField.put("collection", Boolean.valueOf(field.collection));
            if (field.isRelationField) {
                jsonField.put("isrelationfield", Boolean.TRUE);
                jsonField.put("type", field.relationType);
                jsonField.put("sqlreferencetable", field.getSqlReferenceTable());

            } else {
                jsonField.put("isrelationfield", Boolean.FALSE);
                jsonField.put("type", field.fieldType);
                jsonField.put("length", field.fieldLength);
            }
            listJsonField.add(jsonField);
        }

        // uniqueconstraints
        final ArrayList<HashMap<String, Object>> listJsonConstraints = new ArrayList<HashMap<String, Object>>();
        description.put("uniqueconstraints", listJsonConstraints);
        for (final BdmListOfFields constraints : listConstraints) {
            final HashMap<String, Object> jsonConstraints = new HashMap<String, Object>();
            final ArrayList<String> jsonListFieldName = new ArrayList<String>();

            jsonConstraints.put("name", constraints.name);
            jsonConstraints.put("fieldnames", jsonListFieldName);
            for (final String fieldName : constraints.getListFields()) {
                jsonListFieldName.add(fieldName);
            }
            listJsonConstraints.add(jsonConstraints);
        }
        // index
        final ArrayList<HashMap<String, Object>> listJsonIndexes = new ArrayList<HashMap<String, Object>>();
        description.put("indexes", listJsonIndexes);
        for (final BdmListOfFields index : listIndexes) {
            final HashMap<String, Object> jsonIndex = new HashMap<String, Object>();
            final ArrayList<String> jsonListFieldName = new ArrayList<String>();

            jsonIndex.put("name", index.name);
            jsonIndex.put("fieldnames", jsonListFieldName);
            for (final String fieldName : index.getListFields()) {
                jsonListFieldName.add(fieldName);
            }
            listJsonIndexes.add(jsonIndex);
        }
        return description;
    }

    /**
     * read the fields part of the BDM
     *
     * @param childFieldsNode
     * @return
     */
    private void readFields(final Node childFieldsNode, final OperationStatus operationStatus) {
        listFields = new ArrayList<BdmField>();

        Node fieldNode = XmlToolbox.getNextChildElement(childFieldsNode.getFirstChild());
        while (fieldNode != null) {
            final BdmField field = new BdmField(this);
            if (fieldNode.getNodeName().equals("relationField")) {
                field.isRelationField = true;
                field.name = XmlToolbox.getXmlAttribute(fieldNode, "name");
                try {
                    field.relationType = enumRelationType.valueOf(XmlToolbox.getXmlAttribute(fieldNode, "type"));
                } catch (final IllegalArgumentException e) {
                    operationStatus.addErrorMsg(
                            "Illegal relation BusinessObject[" + getName() + "] field[" + field.name + "] type get["
                                    + XmlToolbox.getXmlAttribute(fieldNode, "type") + "] expected ["
                                    + enumRelationType.AGGREGATION.toString() + ","
                                    + enumRelationType.COMPOSITION.toString() + "] in a relationfield.");
                    field.relationType = enumRelationType.AGGREGATION;
                }
                field.fieldType = "LONG";
                field.nullable = "true".equals(XmlToolbox.getXmlAttribute(fieldNode, "nullable"));
                field.collection = "true".equals(XmlToolbox.getXmlAttribute(fieldNode, "collection"));
                field.reference = XmlToolbox.getXmlAttribute(fieldNode, "reference");
                field.referenceSqlTable = getTableNameFromBonitaName(field.reference);

                listFields.add(field);
            }
            if (fieldNode.getNodeName().equals("field")) {
                field.isRelationField = false;
                field.fieldType = XmlToolbox.getXmlAttribute(fieldNode, "type");
                field.name = XmlToolbox.getXmlAttribute(fieldNode, "name");
                field.fieldLength = XmlToolbox.getXmlAttributeInteger(fieldNode, "length", 0);
                field.nullable = "true".equals(XmlToolbox.getXmlAttribute(fieldNode, "nullable"));
                field.collection = "true".equals(XmlToolbox.getXmlAttribute(fieldNode, "collection"));
                listFields.add(field);
            }
            fieldNode = XmlToolbox.getNextChildElement(fieldNode.getNextSibling());
        }
        return;
    }

    /**
     * read the Unique Constraints of the BDM
     *
     * @param childBusinessNode
     * @return
     */
    private void readUniqueConstraints(final Node childBusinessNode, final OperationStatus operationStatus) {

        listConstraints = new ArrayList<BdmListOfFields>();

        Node constraintNode = XmlToolbox.getNextChildElement(childBusinessNode.getFirstChild());
        while (constraintNode != null) {
            if (constraintNode.getNodeName().equals("uniqueConstraint")) {
                final BdmListOfFields constraint = new BdmListOfFields(this, false);
                constraint.name = XmlToolbox.getXmlAttribute(constraintNode, "name");
                // read all field
                final Node fieldNamesNode = XmlToolbox.getNextChildElement(constraintNode.getFirstChild());
                if (fieldNamesNode != null) {
                    Node oneFieldNameNode = XmlToolbox.getNextChildElement(fieldNamesNode.getFirstChild());
                    while (oneFieldNameNode != null) {
                        constraint.addField(XmlToolbox.getNodeValue(oneFieldNameNode));
                        oneFieldNameNode = XmlToolbox.getNextChildElement(oneFieldNameNode.getNextSibling());

                    }
                }
                listConstraints.add(constraint);
            }
            constraintNode = XmlToolbox.getNextChildElement(constraintNode.getNextSibling());
        }
        return;
    }

    /**
     * read Indexes of the BDM
     *
     * @param childBusinessNode
     * @return
     */
    private void readIndexes(final Node childBusinessNode, final OperationStatus operationStatus) {
        listIndexes = new ArrayList<BdmListOfFields>();

        Node indexNode = XmlToolbox.getNextChildElement(childBusinessNode.getFirstChild());
        while (indexNode != null) {
            if (indexNode.getNodeName().equals("index")) {
                final BdmListOfFields index = new BdmListOfFields(this, true);
                index.name = XmlToolbox.getXmlAttribute(indexNode, "name");
                // read all field
                final Node fieldNamesNode = XmlToolbox.getNextChildElement(indexNode.getFirstChild());
                if (fieldNamesNode != null) {
                    Node oneFieldNameNode = XmlToolbox.getNextChildElement(fieldNamesNode.getFirstChild());
                    while (oneFieldNameNode != null) {
                        index.addField(XmlToolbox.getNodeValue(oneFieldNameNode));
                        oneFieldNameNode = XmlToolbox.getNextChildElement(oneFieldNameNode.getNextSibling());

                    }
                }
                listIndexes.add(index);
            }
            indexNode = XmlToolbox.getNextChildElement(indexNode.getNextSibling());
        }
        return;
    }

    public String getName() {
        return name;
    }

    /** return the SqlTableName. It's in lower case anytime */
    public String getSqlTableName() {
        return tableName;
    }

    public List<BdmField> getListFields() {
        return listFields;
    }

    public BdmField getFieldBySqlColumnName(final String colName) {
        for (final BdmField bdmField : listFields) {
            if (bdmField.getSqlColName().equalsIgnoreCase(colName)) {
                return bdmField;
            }
        }
        return null;
    }

    public BdmField getFieldByFieldName(final String fieldName) {
        for (final BdmField bdmField : listFields) {
            if (bdmField.getFieldName().equals(fieldName)) {
                return bdmField;
            }
        }
        return null;
    }

    public List<BdmListOfFields> getListConstraints() {
        return listConstraints;
    }

    public List<BdmListOfFields> getListIndexes() {
        return listIndexes;
    }

    /**
     * the bdm can refere a another BDM as a reference (COMPOSITION or AGREGATION). Search this fields
     *
     * @return
     */
    public BdmField getFieldReference(final String referenceName) {
        for (final BdmField bdmField : listFields) {
            if (referenceName.equals(bdmField.getReference())) {
                return bdmField;
            }
        }
        return null;
    }

}

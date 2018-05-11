package com.bonitasoft.custompage.snowmobile;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XmlToolbox {

    public static String getXmlAttribute(Node node, String attributName) {
        try {
            NamedNodeMap nodeMap = node.getAttributes();
            if (nodeMap == null)
                return null;
            Node attr = nodeMap.getNamedItem(attributName);
            return (attr == null ? null : attr.getNodeValue());
        } catch (Exception e) {
            // The exception is raised when trying to get an attribute
            // which doen't exist
            // The attribute can not exist because this method is called by both
            // the package and the configuration and somme attribute can not
            // exist when
            // reading the xml file. in this case, a null value is returned.
            // It's not an error
            return null;
        }
    }

    public static Integer getXmlAttributeInteger(Node node, String attributName, Integer defaultValue) {
        try {
            return Integer.valueOf(getXmlAttribute(node, attributName));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // -----------------------------------------------------------------------
    /**
     * get the embedded text for a given xml node
     * 
     * @param node
     * @param theLog
     * @return
     */
    public static String getNodeValue(Node node) {
        try {
            // return the value of the node
            Node child = node.getFirstChild();
            String value = null;
            while (child != null) {
                // if the child is a PCDATA element, return the value of this
                // PCDATA
                if ((child.getNodeType() == Node.TEXT_NODE) && (!child.getNodeValue().equals("\n"))) {
                    if (child.getNodeValue() != null) {
                        value = child.getNodeValue();
                    } else {
                    }
                }
                child = child.getNextSibling();
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    // ----------------------------------------------------------------------
    /**
     * check the node, and stop if it is a ELEMENT_NODE, else go the next one.
     * Stop when a ELEMENT_NODE is found, or no node
     */
    public static Node getNextChildElement(Node node) {
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE)
                return node;

            node = node.getNextSibling();
        }
        return node;
    }

}

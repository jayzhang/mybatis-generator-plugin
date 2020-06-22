package org.jay.mybatis.generator.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

public class XmlGeneratorUtils {
	
    public static XmlElement getForeachFieldsElement(String text)
    {
    	XmlElement foreach = new XmlElement("foreach");
		foreach.addAttribute(new Attribute("collection", "fields"));
		foreach.addAttribute(new Attribute("item", "field"));
		foreach.addAttribute(new Attribute("open", ""));
		foreach.addAttribute(new Attribute("close", ""));
		foreach.addAttribute(new Attribute("separator", ","));
		foreach.addElement(new TextElement(text));
		return foreach;
    }
    
    
    public static XmlElement getBaseColumnListElement(IntrospectedTable introspectedTable) {
        XmlElement answer = new XmlElement("include"); //$NON-NLS-1$
        answer.addAttribute(new Attribute("refid", //$NON-NLS-1$
                introspectedTable.getBaseColumnListId()));
        return answer;
    }

    public static XmlElement getBlobColumnListElement(IntrospectedTable introspectedTable) {
        XmlElement answer = new XmlElement("include"); //$NON-NLS-1$
        answer.addAttribute(new Attribute("refid", //$NON-NLS-1$
                introspectedTable.getBlobColumnListId()));
        return answer;
    }
    
    public static XmlElement getIfOrderByElement()
    {
    	XmlElement ifElement = new XmlElement("if"); //$NON-NLS-1$
        ifElement.addAttribute(new Attribute("test", "orderByClause != null")); //$NON-NLS-1$ //$NON-NLS-2$
        ifElement.addElement(new TextElement("order by ${orderByClause}")); //$NON-NLS-1$
        return ifElement;
    }
    
    public static XmlElement getExampleIncludeElement(IntrospectedTable introspectedTable) 
    {
        XmlElement ifElement = new XmlElement("if"); //$NON-NLS-1$
        ifElement.addAttribute(new Attribute("test", "_parameter != null")); //$NON-NLS-1$ //$NON-NLS-2$

        XmlElement includeElement = new XmlElement("include"); //$NON-NLS-1$
        includeElement.addAttribute(new Attribute("refid", //$NON-NLS-1$
                introspectedTable.getExampleWhereClauseId()));
        ifElement.addElement(includeElement);

        return ifElement;
    }
}

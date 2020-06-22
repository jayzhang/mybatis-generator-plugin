package org.jay.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jay.mybatis.generator.plugin.utils.DateISO8601TimeDeserializer;
import org.jay.mybatis.generator.plugin.utils.DateISO8601TimeSerializer;
import org.jay.mybatis.generator.plugin.utils.ISO8601TimeDeserializer;
import org.jay.mybatis.generator.plugin.utils.ISO8601TimeSerializer;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.OutputUtilities;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;
import org.mybatis.generator.config.GeneratedKey;
import org.mybatis.generator.config.PropertyRegistry;
import org.mybatis.generator.internal.util.JavaBeansUtil;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import lombok.Getter;

public class MybatisPlusPlugin extends PluginAdapter {
    
	private String targetProject;
	private String targetPackage;
	
	private GeneratedJavaFile getJavaFile(CompilationUnit compilationUnit)
	{
		GeneratedJavaFile gjf = new GeneratedJavaFile(compilationUnit,
				targetProject,
                context.getProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING),
                context.getJavaFormatter());
		
		return gjf;
	}
	
	@Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        
        targetProject = properties.getProperty("targetProject");
        targetPackage = properties.getProperty("targetPackage");
    }
	
	
	private ServiceProperties getServiceProperties(IntrospectedTable introspectedTable)
	{
		ServiceProperties serviceProperties = new ServiceProperties();
        for (Object key : properties.keySet()) {
			String k = (String) key;
			if (k.contains(".")) {
				List<String> arr = Splitter.on(".").splitToList(k);
				String table = arr.get(0);
				if (table.equals("*") || table.equals(introspectedTable.getFullyQualifiedTableNameAtRuntime())) {
					String method = arr.get(1);
					String v = (String) properties.get(key);
					try {
						java.lang.reflect.Field f = ServiceProperties.class.getDeclaredField(method);
						if (f != null) {
							try {
								f.set(serviceProperties, Boolean.parseBoolean(v));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (NoSuchFieldException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (SecurityException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
        return serviceProperties;
	}
	
	@Override
	public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {

		ServiceProperties serviceProperties = getServiceProperties(introspectedTable);
		MybatisPlusServiceGenerator generator = new MybatisPlusServiceGenerator(targetProject, introspectedTable, serviceProperties,
				targetPackage);
		generator.setContext(context);
		List<GeneratedJavaFile> javaFiles = generator.getCompilationUnits().stream().map(i -> getJavaFile(i))
				.collect(Collectors.toList());

		return javaFiles;
	}
	 
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
    	
    	ServiceProperties serviceProperties = getServiceProperties(introspectedTable);
    	
    	if(serviceProperties.optimizedPage)
    	{
    		addPageXml(document.getRootElement(), introspectedTable);
    	}
    	
    	if(serviceProperties.createBatch)
    	{
    		BatchInsertElementGenerator elementGenerator = new BatchInsertElementGenerator();
            elementGenerator.setContext(context);
            elementGenerator.setIntrospectedTable(introspectedTable);
            elementGenerator.addElements(document.getRootElement());
    	}
    	
        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }
    
    /**
     * 性能优化版分页查询
     */
    private void addPageXml(XmlElement parentElement, IntrospectedTable introspectedTable)
    {
    	XmlElement resultMap = new XmlElement("resultMap"); //$NON-NLS-1$
        resultMap.addAttribute(new Attribute("id", //$NON-NLS-1$
                introspectedTable.getBaseResultMapId()));

        String returnType = introspectedTable.getBaseRecordType();
        
        resultMap.addAttribute(new Attribute("type", //$NON-NLS-1$
                returnType));

        for (IntrospectedColumn introspectedColumn : introspectedTable
                .getPrimaryKeyColumns()) {
            XmlElement resultElement = new XmlElement("id"); //$NON-NLS-1$

            resultElement.addAttribute(generateColumnAttribute(introspectedColumn));
            resultElement.addAttribute(new Attribute(
                    "property", introspectedColumn.getJavaProperty())); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute("jdbcType", //$NON-NLS-1$
                    introspectedColumn.getJdbcTypeName()));

            if (stringHasValue(introspectedColumn.getTypeHandler())) {
                resultElement.addAttribute(new Attribute(
                        "typeHandler", introspectedColumn.getTypeHandler())); //$NON-NLS-1$
            }

            resultMap.addElement(resultElement);
        }
        for (IntrospectedColumn introspectedColumn : introspectedTable.getNonPrimaryKeyColumns()) {
            XmlElement resultElement = new XmlElement("result"); //$NON-NLS-1$

            resultElement.addAttribute(generateColumnAttribute(introspectedColumn));
            resultElement.addAttribute(new Attribute(
                    "property", introspectedColumn.getJavaProperty())); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute("jdbcType", //$NON-NLS-1$
                    introspectedColumn.getJdbcTypeName()));

            if (stringHasValue(introspectedColumn.getTypeHandler())) {
                resultElement.addAttribute(new Attribute(
                        "typeHandler", introspectedColumn.getTypeHandler())); //$NON-NLS-1$
            }

            resultMap.addElement(resultElement);
        }

        parentElement.addElement(resultMap);
        
        XmlElement select = new XmlElement("select");
        parentElement.addElement(select);
        
        select.addAttribute(new Attribute("id", "page"));
        select.addAttribute(new Attribute("parameterType", introspectedTable.getBaseRecordType()));
        select.addAttribute(new Attribute("resultMap", "BaseResultMap"));
        select.addElement(new TextElement("select"));
        
        XmlElement choose = new XmlElement("choose");
        select.addElement(choose);
        
        XmlElement when = new XmlElement("when");
        choose.addElement(when);
        when.addAttribute(new Attribute("test", "fields != null"));
        
        XmlElement foreach0 = new XmlElement("foreach");
        when.addElement(foreach0);
        foreach0.addAttribute(new Attribute("close", ""));
        foreach0.addAttribute(new Attribute("open", ""));
        foreach0.addAttribute(new Attribute("collection", "fields"));
        foreach0.addAttribute(new Attribute("item", "field"));
        foreach0.addAttribute(new Attribute("separator", ","));
        foreach0.addElement(new TextElement("B.${field} AS ${field}"));
        
        XmlElement otherwise = new XmlElement("otherwise");
        choose.addElement(otherwise);
        
        StringBuilder sb = new StringBuilder();
        Iterator<IntrospectedColumn> iter = introspectedTable
                .getNonBLOBColumns().iterator();
        while (iter.hasNext()) {
        	sb.append("B.");
        	String field = MyBatis3FormattingUtilities.getSelectListPhrase(iter
                    .next());
            sb.append(field);
            sb.append(" AS ");
            sb.append(field);
            if (iter.hasNext()) {
                sb.append(", "); //$NON-NLS-1$
            }

            if (sb.length() > 80) {
            	otherwise.addElement(new TextElement(sb.toString()));
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
        	otherwise.addElement(new TextElement(sb.toString()));
        }
        
        List<String> pklist = introspectedTable.getPrimaryKeyColumns().stream().map(i->i.getActualColumnName()).collect(Collectors.toList());
        String pks = Joiner.on(",").join(pklist);
        
        select.addElement(new TextElement("FROM ( SELECT " + pks + " FROM ${tableName} ")) ;
        
        XmlElement ifwhere = new XmlElement("if");
        select.addElement(ifwhere);
        
        XmlElement where = new XmlElement("where");
        ifwhere.addElement(where);
        
        XmlElement trim = new XmlElement("trim");
        trim.addAttribute(new Attribute("prefix",  "("));
        trim.addAttribute(new Attribute("prefixOverrides",  "AND"));
        trim.addAttribute(new Attribute("suffix",  ")"));
        where.addElement(trim);
        
        List<String> fieldsNotNulls = new ArrayList<>();
        
        List<IntrospectedColumn> cols = introspectedTable.getNonBLOBColumns();
        for (IntrospectedColumn col : cols) 
        { 
        	JSONObject columnMeta = JSON.parseObject(col.getRemarks());
        	if(columnMeta == null)
        		continue;
        	String filter =  columnMeta.getString(CommentJsonKeys.FILTER);
        	if(filter == null)
        	{
        		continue;
        	}
        	List<String> filterOpts = Splitter.on(",").splitToList(filter);
        	if(filterOpts.contains(FilterOperators.IN))  //如果有in，则省略eq
        	{
        		filterOpts = filterOpts.stream().filter(i->!FilterOperators.EQ.equals(i)).collect(Collectors.toList());
        	}
        	String fieldName = col.getJavaProperty();
        	String colName = col.getActualColumnName();
            for(String opt: filterOpts)
            {
            	XmlElement ifcmp = new XmlElement("if");
            	String f = fieldName;
            	if(FilterOperators.EQ.equals(opt))
            	{
            		ifcmp.addElement(new TextElement("AND "+ colName + " = #{" + f + "}"));
            	}
            	else if(FilterOperators.GT.equals(opt))
            	{
            		f += "Gt";
            		ifcmp.addElement(new TextElement("AND "+ colName + " <![CDATA[>]]> #{" + f + "}"));
            	}
            	else if(FilterOperators.GTE.equals(opt))
            	{
            		f += "Gte";
            		ifcmp.addElement(new TextElement("AND "+ colName + " <![CDATA[>=]]> #{" + f + "}"));
            	}
            	else if(FilterOperators.LTE.equals(opt))
            	{
            		f += "Lte";
            		ifcmp.addElement(new TextElement(" AND "+ colName + " <![CDATA[<=]]> #{" + f + "}"));
            	}
            	else if(FilterOperators.LT.equals(opt))
            	{
            		f += "Lt";
            		ifcmp.addElement(new TextElement("AND " + colName + " <![CDATA[<]]> #{" + f + "}"));
            	}
            	else if(FilterOperators.IN.equals(opt))
            	{
            		ifcmp.addElement(new TextElement("AND " + colName + " IN "));
            		XmlElement foreach = new XmlElement("foreach");
            		foreach.addAttribute(new Attribute("open",  "("));
            		foreach.addAttribute(new Attribute("close",  ")"));
            		foreach.addAttribute(new Attribute("collection",  fieldName));
            		foreach.addAttribute(new Attribute("item",  "listItem"));
            		foreach.addElement(new TextElement("#{listItem}"));
            		foreach.addAttribute(new Attribute("separator",  ","));
            		ifcmp.addElement(foreach);
            	}
            	else if(FilterOperators.LIKE.equals(opt))
            	{
            		f += "Like";
            		ifcmp.addElement(new TextElement("AND " + colName + " LIKE #{" + f + "}"));
            	}
            	ifcmp.addAttribute(new Attribute("test",  f + " != null"));
            	trim.addElement(ifcmp);
            	fieldsNotNulls.add(f + " != null");
            }
        }
        
        String testif = Joiner.on(" or ").join(fieldsNotNulls);
        ifwhere.addAttribute(new Attribute("test", testif));
        
        XmlElement iforder = new XmlElement("if");
        select.addElement(iforder);
        iforder.addElement(new TextElement("ORDER BY "));
        iforder.addAttribute(new Attribute("test", "orderByFields != null"));
       
        XmlElement foreachOrder = new XmlElement("foreach");
        iforder.addElement(foreachOrder);
        foreachOrder.addAttribute(new Attribute("open",  ""));
        foreachOrder.addAttribute(new Attribute("close", ""));
        foreachOrder.addAttribute(new Attribute("collection",  "orderByFields"));
        foreachOrder.addAttribute(new Attribute("item",  "field"));
        foreachOrder.addAttribute(new Attribute("separator",  ","));
        foreachOrder.addElement(new TextElement("${field.field}"));
        XmlElement chooseAsc = new XmlElement("choose");
        foreachOrder.addElement(chooseAsc);
        XmlElement whenAsc = new XmlElement("when");
        chooseAsc.addElement(whenAsc);
        whenAsc.addAttribute(new Attribute("test",  "field.asc"));
        whenAsc.addElement(new TextElement("ASC"));
        XmlElement otherwiseAsc = new XmlElement("otherwise");
        otherwiseAsc.addElement(new TextElement("DESC"));
        chooseAsc.addElement(otherwiseAsc);
        
        String oncond = Joiner.on(" AND ").join(pklist.stream().map(i-> ("A." + i + " = B." + i)).collect(Collectors.toList()));
        
        select.addElement(new TextElement("LIMIT #{skip} , #{limit} ) AS A LEFT JOIN ${tableName} AS B ON " + oncond));
    }
    
    private Attribute generateColumnAttribute(IntrospectedColumn introspectedColumn) {
        return new Attribute("column", //$NON-NLS-1$
                MyBatis3FormattingUtilities.getRenamedColumnNameForResultMap(introspectedColumn));
    }
    
    @Override
    public boolean clientGenerated(Interface interfaze,
            IntrospectedTable introspectedTable) {
    	ServiceProperties serviceProperties = getServiceProperties(introspectedTable);
    	if(serviceProperties.createBatch)
    	{
    		Method method = getBatchInsertMethod("insertBatch", introspectedTable);
    		interfaze.addMethod(method);
    		interfaze.addImportedType(FullyQualifiedJavaType.getNewListInstance());
    	}
    	
    	interfaze.addImportedType(new FullyQualifiedJavaType(BaseMapper.class.getName()));
    	interfaze.addImportedType(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
    	
    	FullyQualifiedJavaType baseMapper = new FullyQualifiedJavaType(BaseMapper.class.getSimpleName());
    	String modelSimpleTypeName = StringUtils.substringAfterLast(introspectedTable.getBaseRecordType(), ".");
    	baseMapper.addTypeArgument(new FullyQualifiedJavaType(modelSimpleTypeName));
    	
    	interfaze.addSuperInterface(baseMapper);
    	
    	
    	if(serviceProperties.optimizedPage)
    	{
    		FullyQualifiedJavaType listType = FullyQualifiedJavaType.getNewListInstance();
    		listType.addTypeArgument(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
        	Method method = new Method("page");
    		method.setVisibility(JavaVisibility.PUBLIC);
    		method.setReturnType(listType);
    		method.setAbstract(true);
    		String modelClassName = introspectedTable.getBaseRecordType() + "List";
    		String modelClassNameSingle = StringUtils.substringAfterLast(modelClassName, ".");
    		method.addParameter(new Parameter(new FullyQualifiedJavaType(modelClassNameSingle), "list"));
    		interfaze.addMethod(method);
    		interfaze.addImportedType(new FullyQualifiedJavaType(modelClassName));
    		interfaze.addImportedType(FullyQualifiedJavaType.getNewListInstance());
    	}
    	
        return true;
    }
    
    private Method getBatchInsertMethod(String name, IntrospectedTable introspectedTable)
    {
    	FullyQualifiedJavaType listType = FullyQualifiedJavaType.getNewListInstance();
		listType.addTypeArgument(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
    	Method method = new Method(name);
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.setAbstract(true);
		method.addParameter(new Parameter(listType, "records"));
		return method;
    }
	
	 
	public class BatchInsertElementGenerator extends AbstractXmlElementGenerator 
	{    
	    @Override
	    public void addElements(XmlElement parentElement) {
	    	XmlElement answer = new XmlElement("insert"); //$NON-NLS-1$

	        answer.addAttribute(new Attribute(
	                "id", "insertBatch")); //$NON-NLS-1$

	        answer.addAttribute(new Attribute("parameterType", //$NON-NLS-1$
	        		List.class.getName()));

	        GeneratedKey gk = introspectedTable.getGeneratedKey();
	        if (gk != null) {
	            introspectedTable.getColumn(gk.getColumn()).ifPresent(introspectedColumn -> {
	                // if the column is null, then it's a configuration error. The
	                // warning has already been reported
	                if (gk.isJdbcStandard()) {
	                    answer.addAttribute(new Attribute(
	                            "useGeneratedKeys", "true")); //$NON-NLS-1$ //$NON-NLS-2$
	                    answer.addAttribute(new Attribute(
	                            "keyProperty", introspectedColumn.getJavaProperty())); //$NON-NLS-1$
	                    answer.addAttribute(new Attribute(
	                            "keyColumn", introspectedColumn.getActualColumnName())); //$NON-NLS-1$
	                } else {
	                    answer.addElement(getSelectKey(introspectedColumn, gk));
	                }
	            });
	        }

	        StringBuilder insertClause = new StringBuilder();

	        insertClause.append("insert into "); //$NON-NLS-1$
	        insertClause.append(introspectedTable
	                .getFullyQualifiedTableNameAtRuntime());
	        insertClause.append(" ("); //$NON-NLS-1$

	        XmlElement foreach = new XmlElement("foreach");
	        foreach.addAttribute(new Attribute("collection","list"));
	        foreach.addAttribute(new Attribute("item","item"));
	        foreach.addAttribute(new Attribute("separator",","));
	        foreach.addAttribute(new Attribute("open",""));
	        foreach.addAttribute(new Attribute("close",""));
	        
	        
	        StringBuilder valuesClause = new StringBuilder();
	        valuesClause.append(" ("); //$NON-NLS-1$

	        List<String> valuesClauses = new ArrayList<>();
	        List<IntrospectedColumn> columns =
	                ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
	        for (int i = 0; i < columns.size(); i++) {
	            IntrospectedColumn introspectedColumn = columns.get(i);

	            insertClause.append(MyBatis3FormattingUtilities
	                    .getEscapedColumnName(introspectedColumn));
	            
	            valuesClause.append("#{item."); //$NON-NLS-1$
	            valuesClause.append(introspectedColumn.getJavaProperty());
	            valuesClause.append(",jdbcType="); //$NON-NLS-1$
	            valuesClause.append(introspectedColumn.getJdbcTypeName());

	            if (stringHasValue(introspectedColumn.getTypeHandler())) {
	            	valuesClause.append(",typeHandler="); //$NON-NLS-1$
	            	valuesClause.append(introspectedColumn.getTypeHandler());
	            }

	            valuesClause.append('}');
	            
	            if (i + 1 < columns.size()) {
	                insertClause.append(", "); //$NON-NLS-1$
	                valuesClause.append(", "); //$NON-NLS-1$
	            }

	            if (valuesClause.length() > 80) {
	                answer.addElement(new TextElement(insertClause.toString()));
	                insertClause.setLength(0);
	                OutputUtilities.xmlIndent(insertClause, 1);

	                valuesClauses.add(valuesClause.toString());
	                valuesClause.setLength(0);
	                OutputUtilities.xmlIndent(valuesClause, 1);
	            }
	        }

	        insertClause.append(")\n\tvalues");
	        answer.addElement(new TextElement(insertClause.toString()));
	        answer.addElement(foreach);
	        
	        valuesClause.append(')');
	        valuesClauses.add(valuesClause.toString());

	        for (String clause : valuesClauses) {
	        	foreach.addElement(new TextElement(clause));
	        }
	        parentElement.addElement(new TextElement("\n"));
	        parentElement.addElement(answer);
	    }
	}


	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}
	
	
	
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
    	JavaGeneratorUtils.cleanClass(topLevelClass);//清理掉原来的class，重新生成
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        JavaGeneratorUtils.addLombokAnnotations(topLevelClass);
        JavaGeneratorUtils.addSerializable(topLevelClass); //序列化
        topLevelClass.addAnnotation("@TableName(\""+introspectedTable.getFullyQualifiedTableNameAtRuntime()+"\")");
        topLevelClass.addAnnotation("@JsonInclude(Include.NON_NULL) ");
        topLevelClass.addImportedType(JsonInclude.class.getName());
        topLevelClass.addImportedType(JsonInclude.class.getName() + ".Include");
        topLevelClass.addImportedType(TableName.class.getName());
        topLevelClass.addSuperInterface(new FullyQualifiedJavaType("Cloneable"));
        List<IntrospectedColumn> introspectedColumns = JavaGeneratorUtils.getColumnsInThisClass(introspectedTable);
        for (IntrospectedColumn introspectedColumn : introspectedColumns) 
        {   
        	
        	JSONObject columnMeta = JSON.parseObject(introspectedColumn.getRemarks());  
            Field field = JavaBeansUtil.getJavaBeansField(introspectedColumn, context, introspectedTable);
            if(introspectedColumn.isIdentity() && introspectedColumn.isAutoIncrement())
        	{
        		field.addAnnotation("@TableId(type=IdType.AUTO)");
        		topLevelClass.addImportedType(IdType.class.getName());
        		topLevelClass.addImportedType(TableId.class.getName());
        	}
            JavaGeneratorUtils.addFieldComment(field, columnMeta);
            JavaGeneratorUtils.addFieldAnnotations(topLevelClass, field, columnMeta);
            if(JavaGeneratorUtils.isTimeField(columnMeta))
            {
            	field.addAnnotation("@JsonDeserialize(using=ISO8601TimeDeserializer.class)");
            	field.addAnnotation("@JsonSerialize(using=ISO8601TimeSerializer.class)");
            	topLevelClass.addImportedType(ISO8601TimeDeserializer.class.getName());
            	topLevelClass.addImportedType(ISO8601TimeSerializer.class.getName());
            	topLevelClass.addImportedType(JsonDeserialize.class.getName());
            	topLevelClass.addImportedType(JsonSerialize.class.getName());
            }
            else if(introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedName().equals(Date.class.getName()))
        	{
        		field.addAnnotation("@JsonDeserialize(using=DateISO8601TimeDeserializer.class)");
            	field.addAnnotation("@JsonSerialize(using=DateISO8601TimeSerializer.class)");
            	topLevelClass.addImportedType(DateISO8601TimeDeserializer.class.getName());
            	topLevelClass.addImportedType(DateISO8601TimeSerializer.class.getName());
            	topLevelClass.addImportedType(JsonDeserialize.class.getName());
            	topLevelClass.addImportedType(JsonSerialize.class.getName());
        	}
            
            JavaGeneratorUtils.addStringLengthValidationAnnotations(topLevelClass, introspectedColumn, field);
            topLevelClass.addField(field);
            topLevelClass.addImportedType(field.getType());
        }
        
        InnerEnum enumClass = getFieldEnumClass(introspectedTable);
        topLevelClass.addInnerEnum(enumClass);
        topLevelClass.addImportedType(new FullyQualifiedJavaType(Getter.class.getName()));
        
        Method cloneMethod = new Method("clone");
        cloneMethod.setVisibility(JavaVisibility.PUBLIC);
        cloneMethod.setReturnType(topLevelClass.getType());
        cloneMethod.addBodyLine(topLevelClass.getType().getShortName() + " obj = new " + topLevelClass.getType().getShortName() + "();");
        cloneMethod.addBodyLine("BeanUtils.copyProperties(this, obj);");
        cloneMethod.addBodyLine("return obj;");
        topLevelClass.addMethod(cloneMethod);
        topLevelClass.addImportedType(BeanUtils.class.getName());
        return true;
    }
	

    private InnerEnum getFieldEnumClass(IntrospectedTable table)
    {
    	String className = "Fields";
    	InnerEnum enumClass = new InnerEnum(className);
    	enumClass.setVisibility(JavaVisibility.PUBLIC);
    	
    	Field field = new Field("column", FullyQualifiedJavaType.getStringInstance());
    	field.setVisibility(JavaVisibility.PRIVATE);
    	field.addAnnotation("@Getter");
    	
    	enumClass.addField(field);
    	
    	for(IntrospectedColumn col: table.getAllColumns())
    	{
    		enumClass.addEnumConstant(col.getJavaProperty() + "(\"" + col.getActualColumnName() + "\")");
    	}
    	
    	Method method = new Method(className);
    	method.setConstructor(true);
    	method.setVisibility(JavaVisibility.PRIVATE);
    	method.addParameter(new Parameter(FullyQualifiedJavaType.getStringInstance(), "column"));
    	method.addBodyLine("this.column = column;");
    	
    	enumClass.addMethod(method);
    	
    	return enumClass;
    	
    }
	

    @Override
    public boolean clientBasicCountMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicDeleteMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicInsertMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicInsertMultipleMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicInsertMultipleHelperMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicSelectManyMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicSelectOneMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientBasicUpdateMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGeneralCountMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGeneralDeleteMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGeneralSelectDistinctMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGeneralSelectMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientGeneralUpdateMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMultipleMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }


    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectListFieldGenerated(Field field, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectOneMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateAllColumnsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateSelectiveColumnsMethodGenerated(Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(
            Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        return false;
    }


    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        return false;
    }
   
    @Override
    public boolean sqlMapResultMapWithoutBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapCountByExampleElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByExampleElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    /**
     * 生成mapper.xml,文件内容会被清空再写入
     * */
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        sqlMap.setMergeable(false);
        return super.sqlMapGenerated(sqlMap, introspectedTable);
    }

    @Override
    public boolean sqlMapInsertElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapResultMapWithBLOBsElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
    	
    }

    @Override
    public boolean sqlMapBaseColumnListElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapBlobColumnListElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

    
    @Override
    public boolean clientSelectAllMethodGenerated(Method method,
            Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectAllElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        return false;
    }

}
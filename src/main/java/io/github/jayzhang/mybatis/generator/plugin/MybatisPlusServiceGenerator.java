/**
 *    Copyright 2006-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.jayzhang.mybatis.generator.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.PrimitiveTypeWrapper;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.codegen.AbstractJavaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import io.github.jayzhang.mybatis.generator.plugin.utils.DateISO8601TimeDeserializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.DateISO8601TimeSerializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.ISO8601TimeDeserializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.ISO8601TimeSerializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.ListResult;
import io.github.jayzhang.mybatis.generator.plugin.utils.MyDateTimeFormat;
import io.github.jayzhang.mybatis.generator.plugin.utils.OrderByField;
import io.github.jayzhang.mybatis.generator.plugin.utils.String2StringListDeserializer;

public class MybatisPlusServiceGenerator extends AbstractJavaGenerator {

	private String targetPackage;
	
	private String modelName;
	private String modelFullname;
	
	private String mapperName;
	private String mapperFieldName;
	private String mapperFullname;
	
	private String serviceName;
	private String modelListFullname;
	
	private ServiceProperties prop;
	
	public MybatisPlusServiceGenerator(String targetProject, IntrospectedTable introspectedTable
			, ServiceProperties prop, String targetPackage) 
	{
		super(targetProject);
		super.introspectedTable = introspectedTable;
		
    	this.targetPackage = targetPackage;
    	this.prop = prop;
    	
    	mapperFullname = introspectedTable.getMyBatis3JavaMapperType();
		mapperName = org.apache.commons.lang.StringUtils.substringAfterLast(mapperFullname, ".");
		
		modelFullname = introspectedTable.getBaseRecordType();
		modelName = org.apache.commons.lang.StringUtils.substringAfterLast(modelFullname, ".");
		modelListFullname = modelFullname + "List";
		
		serviceName = modelName + "Service";
		
		mapperFieldName = firstLower(mapperName);
	}
	
    private Field getPrivateField(FullyQualifiedJavaType t, String name)
    {
    	Field field = new Field(name, t);
    	field.setVisibility(JavaVisibility.PRIVATE);
    	return field;
    }
    
	
    private String firstUpper(String name)
    {
    	return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    private String firstLower(String name)
    {
    	return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    
    
    private TopLevelClass getServiceClass()
    {
        TopLevelClass topLevelClass = new TopLevelClass(targetPackage + "." + serviceName);
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        topLevelClass.addAnnotation("@Service");
        topLevelClass.addImportedType(Service.class.getName());
        
        topLevelClass.addImportedType(introspectedTable.getMyBatis3JavaMapperType());
        topLevelClass.addImportedType(introspectedTable.getBaseRecordType());
        topLevelClass.addImportedType(Autowired.class.getName());
        String mapperFieldName = firstLower(mapperName);
        
        Field field = new Field(mapperFieldName, new FullyQualifiedJavaType(mapperFullname));
        field.addAnnotation("@Autowired");
        field.setVisibility(JavaVisibility.PROTECTED);
        topLevelClass.addField(field);
        
        if(prop.create)
        	addCreateMethodSingle(topLevelClass);
    	if(prop.update)
    	{
    		 addUpdateMethod1(topLevelClass);
    	    	addUpdateMethod2(topLevelClass);
    	}
    	if(prop.createBatch)
    		addCreateBatchMethodSingle(topLevelClass);
        if(prop.delete)
        	addDeleteMethod(topLevelClass);
        if(prop.get)
        {
        	addGetMethod1(topLevelClass);
            addGetMethod2(topLevelClass);
        }
        if(prop.list)
        {
        	addListMethod(topLevelClass);
        }
        if(prop.listAll)
        	addListAllMethod(topLevelClass);
        
        if(prop.count)
        	addCountMethod(topLevelClass);
        
        return topLevelClass;
    }
 
    private void addCreateMethodSingle(TopLevelClass topLevelClass) 
	{
		Method method = new Method("create");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("int"));
        method.addParameter(new Parameter(new FullyQualifiedJavaType(modelFullname), "create"));
        StringBuilder sb = new StringBuilder();
        boolean nowDeclared = false;
        for (IntrospectedColumn col : JavaGeneratorUtils.getColumnsInThisClass(introspectedTable))
        {	
        	if(!JavaGeneratorUtils.isAutoIncField(col))
        	{
        		JSONObject columnMeta = JSON.parseObject(col.getRemarks());
            	String upperName = firstUpper(col.getJavaProperty());
            	if(JavaGeneratorUtils.isAutoCreateTimeField(columnMeta) 
            			|| JavaGeneratorUtils.isAutoUpdateTimeField(columnMeta))
            	{
            		if(!nowDeclared)
            		{
            			method.addBodyLine("long now = System.currentTimeMillis();");
            			nowDeclared = true;
            		}
            		sb.append("create.set").append(upperName).append("(now);");
                    method.addBodyLine(sb.toString());
                    sb.setLength(0);
            	}
        	}
        }
        sb.append("return ").append(mapperFieldName).append(".insert(create);");
        method.addBodyLine(sb.toString());
        topLevelClass.addMethod(method);
	}
    private void addCreateBatchMethodSingle(TopLevelClass topLevelClass) 
	{
		Method method = new Method("createBatch");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("int"));
        FullyQualifiedJavaType list = FullyQualifiedJavaType.getNewListInstance();
        list.addTypeArgument(new FullyQualifiedJavaType(modelFullname));
        method.addParameter(new Parameter(list, "records"));
        StringBuilder sb = new StringBuilder();
        
        List<IntrospectedColumn> autoTimeColumns = new ArrayList<>();
        for (IntrospectedColumn col : JavaGeneratorUtils.getColumnsInThisClass(introspectedTable))
        {	
        	if(!JavaGeneratorUtils.isAutoIncField(col))
        	{
        		JSONObject columnMeta = JSON.parseObject(col.getRemarks());
            	
            	if(JavaGeneratorUtils.isAutoCreateTimeField(columnMeta) 
            			|| JavaGeneratorUtils.isAutoUpdateTimeField(columnMeta))
            	{
            		autoTimeColumns.add(col);
            	}
        	}
        }
        if(autoTimeColumns.size() > 0)
		{
        	method.addBodyLine("long now = System.currentTimeMillis();");
        	sb.append("for(").append(this.modelName).append(" create : records)");
            method.addBodyLine(sb.toString());
            sb.setLength(0);
            method.addBodyLine("{");
        	for (IntrospectedColumn col :  autoTimeColumns)
        	{
        		String upperName = firstUpper(col.getJavaProperty());
        		sb.append("create.set").append(upperName).append("(now);");
                method.addBodyLine(sb.toString());
                sb.setLength(0);
        	}
        	sb.append("}");
        	method.addBodyLine(sb.toString());
            sb.setLength(0);
		}
        sb.append("return ").append(mapperFieldName).append(".insertBatch(records);");
        method.addBodyLine(sb.toString());
        topLevelClass.addMethod(method);
	}
	
	private void addUpdateMethod1(TopLevelClass topLevelClass) 
	{	
		Method method = new Method("update");
		method.addJavaDocLine("/**\n" + 
				"     * 根据update对象的主键，更新其余非null字段\n" + 
				"     */");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("int"));
        StringBuilder sb = new StringBuilder();
        if(introspectedTable.getPrimaryKeyColumns().size() == 0)
        {
        	return ;
        }
        
        method.addParameter(new Parameter(new FullyQualifiedJavaType(modelFullname), "update"));
       
        boolean now = false;
        List<IntrospectedColumn> columns = introspectedTable.getNonPrimaryKeyColumns(); 
        
        for (IntrospectedColumn col : columns) 
        {
        	JSONObject columnMeta = JSON.parseObject(col.getRemarks());
            if(JavaGeneratorUtils.isAutoUpdateTimeField(columnMeta))
            {
            	String upperName = firstUpper(col.getJavaProperty());
            	if(!now)
            	{
            		method.addBodyLine("long now = System.currentTimeMillis();");
            		now = true;
            	}
            	sb.append("update.set").append(upperName).append("(now);");
            	method.addBodyLine(sb.toString());
            	sb.setLength(0);
            }
        }
        int updateFields = 0;
        for (IntrospectedColumn col : columns) 
        {
            if(JavaGeneratorUtils.isUpdateField(col))
        	{
                ++ updateFields;
        	}
        }
        if(updateFields == 0)
        {
        	return ;
        }
         
        method.addBodyLine("return "+mapperFieldName+".updateById(update);");
        topLevelClass.addMethod(method);
	}
	
	private void addUpdateMethod2(TopLevelClass topLevelClass) 
	{	
		Method method = new Method("update");
		method.addJavaDocLine("/**\n" + 
				"     * 根据update对象的主键字段作为WHERE条件，更新其余出现在fields列表中的字段\n" + 
				"     * @param update 记录的主键字段作为WHERE条件语句，非主键字段作为SET语句\n" + 
				"     * @param fields 只更新fields字段对应的数据列，如果fields为null则强制更新所有update记录的字段，包括值为null字段\n" + 
				"     * @return \n" + 
				"     */");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("int"));
        StringBuilder sb = new StringBuilder();
        if(introspectedTable.getPrimaryKeyColumns().size() == 0)
        {
        	return ;
        }
        
        method.addParameter(new Parameter(new FullyQualifiedJavaType(modelFullname), "update"));
        FullyQualifiedJavaType stringList = FullyQualifiedJavaType.getNewListInstance();
        stringList.addTypeArgument(FullyQualifiedJavaType.getStringInstance());
        method.addParameter(new Parameter(stringList, "fields"));
        
        method.addBodyLine(modelName + " whereEntity = new " + modelName + "();");
        for(IntrospectedColumn col: introspectedTable.getPrimaryKeyColumns())
        {
        	String up = firstUpper(col.getJavaProperty());
        	method.addBodyLine("whereEntity.set" + up + "(update.get" + up + "());");
        }
        
        method.addBodyLine("UpdateWrapper<" + modelName + "> uw = new UpdateWrapper<>();");
        
        boolean now = false;
        List<IntrospectedColumn> columns = introspectedTable.getNonPrimaryKeyColumns(); 
        
        for (IntrospectedColumn col : columns) 
        {
        	JSONObject columnMeta = JSON.parseObject(col.getRemarks());
            if(JavaGeneratorUtils.isAutoUpdateTimeField(columnMeta))
            {
            	if(!now)
            	{
            		method.addBodyLine("long now = System.currentTimeMillis();");
            		now = true;
            	}
            	sb.append("uw.set(\"").append(col.getActualColumnName()).append("\", now);");
            	method.addBodyLine(sb.toString());
            	sb.setLength(0);
            }
        }
        int updateFields = 0;
        for (IntrospectedColumn col : columns) 
        {
            if(JavaGeneratorUtils.isUpdateField(col))
        	{
                ++ updateFields;
        	}
        }
        if(updateFields == 0)
        {
        	return ;
        }
        method.addBodyLine("uw.setEntity(whereEntity);");
        method.addBodyLine("if(fields == null)");
        method.addBodyLine("{");
        
        for (IntrospectedColumn col : columns) 
        {
            if(JavaGeneratorUtils.isUpdateField(col))
        	{
                sb.append("uw.set(\"").append(col.getActualColumnName()).append("\", update.get")
                .append(firstUpper(col.getJavaProperty())).append("());");
                method.addBodyLine(sb.toString());
                sb.setLength(0);
        	}
        }
        
        method.addBodyLine("}");
        method.addBodyLine("else");
        method.addBodyLine("{");
        method.addBodyLine("for(String f: fields)");
        method.addBodyLine("{");
        method.addBodyLine("try");
        method.addBodyLine("{");
        method.addBodyLine(modelName + ".Fields field = " + modelName + ".Fields.valueOf(f);");
                
        method.addBodyLine("if(field != null)");
        method.addBodyLine("{");
        for (IntrospectedColumn col : columns) 
        {
            if(JavaGeneratorUtils.isUpdateField(col))
        	{
                sb.append("if(\"").append(col.getJavaProperty()).append("\".equals(f))uw.set(field.getColumn(), update.get")
                .append(firstUpper(col.getJavaProperty())).append("());");
                method.addBodyLine(sb.toString());
                sb.setLength(0);
        	}
        }
        
        method.addBodyLine("}");
        method.addBodyLine("}");
        method.addBodyLine("catch(Exception e){}");
        method.addBodyLine("}");
        method.addBodyLine("}");
        
        method.addBodyLine("return "+mapperFieldName+".update(null, uw);");
        topLevelClass.addMethod(method);
        topLevelClass.addImportedType(UpdateWrapper.class.getName());
	}
	
	private void addDeleteMethod(TopLevelClass topLevelClass) 
	{
		Method method = new Method("delete");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("int"));
        for(IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns())
        {
        	topLevelClass.addImportedType(NotNull.class.getName());
        	method.addParameter(new Parameter(column.getFullyQualifiedJavaType(), column.getJavaProperty(), "@NotNull"));
        }
        
        String pkfields = Joiner.on(",").join(introspectedTable.getPrimaryKeyColumns().stream().map(i->i.getJavaProperty()).collect(Collectors.toList()));
        StringBuilder sb = new StringBuilder();
        sb.append("return ").append(mapperFieldName).append(".deleteById(").append(pkfields).append(");");
        method.addBodyLine(sb.toString());
        
        topLevelClass.addMethod(method);
	}
	private void addGetMethod1(TopLevelClass topLevelClass) 
	{
		if (introspectedTable.getNonPrimaryKeyColumns().size() == 0) // 没有非主键字段的Mapp不会生成selectByPrimaryKey方法
		{
			return;
		}
		Method method = new Method("get");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType(modelFullname));
        method.addBodyLine("QueryWrapper<" + modelName + "> q = new QueryWrapper<> ();");
        StringBuilder sb = new StringBuilder();
        for(IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns())
        {
        	topLevelClass.addImportedType(NotNull.class.getName());
        	method.addParameter(new Parameter(column.getFullyQualifiedJavaType(), column.getJavaProperty(), "@NotNull"));
        	topLevelClass.addImportedType(column.getFullyQualifiedJavaType());
        	
        	sb.append("q.eq(\"").append(column.getActualColumnName()).append("\",").append(column.getJavaProperty()).append(");");
        	method.addBodyLine(sb.toString());
        	sb.setLength(0);
        }
       
        sb.append("return ").append(mapperFieldName).append(".selectOne(q);");
        
        method.addBodyLine(sb.toString());
        topLevelClass.addMethod(method);
        topLevelClass.addImportedType(QueryWrapper.class.getName());
	}
	private void addGetMethod2(TopLevelClass topLevelClass) 
	{
		if (introspectedTable.getNonPrimaryKeyColumns().size() == 0) // 没有非主键字段的Mapp不会生成selectByPrimaryKey方法
		{
			return;
		}
		Method method = new Method("get");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType(modelFullname));
        method.addBodyLine("QueryWrapper<" + modelName + "> q = new QueryWrapper<> ();");
        StringBuilder sb = new StringBuilder();
        for(IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns())
        {
        	topLevelClass.addImportedType(NotNull.class.getName());
        	method.addParameter(new Parameter(column.getFullyQualifiedJavaType(), column.getJavaProperty(), "@NotNull"));
        	topLevelClass.addImportedType(column.getFullyQualifiedJavaType());
        	
        	sb.append("q.eq(\"").append(column.getActualColumnName()).append("\",").append(column.getJavaProperty()).append(");");
        	method.addBodyLine(sb.toString());
        	sb.setLength(0);
        }
        
        FullyQualifiedJavaType listType = FullyQualifiedJavaType.getNewListInstance();
        listType.addTypeArgument(FullyQualifiedJavaType.getStringInstance());
        method.addParameter(new Parameter(listType, "fields"));
    	topLevelClass.addImportedType(listType);
        
        method.addBodyLine("if(fields != null){");
        
        method.addBodyLine("List<String> columns = new ArrayList<>();");
        method.addBodyLine("for(String f: fields){");
        
        method.addBodyLine("try");
        method.addBodyLine("{");
        
        method.addBodyLine(modelName + ".Fields field = " + modelName + ".Fields.valueOf(f);");
        
        method.addBodyLine("if(field != null){");
        method.addBodyLine("columns.add(field.getColumn());");
        method.addBodyLine("}");
        
        method.addBodyLine("}");
        method.addBodyLine("catch(Exception e){}");
        
        method.addBodyLine("}");
        method.addBodyLine("if(columns.size() > 0){");
        method.addBodyLine("String select = Joiner.on(\",\").join(columns);");
        method.addBodyLine("q.select(select);");
       
        
        method.addBodyLine("}");
        
        method.addBodyLine("}");
        
        sb.append("return ").append(mapperFieldName).append(".selectOne(q);");
        
        method.addBodyLine(sb.toString());
        topLevelClass.addMethod(method);
        topLevelClass.addImportedType(QueryWrapper.class.getName());
        topLevelClass.addImportedType(Joiner.class.getName()); 
        topLevelClass.addImportedType(ArrayList.class.getName());
	}
	private void addListMethod(TopLevelClass topLevelClass) 
	{	
		Method method = new Method("list");
        method.setVisibility(JavaVisibility.PUBLIC);
        
        FullyQualifiedJavaType returnType = new FullyQualifiedJavaType(ListResult.class.getSimpleName());
        returnType.addTypeArgument(new FullyQualifiedJavaType(modelFullname));
        topLevelClass.addImportedType(ListResult.class.getName());
        
        method.setReturnType(returnType);
        Parameter params = new Parameter(new FullyQualifiedJavaType(modelListFullname), "list", "@Valid");
        method.addParameter(params);
        topLevelClass.addImportedType(Valid.class.getName());
        topLevelClass.addImportedType(modelListFullname);
        
        StringBuilder sb = new StringBuilder();
        sb.append(ListResult.class.getSimpleName()).append("<").append(modelName).append("> res = new ").append(ListResult.class.getSimpleName()).append("<>();");
        method.addBodyLine(sb.toString()); sb.setLength(0);
        
        topLevelClass.addImportedType(QueryWrapper.class.getName());
        method.addBodyLine("QueryWrapper<"+modelName+"> q = new QueryWrapper<> ();");  
        List<IntrospectedColumn> cols = introspectedTable.getNonBLOBColumns(); //非BLOB类型字段才可以被过滤
        
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
        	String upperName = firstUpper(fieldName);
        	String colName = col.getActualColumnName();
            for(String opt: filterOpts)
            {
            	if(FilterOperators.EQ.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("() != null)");
    				sb.append(" q.eq(\"").append(colName).append("\", list.get").append(upperName).append("());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.GT.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Gt() != null)");
    				sb.append(" q.gt(\"").append(colName).append("\", list.get").append(upperName).append("Gt());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.GTE.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Gte() != null)");
            		sb.append(" q.ge(\"").append(colName).append("\", list.get").append(upperName).append("Gte());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LTE.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Lte() != null)");
            		sb.append(" q.le(\"").append(colName).append("\", list.get").append(upperName).append("Lte());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LT.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Lt() != null)");
                    sb.append(" q.lt(\"").append(colName).append("\", list.get").append(upperName).append("Lt());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.IN.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("() != null)");
        			sb.append(" q.in(\"").append(colName).append("\", list.get").append(upperName).append("());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LIKE.equals(opt))
            	{
            		if(col.isStringColumn())
            		{
            			sb.append("if(list.get").append(upperName).append("Like() != null)");
            			sb.append(" q.like(\"").append(colName).append("\", list.get").append(upperName).append("Like());");
                        method.addBodyLine(sb.toString()); sb.setLength(0);
            		}
            	}
            }
        }
        sb.append("if(list.isCount()) res.setTotal(").append(mapperFieldName).append(".selectCount(q));");
        method.addBodyLine(sb.toString()); sb.setLength(0);
        method.addBodyLine("if(list.getLimit() != null && list.getLimit().intValue()==0) return res;"); 
        method.addBodyLine("if(list.getLimit() != null && list.getSkip() == null) list.setSkip(0);");
        
        method.addBodyLine("if(list.getOrderByFields() != null)");
        method.addBodyLine("{");
        method.addBodyLine("for(OrderByField f: list.getOrderByFields())");
        method.addBodyLine("{");
        method.addBodyLine("try");
        method.addBodyLine("{");
        method.addBodyLine(modelName + ".Fields field = " + modelName + ".Fields.valueOf(f.getField());");
        method.addBodyLine("if(field != null){");
        method.addBodyLine("if(f.isAsc())");
        method.addBodyLine("q.orderByAsc(field.getColumn());");
        method.addBodyLine("else");
        method.addBodyLine("q.orderByDesc(field.getColumn());");
        
        method.addBodyLine("}");
        method.addBodyLine("}");
        method.addBodyLine("catch(Exception e){}");
        method.addBodyLine("}");
        method.addBodyLine("}");
        
        method.addBodyLine("if(list.getFields() != null)");
        method.addBodyLine("{");
        method.addBodyLine("List<String> columns =  new ArrayList<>();");
        method.addBodyLine("for(String f: list.getFields())");
        method.addBodyLine("{");
        method.addBodyLine("try");
        method.addBodyLine("{");
        method.addBodyLine(modelName + ".Fields field = " + modelName + ".Fields.valueOf(f);");
        method.addBodyLine("if(field != null)columns.add(field.getColumn());");
        method.addBodyLine("}");
        method.addBodyLine("catch(Exception e){}");
        method.addBodyLine("}");
        
        method.addBodyLine("if(columns.size() > 0)");
        method.addBodyLine("{");
        method.addBodyLine("String select = Joiner.on(\",\").join(columns);");
        method.addBodyLine("if(list.isDistinct())select = \"DISTINCT(\" + select + \")\";");
        method.addBodyLine("q.select(select);");
        method.addBodyLine("}");
       
        method.addBodyLine("}");
        
        method.addBodyLine("if(list.getLimit() != null) //分页");
        method.addBodyLine("{");
        
		if (prop.optimizedPage) {
			method.addBodyLine("res.setData(" + mapperFieldName + ".page(list));");
		} else {
			topLevelClass.addImportedType(Page.class.getName());
			topLevelClass.addImportedType(IPage.class.getName());
			method.addBodyLine("long currentPage = (list.getSkip() / list.getLimit()) + 1;");
			method.addBodyLine("Page<" + modelName + "> page = new Page<>(currentPage, list.getLimit()); ");
			method.addBodyLine("page.setSearchCount(false);");
			method.addBodyLine("page.setOptimizeCountSql(false);");
			method.addBodyLine("IPage<" + modelName + "> ipage = " + mapperFieldName + ".selectPage(page, q);");
			method.addBodyLine("res.setData(ipage.getRecords());");
		}
        
        method.addBodyLine("}");
        
        method.addBodyLine("else //不分页");
        method.addBodyLine("{");
        method.addBodyLine("List<"+modelName+"> ilist = "+mapperFieldName+".selectList(q);");
        method.addBodyLine("res.setData(ilist);");
        method.addBodyLine("}");
        
        method.addBodyLine("return res;");
        topLevelClass.addImportedType(OrderByField.class.getName());
        topLevelClass.addMethod(method);
        topLevelClass.addImportedType(FullyQualifiedJavaType.getNewListInstance());
       
        topLevelClass.addImportedType(ArrayList.class.getName());
        topLevelClass.addImportedType(Joiner.class.getName());
	}
	
	
	private void addListAllMethod(TopLevelClass topLevelClass) 
	{	
		Method method = new Method("listAll");
        method.setVisibility(JavaVisibility.PUBLIC);
        
        FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getNewListInstance();
        returnType.addTypeArgument(new FullyQualifiedJavaType(modelFullname));
        topLevelClass.addImportedType(ListResult.class.getName());
        method.setReturnType(returnType);
        
        FullyQualifiedJavaType fieldsType = FullyQualifiedJavaType.getNewListInstance();
        fieldsType.addTypeArgument(FullyQualifiedJavaType.getStringInstance());
        Parameter params = new Parameter(fieldsType, "fields");
        method.addParameter(params);
        
        method.addBodyLine("QueryWrapper<"+modelName+"> q = new QueryWrapper<> ();");  
        
        
        method.addBodyLine("if(fields != null)");
        method.addBodyLine("{");
        method.addBodyLine("List<String> columns =  new ArrayList<>();");
        method.addBodyLine("for(String f: fields)");
        method.addBodyLine("{");
        method.addBodyLine("try");
        method.addBodyLine("{");
        method.addBodyLine(modelName + ".Fields field = " + modelName + ".Fields.valueOf(f);");
        method.addBodyLine("if(field != null)columns.add(field.getColumn());");
        method.addBodyLine("}");
        method.addBodyLine("catch(Exception e){}");
        method.addBodyLine("}");
        
        method.addBodyLine("if(columns.size() > 0)");
        method.addBodyLine("{");
        method.addBodyLine("String select = Joiner.on(\",\").join(columns);");
        method.addBodyLine("q.select(select);");
        method.addBodyLine("}");
       
        method.addBodyLine("}");
        
        method.addBodyLine("return "+mapperFieldName+".selectList(q);");
        
        topLevelClass.addImportedType(OrderByField.class.getName());
        topLevelClass.addMethod(method);
        topLevelClass.addImportedType(FullyQualifiedJavaType.getNewListInstance());
        topLevelClass.addImportedType(ArrayList.class.getName());
        topLevelClass.addImportedType(Joiner.class.getName());
	}
	private void addCountMethod(TopLevelClass topLevelClass) 
	{	
		Method method = new Method("count");
        method.setVisibility(JavaVisibility.PUBLIC);
        
        FullyQualifiedJavaType returnType = new FullyQualifiedJavaType(ListResult.class.getSimpleName());
        returnType.addTypeArgument(new FullyQualifiedJavaType(modelFullname));
        topLevelClass.addImportedType(ListResult.class.getName());
        
        method.setReturnType(new FullyQualifiedJavaType("int"));
        Parameter params = new Parameter(new FullyQualifiedJavaType(modelListFullname), "list", "@Valid");
        method.addParameter(params);
        topLevelClass.addImportedType(Valid.class.getName());
        topLevelClass.addImportedType(modelListFullname);
        
        StringBuilder sb = new StringBuilder();
        
        topLevelClass.addImportedType(QueryWrapper.class.getName());
        method.addBodyLine("QueryWrapper<"+modelName+"> q = new QueryWrapper<> ();");  
        List<IntrospectedColumn> cols = introspectedTable.getNonBLOBColumns(); //非BLOB类型字段才可以被过滤
        
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
        	String upperName = firstUpper(fieldName);
        	String colName = col.getActualColumnName();
            for(String opt: filterOpts)
            {
            	if(FilterOperators.EQ.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("() != null)");
    				sb.append(" q.eq(\"").append(colName).append("\", list.get").append(upperName).append("());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.GT.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Gt() != null)");
    				sb.append(" q.gt(\"").append(colName).append("\", list.get").append(upperName).append("Gt());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.GTE.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Gte() != null)");
            		sb.append(" q.ge(\"").append(colName).append("\", list.get").append(upperName).append("Gte());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LTE.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Lte() != null)");
            		sb.append(" q.le(\"").append(colName).append("\", list.get").append(upperName).append("Lte());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LT.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("Lt() != null)");
                    sb.append(" q.lt(\"").append(colName).append("\", list.get").append(upperName).append("Lt());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.IN.equals(opt))
            	{
            		sb.append("if(list.get").append(upperName).append("() != null)");
        			sb.append(" q.in(\"").append(colName).append("\", list.get").append(upperName).append("());");
                    method.addBodyLine(sb.toString()); sb.setLength(0);
            	}
            	else if(FilterOperators.LIKE.equals(opt))
            	{
            		if(col.isStringColumn())
            		{
            			sb.append("if(list.get").append(upperName).append("Like() != null)");
            			sb.append(" q.like(\"").append(colName).append("\", list.get").append(upperName).append("Like());");
                        method.addBodyLine(sb.toString()); sb.setLength(0);
            		}
            	}
            }
        }
        sb.append("return ").append(mapperFieldName).append(".selectCount(q);");
        method.addBodyLine(sb.toString()); sb.setLength(0);
        topLevelClass.addMethod(method);
	}
	private TopLevelClass getListModelClass()
    {
        TopLevelClass topLevelClass = new TopLevelClass(modelListFullname);
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        JavaGeneratorUtils.addLombokAnnotations(topLevelClass);
        JavaGeneratorUtils.addSerializable(topLevelClass); //序列化
        List<IntrospectedColumn> introspectedColumns = introspectedTable.getNonBLOBColumns();  //非BLOB类型字段才可以被过滤

        for (IntrospectedColumn col : introspectedColumns) 
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
            for(String opt: filterOpts)
            {
            	Field field = null;
            	if(FilterOperators.EQ.equals(opt))
            	{
            		field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName);
            	}
            	else if(FilterOperators.GT.equals(opt))
            	{
            		field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName + "Gt");
            	}
            	else if(FilterOperators.GTE.equals(opt))
            	{
            		field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName + "Gte");
            	}
            	else if(FilterOperators.LTE.equals(opt))
            	{
            		field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName + "Lte");
            	}
            	else if(FilterOperators.LT.equals(opt))
            	{
            		field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName + "Lt");
            	}
            	else if(FilterOperators.IN.equals(opt))
            	{
            		FullyQualifiedJavaType listType = FullyQualifiedJavaType.getNewListInstance();
                    listType.addTypeArgument(col.getFullyQualifiedJavaType());
                    field = getPrivateField(listType, fieldName);
                    FullyQualifiedJavaType type = col.getFullyQualifiedJavaType();
                    String shortTypeName = type.getShortName();
                    String className = "org.jay.mybatis.generator.plugin.utils.String2" + shortTypeName + "ListDeserializer";
                    try {
						Class<?> cls = Class.forName(className);
						field.addAnnotation("@JsonDeserialize(using="+cls.getSimpleName()+".class)");
						topLevelClass.addImportedType(className);
						topLevelClass.addImportedType(listType);
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            	else if(FilterOperators.LIKE.equals(opt))
            	{
            		if(col.isStringColumn())
            		{
            			field = getPrivateField(col.getFullyQualifiedJavaType(), fieldName + "Like");
            		}
            	}
            	if(JavaGeneratorUtils.isTimeField(columnMeta) && !FilterOperators.IN.equals(opt))
                {
                	field.addAnnotation("@JsonDeserialize(using=ISO8601TimeDeserializer.class)");
                	field.addAnnotation("@JsonSerialize(using=ISO8601TimeSerializer.class)");
                	
                	topLevelClass.addImportedType(ISO8601TimeDeserializer.class.getName());
                	topLevelClass.addImportedType(ISO8601TimeSerializer.class.getName());
                	topLevelClass.addImportedType(JsonDeserialize.class.getName());
                	topLevelClass.addImportedType(JsonSerialize.class.getName());
                	field.addAnnotation("@MyDateTimeFormat");
                	topLevelClass.addImportedType(MyDateTimeFormat.class.getName());
                }
            	else if(col.getFullyQualifiedJavaType().getFullyQualifiedName().equals(Date.class.getName()) && !FilterOperators.IN.equals(opt))
            	{
            		field.addAnnotation("@JsonDeserialize(using=DateISO8601TimeDeserializer.class)");
                	field.addAnnotation("@JsonSerialize(using=DateISO8601TimeSerializer.class)");
                	topLevelClass.addImportedType(DateISO8601TimeDeserializer.class.getName());
                	topLevelClass.addImportedType(DateISO8601TimeSerializer.class.getName());
                	topLevelClass.addImportedType(JsonDeserialize.class.getName());
                	topLevelClass.addImportedType(JsonSerialize.class.getName());
            	}
            	topLevelClass.addField(field);
            }
        }
        Field f = getPrivateField(PrimitiveTypeWrapper.getIntegerInstance(), "skip");
        f.setInitializationString("0");
        f.addAnnotation("@Min(0)");
        topLevelClass.addField(f);
        f = getPrivateField(PrimitiveTypeWrapper.getIntegerInstance(), "limit");
        f.setInitializationString("20");
        f.addAnnotation("@Min(0)");
        topLevelClass.addField(f);
        f = getPrivateField(PrimitiveTypeWrapper.getBooleanPrimitiveInstance(), "count");
        f.setInitializationString("true");
        topLevelClass.addField(f);
        topLevelClass.addImportedType(Min.class.getName());
        FullyQualifiedJavaType listType = FullyQualifiedJavaType.getNewListInstance();
        listType.addTypeArgument(FullyQualifiedJavaType.getStringInstance());
        Field fieldsField = getPrivateField(listType, "fields");
        fieldsField.addAnnotation("@JsonDeserialize(using=String2StringListDeserializer.class)");
        topLevelClass.addField(fieldsField);
        topLevelClass.addImportedType(listType);
        topLevelClass.addImportedType(JsonDeserialize.class.getName());
        topLevelClass.addImportedType(String2StringListDeserializer.class.getName());
        topLevelClass.addField(getPrivateField(PrimitiveTypeWrapper.getBooleanPrimitiveInstance(), "distinct"));
        listType = FullyQualifiedJavaType.getNewListInstance();
        listType.addTypeArgument(new FullyQualifiedJavaType(OrderByField.class.getSimpleName()));
        
        topLevelClass.addImportedType(OrderByField.class.getName());
        
        
        
        Field ff = getPrivateField(FullyQualifiedJavaType.getStringInstance(), "tableName");
        ff.setInitializationString("\"" + this.introspectedTable.getFullyQualifiedTableNameAtRuntime() + "\"");
        topLevelClass.addField(ff);
        
        topLevelClass.addField(getPrivateField(listType, "orderByFields"));
        
        return topLevelClass;
    }

	@Override
	public List<CompilationUnit> getCompilationUnits()
	{
		List<CompilationUnit>  units = new ArrayList<>();
		
		if(prop.list || prop.count)
		{
			units.add(getListModelClass());
		}
		
		units.add(getServiceClass());
		
		return units;
	}
}


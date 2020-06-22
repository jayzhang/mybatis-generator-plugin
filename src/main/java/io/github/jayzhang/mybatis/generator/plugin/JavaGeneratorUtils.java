package io.github.jayzhang.mybatis.generator.plugin;

import java.util.List;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.jayzhang.mybatis.generator.plugin.utils.DateISO8601TimeSerializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.DoubleSerializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.ISO8601TimeDeserializer;
import io.github.jayzhang.mybatis.generator.plugin.utils.ISO8601TimeSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

public class JavaGeneratorUtils {
	
	
	
	public static void cleanClass(TopLevelClass topLevelClass)
    {
    	topLevelClass.getImportedTypes().clear();
    	topLevelClass.getStaticImports().clear();
    	topLevelClass.getAnnotations().clear();
    	topLevelClass.getFields().clear();
    	topLevelClass.getMethods().clear();
    }
    
	public static void addSerializable(TopLevelClass topLevelClass)
	{
		FullyQualifiedJavaType serializable = new FullyQualifiedJavaType("java.io.Serializable"); 
		topLevelClass.addImportedType(serializable);
		topLevelClass.addSuperInterface(serializable);
		Field field = new Field("serialVersionUID", //$NON-NLS-1$
				new FullyQualifiedJavaType("long")); //$NON-NLS-1$
		field.setFinal(true);
		field.setInitializationString("1L"); //$NON-NLS-1$
		field.setStatic(true);
		field.setVisibility(JavaVisibility.PRIVATE);
		topLevelClass.addField(field);
	}
	
	public static boolean includePrimaryKeyColumns(IntrospectedTable introspectedTable) {
        return !introspectedTable.getRules().generatePrimaryKeyClass()
                && introspectedTable.hasPrimaryKeyColumns();
    }

	public static boolean includeBLOBColumns(IntrospectedTable introspectedTable) {
        return !introspectedTable.getRules().generateRecordWithBLOBsClass()
                && introspectedTable.hasBLOBColumns();
    }

	public static List<IntrospectedColumn> getColumnsInThisClass(IntrospectedTable introspectedTable) {
        List<IntrospectedColumn> introspectedColumns;
        if (includePrimaryKeyColumns(introspectedTable)) {
            if (includeBLOBColumns(introspectedTable)) {
                introspectedColumns = introspectedTable.getAllColumns();
            } else {
                introspectedColumns = introspectedTable.getNonBLOBColumns();
            }
        } else {
            if (includeBLOBColumns(introspectedTable)) {
                introspectedColumns = introspectedTable
                        .getNonPrimaryKeyColumns();
            } else {
                introspectedColumns = introspectedTable.getBaseColumns();
            }
        }

        return introspectedColumns;
    }
	
	public static void addFieldComment(Field field, JSONObject meta)
	{
		if(meta != null && meta.getString(CommentJsonKeys.COMMENT) != null)
		{
			field.addJavaDocLine("/**"); //$NON-NLS-1$
			field.addJavaDocLine(" *" + meta.getString(CommentJsonKeys.COMMENT)); //$NON-NLS-1$
			field.addJavaDocLine(" */"); //$NON-NLS-1$
		}
	}
	
	public static void addAnnotationImport(TopLevelClass topLevelClass, String annotation)
	{
		if(annotation == null) return ;
		annotation = annotation.trim();
		Class<?> annotationClass = null;
		if(annotation.startsWith("@NotNull")) 
		{
			annotationClass = NotNull.class;
		}
		else if(annotation.startsWith("@NotBlank"))
		{
			annotationClass = NotBlank.class;
		}
		else if(annotation.contains("@JsonSerialize") && annotation.contains("DateISO8601TimeSerializer.class"))
		{
			annotationClass = DateISO8601TimeSerializer.class;
			topLevelClass.addImportedType(DateISO8601TimeSerializer.class.getName());
			topLevelClass.addImportedType(JsonSerialize.class.getName());
		}
		else if(annotation.contains("@JsonSerialize") && annotation.contains("ISO8601TimeSerializer.class"))
		{
			annotationClass = JsonSerialize.class;
			topLevelClass.addImportedType(ISO8601TimeSerializer.class.getName());
			topLevelClass.addImportedType(JsonSerialize.class.getName());
		}
		else if(annotation.contains("@JsonSerialize") && annotation.contains("DoubleSerializer.class"))
		{
			annotationClass = JsonSerialize.class;
			topLevelClass.addImportedType(DoubleSerializer.class.getName());
			topLevelClass.addImportedType(JsonSerialize.class.getName());
		}
		else if(annotation.contains("@JsonDeserialize") && annotation.contains("ISO8601TimeDeserializer.class"))
		{
			annotationClass = JsonDeserialize.class;
			topLevelClass.addImportedType(ISO8601TimeDeserializer.class.getName());
			topLevelClass.addImportedType(JsonDeserialize.class.getName());
		}
		
		else if(annotation.startsWith("@Pattern"))
		{
			annotationClass = Pattern.class;
		}
		else if(annotation.startsWith("@Size"))
		{
			annotationClass = Size.class;
		}
		else if(annotation.startsWith("@Min"))
		{
			annotationClass = Min.class;
		}
		else if(annotation.startsWith("@Max"))
		{
			annotationClass = Max.class;
		}
		else if(annotation.startsWith("@DecimalMax"))
		{
			annotationClass = DecimalMax.class;
		}
		else if(annotation.startsWith("@DecimalMin"))
		{
			annotationClass = DecimalMin.class;
		}
		else if(annotation.startsWith("@Digits"))
		{
			annotationClass = Digits.class;
		}
		
		if(annotationClass != null)
		{
			topLevelClass.addImportedType(annotationClass.getName());
		}
		else 
		{
			System.out.println("annotation not found: " + annotation);
		}
	}
	
	public static void addFieldAnnotations(TopLevelClass topLevelClass, Field field,  JSONObject columnMeta)
	{
		if(columnMeta == null) return;
		JSONArray list2 = columnMeta.getJSONArray(CommentJsonKeys.ANNOTATIONS);
		if(list2 == null) return ;
		for(Object o: list2)
		{
			if(o instanceof String)
			{
				String annotation = (String) o;
				field.addAnnotation(annotation);
				addAnnotationImport(topLevelClass, annotation);
			}
		}
	}
	
	public static void addLombokAnnotations(TopLevelClass topLevelClass)
	{
		topLevelClass.addImportedType(Data.class.getName());
		topLevelClass.addImportedType(Accessors.class.getName());
		topLevelClass.addImportedType(EqualsAndHashCode.class.getName());
		topLevelClass.addAnnotation("@Data");
		topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = false)");
		topLevelClass.addAnnotation("@Accessors(chain = true)");
	}
	
	public static void addStringLengthValidationAnnotations(TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, Field field)
	{
 		if(introspectedColumn.isStringColumn())
		{
			field.addAnnotation("@Size(max="+introspectedColumn.getLength()+")");
			topLevelClass.addImportedType(Size.class.getName());
		}
	}
	 /**
     * 时间
     */
	public static boolean isTimeField(JSONObject columnMeta)
    {
		if( columnMeta != null && isTrue(columnMeta,CommentJsonKeys.TIME) )
			return true;
		
		return isAutoUpdateTimeField(columnMeta) || isAutoCreateTimeField(columnMeta);
    }
	
    /**
     * 自动更新时间
     */
	public static boolean isAutoUpdateTimeField(JSONObject columnMeta)
    {
    	return columnMeta != null && isTrue(columnMeta,CommentJsonKeys.UPDATE_TIME);
    }
    
    /**
     * 自动创建时间
     */
	public static boolean isAutoCreateTimeField(JSONObject columnMeta)
    {
    	return columnMeta != null && isTrue(columnMeta,CommentJsonKeys.CREATE_TIME);
    }
     
	static private boolean  isTrue(JSONObject columnMeta, String key)
	{
		if(columnMeta == null) return false;
		return columnMeta.getBooleanValue(key) || columnMeta.getIntValue(key)> 0;
	}
    
    /**
     * 更新对象时需要传入的字段
     */
	public static boolean isUpdateField(IntrospectedColumn col)
    {
    	if(col.isIdentity() && col.isAutoIncrement()) //自增主键不允许更新
    	{
    		return false;
    	}
        JSONObject columnMeta = JSON.parseObject(col.getRemarks());
        if(columnMeta != null && isTrue(columnMeta,CommentJsonKeys.UPDATE))
        {
        	return true;
        }
        return false;
    }
    
	public static boolean isAutoIncField(IntrospectedColumn col)
    {
    	if(col.isIdentity() && col.isGeneratedAlways())  
    	{
    		return true;
    	}
    	return false;
    }
    
	public static boolean isAutoGenField(IntrospectedColumn col)
    {
    	if(col.isIdentity() && col.isGeneratedAlways())  
    	{
    		return true;
    	}
    	JSONObject columnMeta = JSON.parseObject(col.getRemarks());
    	if(isAutoUpdateTimeField(columnMeta) || isAutoCreateTimeField(columnMeta))
    	{
    		return true;
    	}
    	return false;
    }
    
    
//	public static boolean isFilterField(IntrospectedColumn introspectedColumn)
//    {
//        if(introspectedColumn.isIdentity()) //主键允许过滤
//        {
//        	return true;
//        }
//        JSONObject columnMeta = JSON.parseObject(introspectedColumn.getRemarks());
//        if(columnMeta != null && isTrue(columnMeta,CommentJsonKeys.FILTER))
//        {
//        	return true;
//        }
//        return false;
//    }
    
}

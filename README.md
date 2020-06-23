# 背景介绍

**MyBatis**是apache的一个开源项目，当前比较流行的一个数据库持久层框架。它对jdbc的操作数据库的过程进行封装，使开发者只需要关注SQL本身，而不需要花费精力去处理例如注册驱动、创建connection、创建statement、手动设置参数、结果集检索等jdbc繁杂的过程代码。Mybatis通过xml或注解的方式将要执行的各种statement（statement、preparedStatemnt、CallableStatement）配置起来，并通过java对象和statement中的sql进行映射生成最终执行的sql语句，最后由mybatis框架执行sql并将结果映射成java对象并返回。

**MyBatis Generator**是MyBatis的代码生成器，它可以根据数据库表结构生成MyBatis执行通用数据库操作CRUD的代码，包括：
-  Java模型对象：对应数据库表对象
-  Mapper文件：Mapper接口Java文件和xml文件。Java接口文件封装了数据库CRUD访问操作；xml文件定义了和Java接口绑定的SQL语句
    
**MyBatis Plus**是MyBatis的增强工具，在MyBatis的基础上增强，将数据库表的通用CRUD操作通过内置的方式进行封装，通用操作的SQL语句无须定义开发者定义或生成，进一步简化开发、提高效率。

本项目以MyBatis Generator插件方式，实现生成基于Mybatis Plus的DAO层代码（entity、mapper)。同时生成基于DAO的service层代码。实现以下通用的service层接口，增强了翻页查询的能力，并对翻页查询的SQL进行了优化，代替默认SQL实现，提升查询性能。

# 插件介绍
插件名字：com.qxwz.mybatis.generator.plugin.MybatisPlusPlugin

## 参考配置文件
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration>
    <classPathEntry
            location="/Users/username/.m2/repository/mysql/mysql-connector-java/5.1.45/mysql-connector-java-5.1.45.jar"/>
    <context id="Mysql" targetRuntime="MyBatis3" defaultModelType="flat">
        <property name="autoDelimitKeywords" value="false"/>
        <!-- 生成的Java文件的编码 -->
        <property name="javaFileEncoding" value="UTF-8"/>
        <!-- 格式化java代码 -->
        <property name="javaFormatter" value="org.mybatis.generator.api.dom.DefaultJavaFormatter"/>
        <!-- 格式化XML代码 -->
        <property name="xmlFormatter" value="org.mybatis.generator.api.dom.DefaultXmlFormatter"/>

        <property name="beginningDelimiter" value="`"/>
        <property name="endingDelimiter" value="`"/>
        
        <plugin type="io.github.jayzhang.mybatis.generator.plugin.MybatisPlusPlugin">
        	<property name="targetProject" value="src/main/java"/>
            <property name="targetPackage" value="com.xxx.dao.service"/>
        	<property name="table1.createBatch" value="true"/>
        	 <property name="table1.optimizedPage" value="true"/>
        </plugin> 
       
        <commentGenerator>
            <property name="suppressAllComments" value="true"/>
            <property name="suppressDate" value="true"/>
        </commentGenerator>
        <jdbcConnection driverClass="com.mysql.jdbc.Driver"
                        connectionURL="jdbc:mysql://localhost:3306/ntrip"
                        userId="username" password="yourpassword"/>
        <javaModelGenerator targetPackage="com.xxx.dao.entity" targetProject="src/main/java">
            <property name="trimStrings" value="true"/>
        </javaModelGenerator>
        <sqlMapGenerator targetPackage="com.xxx.dao.mapper" targetProject="src/main/java"/>
        <javaClientGenerator targetPackage="com.xxx.dao.mapper" targetProject="src/main/java" type="XMLMAPPER"/>
 
 		<table tableName="table1" domainObjectName="Table1">
            <generatedKey column="id" sqlStatement="JDBC" identity="true"/>
        </table> 
     
    </context>
</generatorConfiguration>
```

## Mapper更加简洁
生成的Mapper基于MybatisPlus，生成的Mapper接口和xml比Mybatis Generator默认生成的更加简洁，例如：
```java

public interface Table1Mapper extends BaseMapper<Table1> {
    int insertBatch(List<Table1> records);
}
```
## Entity功能更完善
生成的Java Entity类基于lombok，属性的存取方法更加简洁，同时支持链式属性设置，例如：
```java
Table1 record = new Table1();
     record.setBind(0)
     .setConnectType("SDK")
     .setStatus(0)
     .setPassword(RandomStringUtils.randomAlphabetic(8));
```
另外，Entity对象内嵌Fields枚举类，可以方便获得对象的数据库字段名以及映射关系：
```java
String column = Table1.Fields.bind.getColumn();
System.out.println(column);
```
对于字符串类型的字段，自动生成长度限制等校验注解，例如对于数据库中字段定义为varchar(30)
```sql
`name` varchar(30) ...
```
生成注解：
```java
 @Size(max=30)
 private String name;
```
对于时间类型的字段，自动生成默认的ISO8601时间JSON序列化注解。
在数据库字段的COMMENT中通过JSON定义该字段为时间类型{"t":1}或{"ut":1}或{"ct":1}。
```
`effective_time` bigint(20) DEFAULT NULL COMMENT '{"t":1, "u":1, "f":"gte,lte", "cmt":"生效时间"}',
`expire_time` bigint(20) DEFAULT NULL COMMENT '{"t":1, "u":1, "f":"gte,lte", "cmt":"失效时间"}',
`update_time` bigint(20) NOT NULL COMMENT '{"ut":1, "f":"gte,lte", "cmt":"更新时间"}',
`create_time` bigint(20) NOT NULL COMMENT '{"ct":1, "cmt":"创建时间"}',
...
```
自动在对应字段生成序列化注解：
```
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("table1")
@JsonInclude(Include.NON_NULL) 
public class Table1 implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    @TableId(type=IdType.AUTO)
    private Long id;

    ...
    /**
     *生效时间
     */
    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    private Long effectiveTime;

    /**
     *失效时间
     */
    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    private Long expireTime;
 
    /**
     *更新时间
     */
    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    private Long updateTime;

    /**
     *创建时间
     */
    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    private Long createTime;
    ...
}
```


## 服务生成
该插件除了生成entity和mapper文件之外，还会生成DAO层面的service的CRUD代码。 参数配置targetProject和targetPackage指定生成的DAO层service代码的路径和相应的包。

```
 <property name="targetProject" value="src/main/java"/>
 <property name="targetPackage" value="com.xxx.dao.service"/>
```

生成CRUD方法可配置：

| 方法名 | 描述 | 默认值 |
| --- | --- | --- |
| get | 是否生成get方法，根据主键查询单条记录 | Y |
| create | 是否生成create方法，插入单条记录 | Y |
| createBatch | 是否生成createBatch方法，批量插入 | N |
| update | 是否生成update方法，根据主键更新单条记录 | Y |
| delete | 是否生成delete方法，根据主键删除单条记录 | Y |
| list | 是否生成list方法，支持分页查询 | Y |
| listAll | 是否生成listAll方法，查询所有记录 | Y |
| count | 是否生成count方法，计算记录数量 | Y |
| optimizedPage | 是否对list方法优化翻页查询性能 | N |

默认情况批量插入接口不生成，如果要生成，则需要通过名字为${表名}.${方法名}配置项开启，例如：
```
<property name="table1.createBatch" value="true"/>
```

## 分页查询增强
插件会根据数据库DDL的备注字段，自动生成查询过滤条件字段，同时支持翻页、返回字段选择、排序等参数。例如：
```sql
CREATE TABLE `table1` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '{"f":"in"}',
  `name` varchar(30) NOT NULL DEFAULT '' COMMENT '{"f":"eq,in,like", "a":["@Pattern(regexp=\\"[0-9a-zA-Z_]*\\", message=\\"必须字母数字下划线组合\\")"],"cmt":"差分账号名，全局唯一，不允许更新"}',
  `alias` varchar(30) DEFAULT NULL COMMENT '{"f":"eq,like", "u":1, "cmt":"别名,允许更新"}',
  `prefix` varchar(30) DEFAULT '' COMMENT '{"f":"eq", "cmt":"账号前缀"}',
  `password` varchar(255) NOT NULL DEFAULT '' COMMENT '{"u":1, "f":"eq", "cmt":"账号密码"}',
  `device_id` varchar(100) DEFAULT NULL COMMENT '{"u":1, "f":"eq,in", "cmt":"设备id"}',
  `device_type` varchar(100) DEFAULT NULL COMMENT '{"u":1, "f":"eq,in", "cmt":"设备类型"}',
  `connect_type` varchar(20) NOT NULL DEFAULT '' COMMENT '{"u":1, "f":"eq", "cmt":"接入方式 NTRIP，SDK"}',
  `bind` int(4) NOT NULL DEFAULT '0' COMMENT '{"u":1, "f":"eq", "cmt":"是否绑定sdk，1：已绑定，0：未绑定 NTRIP，SDK"}',
  `status` int(4) NOT NULL DEFAULT '0' COMMENT '{"u":1, "f":"eq", "cmt":"1->禁用， 0->启用"}',
  `geometry` varchar(2048) DEFAULT NULL COMMENT '{"u":1, "cmt":"区域限制范围，WKT格式"}',
  `effective_time` bigint(20) DEFAULT NULL COMMENT '{"t":1, "u":1, "f":"gte,lte", "cmt":"生效时间"}',
  `expire_time` bigint(20) DEFAULT NULL COMMENT '{"t":1, "u":1, "f":"gte,lte", "cmt":"失效时间"}',
  `remark` varchar(200) DEFAULT NULL COMMENT '{"u":1, "cmt":"备注"}',
  `update_time` bigint(20) NOT NULL COMMENT '{"ut":1, "f":"gte,lte", "cmt":"更新时间"}',
  `create_time` bigint(20) NOT NULL COMMENT '{"ct":1, "cmt":"创建时间"}',
  `created_by` bigint(20) DEFAULT NULL COMMENT '{"u":1, "f":"eq,in", "cmt":"创建者"}',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_index` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8 COMMENT='差分账号';
```

JSON描述字段说明：
- f: 生成Service代码时，字段是否可被查询，f过滤操作符可以是eq,like,gt,lt,in,gte,lte中的任意组合，例如 "f":"eq,in"。
- ut:更新时间，自动更新，必须为BIGINT，默认会加上时间的JSON序列化/反序列化注解
- ct:创建时间，自动更新，必须为BIGINT，默认会加上时间的JSON序列化/反序列化注解
- t:表示该字段是时间，对于时间字段，默认会加上时间的JSON序列化/反序列化注解
- cmt: 字段备注描述

插件会生成查询对象，例table1表，除了生成Table1实体类之外，还会生成查询对象类Table1List，该类定义了一些分页查询的参数。 注意：查询过滤参数中eq过滤算法直接生成对应的字段名，范围查询过滤条件会在原始字段名后添加Gte、Lte、Gt、Lt等名字。列表查询请求参数包含4类：（注意：所有分页查询列表的接口都遵循此规范） 
- fields:指定返回的字段列表，参见数据结构定义，如果不指定则返回所有字段
- 过滤条件，字段取名参见参数规范：

| 过滤算子 | 过滤key |
| :--- | :--- |
| in,eq | 原始字段名，例如id=1,2,3,4,5 |
| like | 原始字段名+Like，例如nameLike=name% |
| gte | 原始字段名+Gte，例如createTimeGte=xxxx |
| lte | 原始字段名+Lte |
| gt | 原始字段名+Gt |
| lt | 原始字段名+Lt |

- 分页参数：skip,limit
- 排序参数：orderByFields

```
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Table1List implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonDeserialize(using=String2LongListDeserializer.class)
    private List<Long> id;

    @JsonDeserialize(using=String2StringListDeserializer.class)
    private List<String> name;

    private String nameLike;

    private String alias;

    private String aliasLike;

    private String prefix;

    private String password;

    @JsonDeserialize(using=String2StringListDeserializer.class)
    private List<String> deviceId;

    @JsonDeserialize(using=String2StringListDeserializer.class)
    private List<String> deviceType;

    private String connectType;

    private Integer bind;

    private Integer status;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long effectiveTimeGte;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long effectiveTimeLte;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long expireTimeGte;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long expireTimeLte;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long updateTimeGte;

    @JsonDeserialize(using=ISO8601TimeDeserializer.class)
    @JsonSerialize(using=ISO8601TimeSerializer.class)
    @MyDateTimeFormat
    private Long updateTimeLte;

    @JsonDeserialize(using=String2LongListDeserializer.class)
    private List<Long> createdBy;

    @Min(0)
    private Integer skip = 0;

    @Min(0)
    private Integer limit = 20;

    private boolean count = true;

    @JsonDeserialize(using=String2StringListDeserializer.class)
    private List<String> fields;

    private boolean distinct;

    private List<OrderByField> orderByFields;
}
```

当开启optimizedPage选项时，会生成额外的Mapper接口page：
```
    List<Table1> page(Table1List list);
```
该方法的SQL实现是翻页查询性能进行了优化，原理是采用left join，保证了当翻页数量比较大的时候避免查询性能的降低。

例如原始的翻页查询语句：
```
    SELECT column1,column2,... FROM table1 WHERE ${condition} ORDER BY ${orderbyfields} LIMIT ${skip},${limit}
```
经过优化以后的查询语句：
```
    SELECT column1,column2,... 
    FROM 
    ( SELECT ${primary key} FROM table1 WHERE ${condition} ORDER BY ${orderbyfields}) AS T1 LEFT JOIN  table1 AS T2 ON T1.${primarykey} = T2.${primarykey}
```

# 一键生成
```
File configFile = new File("demo.xml");
Generator.generate(configFile);
```
# 实践建议
   
1.如果是dubbo服务，生成的entity模型可以直接作为api包的组成部分，mapper和service作为服务端的实现代码；如果是web服务，entity模型和entityList对象可直接作为DTO对象；
    
2.数据库的时间字段建议统一用BIGINT类型，并且用标记成时间字段（t,ut,ct)，更新时间和创建时间由生成的service来传入，建议不要用TIMESTAMP。 

3.在开发过程中如果有数据库DDL变更，有可能需要重新生成entity、mapper、service包的代码，并替换老的，因此建议不要手动更改生成的代码

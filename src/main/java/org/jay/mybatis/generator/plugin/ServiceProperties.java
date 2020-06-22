package org.jay.mybatis.generator.plugin;

public class ServiceProperties {
	boolean get = true;
	boolean create = true;
	boolean update = true;
	boolean delete = true;
	boolean list = true;
	boolean listAll = true;
	boolean count = true;
	boolean createBatch = false;
	boolean optimizedPage = false;//是否优化翻页查询性能
}

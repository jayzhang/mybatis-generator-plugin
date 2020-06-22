package org.jay.mybatis.generator.plugin.utils;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class OrderByField implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String field;
	private boolean asc;
	
	public OrderByField(String field, boolean asc)
	{
		this.field = field;
		this.asc = asc;
	}
	
	public OrderByField(String field)
	{
		this.field = field;
		this.asc = true;
	}
}

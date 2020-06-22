package io.github.jayzhang.mybatis.generator.plugin.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Splitter;

public class String2ShortListDeserializer extends JsonDeserializer<List<Short>> {

	@Override
	public List<Short> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException 
	{
		List<Short> result = new ArrayList<>();
		JsonToken jt = p.currentToken();
        if (jt == JsonToken.VALUE_STRING) 
        {
        	String txt = p.getValueAsString();
        	List<String> list = Splitter.on(",").splitToList(txt);
        	 
        	for(String i: list)
        	{
        		result.add(Short.parseShort(i));
        	}
        	return result;
        }
        return null;
	}
}

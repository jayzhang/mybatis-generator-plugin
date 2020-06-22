package io.github.jayzhang.mybatis.generator.plugin.utils;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class DateISO8601TimeDeserializer extends JsonDeserializer<Date> {

	@Override
	public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException 
	{
		JsonToken jt = p.currentToken();
        if (jt == JsonToken.VALUE_STRING) 
        {
        	return new Date(Instant.parse(p.getValueAsString()).toEpochMilli());
        }
        return null;
	}
}

package io.github.jayzhang.mybatis.generator.plugin.utils;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ISO8601TimeSerializer extends JsonSerializer<Long> {

	@Override
	public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		if(value != null)
		{
			gen.writeString(Instant.ofEpochMilli(value).toString());
		}
	}

}

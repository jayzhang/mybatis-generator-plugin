package io.github.jayzhang.mybatis.generator.plugin.utils;

import java.io.IOException;
import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DoubleSerializer extends JsonSerializer<Double> {
 
    private DecimalFormat df = new DecimalFormat("##.00");  

	@Override
	public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if(value != null) {
            gen.writeString(df.format(value));  
        }
	}
} 


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.jay.mybatis.generator.plugin.Generator;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.XMLParserException;

public class GeneratorTest {

	public static void main(String[] args) throws XMLParserException, IOException, InvalidConfigurationException, SQLException, InterruptedException {
		File configFile = new File("src/test/resources/hermes.xml");
		Generator.generate(configFile);
	}
}

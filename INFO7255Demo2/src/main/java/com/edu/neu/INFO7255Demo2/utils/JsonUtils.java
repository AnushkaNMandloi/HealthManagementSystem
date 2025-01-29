 package com.edu.neu.INFO7255Demo2.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.edu.neu.INFO7255Demo2.model.ApiError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JsonUtils {

	private static JsonSchema jsonSchema = null;
	private final static JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

	/**
	 * Added to validate json against the given schema
	 * 
	 * @param inputJson
	 * @return
	 */
	public static JsonNode validateSchema(String inputJson) {
		JsonNode jsonOutput = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			// Read the json schema
			File initialFile = new File("./medicalPlan.schema.json");

			try {
				InputStream schema = new FileInputStream(initialFile);
				JsonNode schemaNode = mapper.readTree(schema);
				// Add the input.json as the schema to validate all inputs against
				jsonSchema = factory.getJsonSchema(schemaNode);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

			// Read the input json and assign it to a jsonNode
			// Validate the inputjson against the schema
			if (inputJson != null && !inputJson.isBlank()) {
				jsonOutput = JsonLoader.fromString(inputJson);
				ProcessingReport processingReport = jsonSchema.validate(jsonOutput);

				ProcessingMessage message = null;
				if (processingReport.isSuccess())
					return jsonOutput;
				else {
					Iterator itr = processingReport.iterator();
					String messages = "";
					while (itr.hasNext()) {
						message = (ProcessingMessage) itr.next();
						messages += (message.asJson().toString());
					}
					ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(), messages,
							new Timestamp(System.currentTimeMillis()));
					String json = mapper.writeValueAsString(apiError);
					jsonOutput = mapper.readTree(json);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonOutput;
	}

}

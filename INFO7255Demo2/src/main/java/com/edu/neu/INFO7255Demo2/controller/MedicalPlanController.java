package com.edu.neu.INFO7255Demo2.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.edu.neu.INFO7255Demo2.model.ApiError;
import com.edu.neu.INFO7255Demo2.model.AuthRequest;
import com.edu.neu.INFO7255Demo2.model.AuthResponse;
import com.edu.neu.INFO7255Demo2.service.Helper;
import com.edu.neu.INFO7255Demo2.service.JwtUtilsService;
import com.edu.neu.INFO7255Demo2.service.MyUserDetailsService;
import com.edu.neu.INFO7255Demo2.service.RedisService;
import com.edu.neu.INFO7255Demo2.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

@RestController
public class MedicalPlanController {

	@Autowired
	private RedisService redisService;

	@Autowired
	private JwtUtilsService jwtUtilsService;

	@Autowired
	private MyUserDetailsService userDetailsService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private Helper helper;

	@PostMapping("/test")
	public ResponseEntity<String> testMethod() {

		return ResponseEntity.ok().body("jsonNode");
	}

	@PostMapping("/authenticate")
	public ResponseEntity<?> generateAuthenticationToken(@RequestBody AuthRequest authenticationRequest)
			throws Exception {
		System.out.println("In");
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUsername(), authenticationRequest.getPassword())
					);
		} catch (BadCredentialsException e) {
			System.out.println("Incorrect Username or password");
			throw new Exception("Incorrect Username and password",e);
		}

		final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());

		final String token = jwtUtilsService.generateToken(userDetails);

		return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(token));
	}

	@GetMapping("/authenticate")
	public ResponseEntity<?> generateAuthenticationToken(HttpServletRequest request, @RequestHeader HttpHeaders headers)
			throws Exception {

		final String authorizationHeader = request.getHeader("Authorization");
		String token = null;
		String username = null;
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			token = authorizationHeader.substring(7);
			username = jwtUtilsService.getUsernameFromToken(token);
		}
		final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if (jwtUtilsService.validateToken(token, userDetails))
			return ResponseEntity.status(HttpStatus.OK).body("Successful");

		return ResponseEntity.status(HttpStatus.OK).body("Error");
	}

	@PostMapping("/plan")
	public ResponseEntity<?> addPlan(@RequestBody String requestBody) {
		
		JsonNode rootNode = JsonUtils.validateSchema(requestBody);
		System.out.println("Root node"+ rootNode);
		if (rootNode.get("objectId") == null)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(rootNode);
		//String objectId = rootNode.get("objectId").textValue();
		String checkplanId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();

		String existingPlan = redisService.getValue(checkplanId);
		System.out.println("Existing plan"+ checkplanId);
		if (existingPlan != null && !existingPlan.isBlank()) {
			ApiError apiError = new ApiError(HttpStatus.CONFLICT.toString(),
					"Plan with objectId: " + rootNode.get("objectId").textValue() + " already exists", new Timestamp(System.currentTimeMillis()));
			return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
		}

		helper.saveKeyValuePairs(rootNode);
		String planId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();
		System.out.println("planId"+ planId);
		redisService.postValue(planId, rootNode.toString());
        String newPlan = redisService.getValue(planId);
        System.out.println("newPlan"+ newPlan);
        System.out.println("Post  " + newPlan);
		String etag = MD5(newPlan);
		System.out.println("POST " + etag);
		System.out.println(newPlan);
		ApiError apiError = new ApiError(HttpStatus.CREATED.toString(),
				"Plan with objectId: " + rootNode.get("objectId").textValue() + "created ", new Timestamp(System.currentTimeMillis()));
		return ResponseEntity.status(HttpStatus.CREATED).eTag(etag).body(apiError);
	}

	@GetMapping("/plan/{id}")
	public ResponseEntity<Object> getPlanById(@PathVariable String id, HttpServletRequest request,
			@RequestHeader HttpHeaders headers) {
		String internalId = "plan" + "-" + id;
		String existingPlan = redisService.getValue(internalId);
		if (existingPlan == null || existingPlan.isBlank()) {
			ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(),
					"Plan with objectId: " + id + " does not exist", new Timestamp(System.currentTimeMillis()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
		}
		String value = redisService.getValue(internalId);
		JsonNode jsonNode = JsonUtils.validateSchema(value);
		String etag = MD5(existingPlan);
		String actualEtag = headers.getFirst("If-None-Match");
		if (etag != null && etag.equals(actualEtag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(actualEtag).build();
		}
		System.out.println("GET WAla " + etag);
		System.out.println(existingPlan);
		return ResponseEntity.ok().eTag(etag).body(jsonNode);
	}

	@PatchMapping("/plan/{id}")
	public ResponseEntity<?> patchPlan(@RequestBody String input, @PathVariable String id,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {

		 if (ifMatch == null) {
		        // If the If-Match header is missing
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"eTag is not provided in request \" }");
		    }

		String internalID = "plan" + "-" + id;
		
		System.out.println("Internal ID is "+internalID);
		String value = redisService.getValue(internalID);
		System.out.println("Value is "+value);

		if (value == null) {
			return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
		}

		
		System.out.println("Before replace"+ifMatch);
		ifMatch = ifMatch.replace("\"", "");
		System.out.println("After replace"+ifMatch);
		String checkEtag = MD5(value);
		//System.out.println("ETAG is " + existEtag);
		
		//String  checkEtag = MD5(value);
		System.out.println("PATCH is " + checkEtag);
		if (ifMatch == null || ifMatch == "" || !ifMatch.equals(checkEtag)) {
			return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Plan with objectId: " + id + " doesnot match with the eTag");
		}
		else {
			System.out.println(ifMatch);
			ifMatch = ifMatch.replace("\"", "");
			System.out.println(ifMatch);
			System.out.println(input);
		}
		
		try {
			// Get the old node from redis using the object Id
			JsonNode oldNode = JsonUtils.validateSchema(value);
			// redisService.populateNestedData(oldNode, null);
			value = oldNode.toString();
			System.out.println("old node is "+value);
			// Construct the new node from the input body
			String inputData = input;
			JsonNode newNode = JsonUtils.validateSchema(inputData);
			System.out.println("New node  is "+ newNode);
			ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
			System.out.println("PlanServices New is "+planServicesNew);
			Set<JsonNode> planServicesSet = new HashSet<>();
			Set<String> objectIds = new HashSet<String>();
			ArrayNode planServicesold = (ArrayNode) oldNode.get("linkedPlanServices");
			System.out.println("PlanServices old is "+planServicesold);
			
			planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));
			
			System.out.println("PlanServices New  after add is "+planServicesNew);
			
			for (JsonNode node : planServicesNew) {
				System.out.println("Initail node "+node);
				System.out.println(" node feilds "+node.fields());
				Iterator<Map.Entry<String, JsonNode>> sitr = node.fields();
				//System.out.println("PlanServices New  after add is "+planServicesNew);
			    System.out.println("node feilds  "+sitr);
				while (sitr.hasNext()) {
					
					Map.Entry<String, JsonNode> val = sitr.next();
					System.out.println("value of key  "+val);
					System.out.println("value of get key  "+val.getKey());
					if (val.getKey().equals("objectId")) {
						if (!objectIds.contains(val.getValue().toString())) {
							System.out.println("value of object id to add in objectids  key  "+val.getValue());
							System.out.println("value of object id to add in objectids  node  "+val.getValue());
							planServicesSet.add(node);
							objectIds.add(val.getValue().toString());
						}
					}
				}
			}
			planServicesNew.removeAll();
			if (!planServicesSet.isEmpty())
				planServicesSet.forEach(s -> {
					planServicesNew.add(s);
				});
			redisService.postValue(internalID, newNode.toString());
			helper.deleteKeyValuePairs(oldNode);
			helper.saveKeyValuePairs(newNode);
			
			 System.out.println("Patch  " + newNode);
				String newEtag = MD5(newNode.toString());
				System.out.println("Patch " + newEtag);
				System.out.println(newNode);
			
			return ResponseEntity.ok().eTag(newEtag).body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
		} catch (Exception e) {
			return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
		}

		//return ResponseEntity.ok().eTag(newEtag).body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
	}


	@DeleteMapping("/plan/{id}")
	public ResponseEntity<?> deletePlan(@PathVariable String id,@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		 if (ifMatch == null) {
		        // If the If-Match header is missing
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"eTag is not provided in request \" }");
		    }
		String internalId = "plan" + "-" + id;
		String existingPlan = redisService.getValue(internalId);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(),
				"Plan with objectId: " + id + " does not exist", new Timestamp(System.currentTimeMillis()));
		if (existingPlan == null || existingPlan.isEmpty()) {
			return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
		}
		
		String internalID = "plan" + "-" + id;
		String value = redisService.getValue(internalID);
		System.out.println(ifMatch);
		ifMatch = ifMatch.replace("\"", "");
		System.out.println(ifMatch);
		System.out.println("value is " + value);
		String newEtag = MD5(value);
		System.out.println("delete new ETAG is " + newEtag);
		if (ifMatch == null || ifMatch == "" || !ifMatch.equals(newEtag)) {
			return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Plan with objectId: " + id + " doesnot match with the eTag");
		}
		
		boolean success = redisService.deleteValue(internalId);
		JsonNode rootNode = JsonUtils.validateSchema(existingPlan);
		helper.deleteKeyValuePairs(rootNode);
		if (success)
			return ResponseEntity.status(HttpStatus.NO_CONTENT).body(" {\"message\": \"Plan has been Deleted\" }");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	}

	@PutMapping("/plan/{id}")
	public ResponseEntity<?> updatePlan(@RequestBody String input, @PathVariable String id, @RequestHeader(value = "If-Match", required = false) String ifMatch) {

		String internalID = "plan" + "-" + id;
		String value = redisService.getValue(internalID);

		if (value == null) {
			return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
		}
		
		 if (ifMatch == null) {
		        // If the If-Match header is missing
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"eTag is not provided in request \" }");
		    }

		System.out.println("Before replace"+ifMatch);
		ifMatch = ifMatch.replace("\"", "");
		System.out.println("After replace"+ifMatch);
		String existEtag = MD5(value);
		System.out.println("ETAG is " + existEtag);
		if (ifMatch == null || ifMatch == "" || !ifMatch.equals(existEtag)) {
			return new ResponseEntity<String>("{\"message\": \"Plan has been updated by other user\" }", HttpStatus.PRECONDITION_FAILED);
		}

		try {
			// Get the old node from redis using the object Id
			JsonNode oldNode = JsonUtils.validateSchema(value);
			// redisService.populateNestedData(oldNode, null);
			value = oldNode.toString();
			// Construct the new node from the input body
			String inputData = input;
			JsonNode newNode = JsonUtils.validateSchema(inputData);

			helper.deleteKeyValuePairs(oldNode);
			helper.saveKeyValuePairs(newNode);
			
			
	        System.out.println("Put  " + newNode);
			String newEtag = MD5(newNode.toString());
			System.out.println("Put " + newEtag);
			System.out.println(newNode);
			redisService.deleteValue(internalID);
			redisService.postValue(internalID, newNode.toString());
			System.out.println("PUT " + newEtag);
			return ResponseEntity.ok().eTag(newEtag).body(" {\"message\": \"Updated data with key: " + internalID + "\" }");

		} catch (Exception e) {
			return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
		}
		
		//System.out.println("PUT " + newEtag);
		//return ResponseEntity.ok().eTag(newEtag).body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
	}

	public String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

}

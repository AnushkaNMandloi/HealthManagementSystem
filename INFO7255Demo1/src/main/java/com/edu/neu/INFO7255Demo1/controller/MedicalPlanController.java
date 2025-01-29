package com.edu.neu.INFO7255Demo1.controller;

import com.edu.neu.INFO7255Demo1.service.PlanService;
import com.edu.neu.INFO7255Demo1.utils.JsonValidator;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MedicalPlanController {

    @Autowired
    JsonValidator validator;

    @Autowired
    PlanService planservice ;

    Map<String, Object> m = new HashMap<String, Object>();

    @PostMapping(path = "/plan", produces = "application/json")
    public ResponseEntity<Object> createPlan(@Valid @RequestBody(required = false) String medicalPlan, @RequestHeader HttpHeaders headers) throws IOException {
        m.clear();
        if (medicalPlan == null || medicalPlan.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("Error", "Body is Empty! Kindly provide the JSON").toString());
        }

        JSONObject json = new JSONObject(medicalPlan);
        try{
            validator.validateJson(json);
        }catch(ValidationException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("This is Schema Violation Error",ex.getErrorMessage()).toString());

        }

        String key = json.get("objectType").toString() + "_" + json.get("objectId").toString();
        if(planservice.checkIfKeyExists(key)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new JSONObject().put("Message", "ObjectId already exist").toString());
        }

        String newEtag = planservice.savePlanToRedis(json, key);

        return ResponseEntity.status(HttpStatus.CREATED).eTag(newEtag).body(" {\"message\": \"Created new record with objectId: " + json.get("objectId") + "\" }");
    }

    @GetMapping(path = "/{type}/{objectId}", produces = "application/json")
    public ResponseEntity<Object> getPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId,@PathVariable String type) {

        if (!planservice.checkIfKeyExists(type + "_" + objectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "Object with Id "+ objectId + " does not exist").toString());
        }

        String actualEtag = null;
        if (type.equals("plan")) {
            actualEtag = planservice.getEtag(type + "_" + objectId, "eTag");
            String eTag = headers.getFirst("If-None-Match");
            if (eTag != null && eTag.equals(actualEtag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(actualEtag).build();
            }
        }

        String key = type + "_" + objectId;
        Map<String, Object> plan = planservice.getPlan(key);

        if (type.equals("plan")) {
            return ResponseEntity.ok().eTag(actualEtag).body(new JSONObject(plan).toString());
        }
        return ResponseEntity.ok().body(new JSONObject(plan).toString());
    }

    @DeleteMapping(path = "/plan/{objectId}", produces = "application/json")
    public ResponseEntity<Object> deletePlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId){

        if (!planservice.checkIfKeyExists("plan"+ "_" + objectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "Object with Id " + objectId + " does not exist").toString());
        }
        planservice.deletePlan("plan" + "_" + objectId);

        // return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new JSONObject().put("Message", "Object with Id " + objectId + " is deleted").toString());
    }
}



package com.edu.neu.INFO7255Demo2.service;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class Helper {

	@Autowired
	private RedisService redisService;

	public void saveKeyValuePairs(JsonNode rootNode) {
		JsonNode planCostSharesNode = rootNode.get("planCostShares");
		String planCostSharesId = planCostSharesNode.get("objectType").textValue() + "-" + planCostSharesNode.get("objectId").textValue();
		redisService.postValue(planCostSharesId, planCostSharesNode.toString());
		
		ArrayNode planServices = (ArrayNode) rootNode.get("linkedPlanServices");
		for (JsonNode node : planServices) {
			Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
			if (node != null) {
				redisService.postValue(node.get("objectType").textValue() + "-" + node.get("objectId").textValue(),
						node.toString());
			}
				

			while (itr.hasNext()) {
				Map.Entry<String, JsonNode> val = itr.next();
				System.out.println(val.getKey());
				System.out.println(val.getValue());
				if (val.getKey().equals("linkedService")) {
					JsonNode linkedServiceNode = (JsonNode) val.getValue();
					redisService.postValue(linkedServiceNode.get("objectType").textValue() + "-"
							+ linkedServiceNode.get("objectId").textValue(), linkedServiceNode.toString());
				}
				if (val.getKey().equals("planserviceCostShares")) {
					JsonNode planserviceCostSharesNode = (JsonNode) val.getValue();
					redisService.postValue(
							planserviceCostSharesNode.get("objectType").textValue() + "-"
									+ planserviceCostSharesNode.get("objectId").textValue(),
							planserviceCostSharesNode.toString());
				}
			}
		}
	}

	public void deleteKeyValuePairs(JsonNode rootNode) {
		JsonNode planCostSharesNode = rootNode.get("planCostShares");
		String planCostSharesId = planCostSharesNode.get("objectType").textValue() + "-" + planCostSharesNode.get("objectId").textValue();
		redisService.deleteValue(planCostSharesId);
		ArrayNode planServices = (ArrayNode) rootNode.get("linkedPlanServices");
		for (JsonNode node : planServices) {
			Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
			if (node != null)
				redisService.deleteValue(node.get("objectType").textValue() + "-" + node.get("objectId").textValue());

			while (itr.hasNext()) {
				Map.Entry<String, JsonNode> val = itr.next();
				System.out.println(val.getKey());
				System.out.println(val.getValue());
				if (val.getKey().equals("linkedService")) {
					JsonNode linkedServiceNode = (JsonNode) val.getValue();
					redisService.deleteValue(linkedServiceNode.get("objectType").textValue() + "-"
							+ linkedServiceNode.get("objectId").textValue());

				}
				if (val.getKey().equals("planserviceCostShares")) {
					JsonNode planserviceCostSharesNode = (JsonNode) val.getValue();
					redisService.deleteValue(planserviceCostSharesNode.get("objectType").textValue() + "-"
							+ planserviceCostSharesNode.get("objectId").textValue());

				}
			}
		}
	}

}

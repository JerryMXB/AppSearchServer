import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

public class ElasticSearchClient {
	private static RestHighLevelClient client = null;
	private static String host = "search-app-5nms4szu2o7uqd7eq2q7vmov6u.us-east-1.es.amazonaws.com";
	
	private static RestHighLevelClient getClient() {
		if (client == null) {
			client = new RestHighLevelClient(RestClient.builder(
					new HttpHost(host, 80, "http")));
		}
		return client;
	} 
	
	public static JsonObject search(String term) throws IOException {
		client = getClient();
		JsonObject queryResult = queryProcessing(term, 5);   
		JsonObject rankedResult = rankResult(queryResult);
		return rankedResult;
	}
	
	private static JsonObject queryProcessing(String term, int k) throws IOException {
		String fieldName = "functions";
		JsonObject resultJson, mergedJson = new JsonObject();
		mergedJson.put("result", new JsonArray());
		
		// exact match
		resultJson = doQuery(QueryBuilders.fuzzyQuery(fieldName, term), 2.0);
		if(resultJson != null) {
			mergedJson = mergeJsonResult(mergedJson, resultJson);
		}
		
		// and query
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		for (String subTerm : term.split(" ")) {
			queryBuilder.must(QueryBuilders.fuzzyQuery(fieldName, subTerm));
		}
		resultJson = doQuery(queryBuilder, 1.0);
		if(resultJson != null) {
			mergedJson = mergeJsonResult(mergedJson, resultJson);
		}
		
		// common terms query
		resultJson = doQuery(QueryBuilders.commonTermsQuery(fieldName, term), 0.8);
		if(resultJson != null) {
			mergedJson = mergeJsonResult(mergedJson, resultJson);
		}
		
		// or query
		resultJson = doQuery(QueryBuilders.termsQuery(fieldName, term.split(" ")), 0.1);
		if(resultJson != null) {
			mergedJson = mergeJsonResult(mergedJson, resultJson);
		}

		JsonObject cascadedJson = doCascading(mergedJson, k);
		return cascadedJson;
	}
	
	private static JsonObject rankResult(JsonObject result) {
		JsonArray jsonArray = result.getJsonArray("result");
		List<JsonObject> jsonList = new ArrayList<>();
		for (int i = 0; i < jsonArray.size(); i++) {
			jsonList.add(jsonArray.getJsonObject(i));
		}
		for (int i = 0; i < jsonList.size(); i++) {
			double apprank = 1.0;
			// TO DO: fetch apprank and put it into variable apprank
			//
			jsonList.get(i).put("apprank", apprank);
		}
		Collections.sort(jsonList, new Comparator<JsonObject> () {
			@Override
			public int compare(JsonObject o1, JsonObject o2) {
				return o2.getDouble("apprank").compareTo(o1.getDouble("apprank"));
			}
		});
		return new JsonObject().put("result", jsonList);
	}
	
	private static JsonObject doCascading(JsonObject jsonObj, int k) {
		if (jsonObj == null || k <= 0) {
			return null;
		}
		JsonArray jsonArray = jsonObj.getJsonArray("result");
		if (jsonArray == null) {
			return null;
		}
		List<JsonObject> jsonList = new ArrayList<>();
		for (int i = 0; i < jsonArray.size(); i++) {
			jsonList.add(jsonArray.getJsonObject(i));
		}
		Collections.sort(jsonList, new Comparator<JsonObject>() {
			@Override
			public int compare(JsonObject o1, JsonObject o2) {
				return o2.getDouble("score").compareTo(o1.getDouble("score"));
			}
		});
		JsonArray resultArray = new JsonArray();
		for (int i = 0; i < k; i++) {
			resultArray.add(jsonList.get(i));
		}
		return new JsonObject().put("result", resultArray);
	}
	
	private static JsonObject doQuery(QueryBuilder qb, double weight) throws IOException {
		SearchSourceBuilder searchSourceBuilder;
		JsonObject resultJson;
		JsonArray resultList;
		
		searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(qb);
		resultJson = getQueryResult(searchSourceBuilder, weight);
		resultList = (JsonArray) resultJson.getValue("result") ;
		if(resultList.size() > 0) {
			return resultJson;
		} else {
			return null;
		}
	}
	
	private static JsonObject getQueryResult(SearchSourceBuilder searchSourceBuilder) throws IOException {
		return getQueryResult(searchSourceBuilder, 1.0);
	}
	
	private static JsonObject getQueryResult(SearchSourceBuilder searchSourceBuilder, double weight) throws IOException {
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = client.search(searchRequest);
		SearchHits searchHits = searchResponse.getHits();
		JsonObject resultJson = new JsonObject();
		List<JsonObject> hitJsonList = new ArrayList<>();
		for (SearchHit hit : searchHits.getHits()) {
			JsonObject hitAsJson = new JsonObject(hit.getSourceAsMap());
			hitAsJson.put("id", hit.getId());
			hitAsJson.put("score", hit.getScore() * weight);
			hitJsonList.add(hitAsJson);
		}
		resultJson.put("result", hitJsonList);
		return resultJson;
	}
	
	private static JsonObject mergeJsonResult(JsonObject a, JsonObject b) {
		if (a == null || b == null) {
			return null;
		}
		JsonArray result_array = new JsonArray();
		JsonArray a_array = a.getJsonArray("result");
		JsonArray b_array = b.getJsonArray("result");
		if (a_array == null || b_array == null) {
			return null;
		}
		for (int i = 0; i < a_array.size(); i++) {
			JsonObject a_jsonObj = a_array.getJsonObject(i);
			for (int j = 0; j < b_array.size(); j++) {
				JsonObject b_jsonObj = b_array.getJsonObject(j);
				if (b_jsonObj.getString("id").equals(a_jsonObj.getString("id"))) {
					a_jsonObj.put("score", a_jsonObj.getDouble("score") + b_jsonObj.getDouble("score"));
					b_array.remove(j);
				}
			}
			result_array.add(a_jsonObj);
		}
		for (int i = 0; i < b_array.size(); i++) {
			result_array.add(b_array.getJsonObject(i));
		}
		JsonObject result = new JsonObject().put("result", result_array);
		return result;
	}
}

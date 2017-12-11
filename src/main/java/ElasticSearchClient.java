import java.io.IOException;
import java.util.ArrayList;
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
		JsonObject queryResult = runQuery(term);   
		JsonObject rankedResult = rankResult(queryResult);
		return rankedResult;
	}
	
	private static JsonObject runQuery(String term) throws IOException {
		// TO DO: run multiple types of queries to find all possible hits in the database
		SearchSourceBuilder searchSourceBuilder;
		JsonObject resultJson;
		String fieldName = "functions";
		
		// This is an example of a common terms query
		searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.commonTermsQuery(fieldName, term)
		//		.lowFreqOperator(Operator.AND)
		);
		resultJson = getQueryResult(searchSourceBuilder, 0.8);
		
		// This is an example of a terms query (or query)
		searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.termsQuery(fieldName, term.split(" ")));
		resultJson = getQueryResult(searchSourceBuilder, 0.1);
		
		// This is an example of an and query
		searchSourceBuilder = new SearchSourceBuilder();
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		for (String subTerm : term.split(" ")) {
			queryBuilder.must(QueryBuilders.termQuery(fieldName, subTerm));
		}
		searchSourceBuilder.query(queryBuilder);
		resultJson = getQueryResult(searchSourceBuilder, 1.0);
		
		// TO DO: merge the previous results in some way
		
		// Tips: switch to fuzzy search for single term if possible

		return resultJson;
	}
	
	private static JsonObject rankResult(JsonObject result) {
		JsonObject sortedResult = result;
		// TO DO: do ranking and sort the list
		return sortedResult;
	}
	
	private static JsonObject getQueryResult(SearchSourceBuilder searchSourceBuilder) throws IOException {
		return getQueryResult(searchSourceBuilder, 0.0);
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
			hitAsJson.put("score", hit.getScore());
			hitJsonList.add(hitAsJson);
		}
		// For query processing
		// Put a weight field in the returning json object 
		if (weight > 0) {
			resultJson.put("weight", weight);
		}
		resultJson.put("result", hitJsonList);
		return resultJson;
	}
}

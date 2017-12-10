import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

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
	
	public static List<String> search(String term) throws IOException {
		client = getClient();
		SearchSourceBuilder searchSourceBuilder = parseQueryTerm(term);   
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = client.search(searchRequest);
		SearchHits searchHits = searchResponse.getHits();
		List<String> result = new ArrayList<>();
		for (SearchHit hit : searchHits.getHits()) {
			result.add(hit.getSourceAsString());
		}
		result = rankResult(result);
		return result;
	}
	
	private static SearchSourceBuilder parseQueryTerm(String term) {
		// TO DO: parse query
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.termQuery("functions", term));
		return searchSourceBuilder;
	}
	
	private static List<String> rankResult(List<String> result) {
		List<String> sortedResult = result;
		// TO DO: do ranking and sort the list
		return sortedResult;
	}
}

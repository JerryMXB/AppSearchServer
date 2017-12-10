import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle {
    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/search/:key").handler(routingContext -> {
        	String searchKey = routingContext.request().getParam("key");
        	HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.putHeader("content-type", "text/plain");
            response.putHeader("Access-Control-Allow-Origin", "*");
            List<String> searchResponse = new ArrayList<>();
			try {
				searchResponse = ElasticSearchClient.search(searchKey);
			} catch (IOException e) {
				e.printStackTrace();
			}
			StringBuilder strBuilder = new StringBuilder();
			for(String result: searchResponse) {
				strBuilder.append(result);
			}
            response.end(strBuilder.toString());
        });
        server.requestHandler(router::accept).listen(8080);
    }
}
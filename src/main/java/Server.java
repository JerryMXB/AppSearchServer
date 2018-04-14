import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle {
    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/search/:key").handler(routingContext -> {
        	String searchKey = routingContext.request().getParam("key");
        	HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/json");
            response.putHeader("Access-Control-Allow-Origin", "*");
            JsonObject searchResponse = new JsonObject();
			try {
				searchResponse = ElasticSearchClient.search(searchKey);
			} catch (IOException e) {
				e.printStackTrace();
			}
            response.end(searchResponse.toString());
        });
        server.requestHandler(router::accept).listen(9090);
    }
}
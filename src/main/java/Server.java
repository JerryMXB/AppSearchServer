import java.io.IOException;

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
            String searchResponse = "";
			try {
				searchResponse = ElasticSearchClient.search(searchKey);
			} catch (IOException e) {
				e.printStackTrace();
				searchResponse = "500 Error";
			}
            response.end(searchResponse);
        });
        server.requestHandler(router::accept).listen(8080);
    }
}
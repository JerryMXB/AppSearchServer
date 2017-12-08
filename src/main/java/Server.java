import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;


public class Server extends AbstractVerticle {
    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {
            // This handler gets called for each request that arrives on the server
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "text/plain");
            request.bodyHandler(body -> System.out.println(body));
            response.putHeader("content-type", "text/plain");
            response.putHeader("Access-Control-Allow-Origin", "*");
            response.end("Hello World");
        });
        server.listen(8080);
    }
}
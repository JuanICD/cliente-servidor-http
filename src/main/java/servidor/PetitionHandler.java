package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.logging.Logger;

public class PetitionHandler implements HttpHandler {
    private Logger LOGGER = Logger.getLogger(PetitionHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        //Obtenemos el tipo de la peticion
        String method = exchange.getRequestMethod().toUpperCase();

        switch (method) {
            case "POST" -> handlePOST(exchange);
            case "PUT" -> handlePUT(exchange);
            default -> exchange.sendResponseHeaders(405, 0);
        }


    }

    private void handlePOST(HttpExchange exchange) throws IOException {

    }
    private void handlePUT(HttpExchange exchange) throws IOException {

    }
}

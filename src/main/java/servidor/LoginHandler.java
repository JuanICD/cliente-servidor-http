package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase encargada de manejar exclusivamente las peticiones de inicio de sesión (Login).
 * Recibe usuario y contraseña, y verifica su validez contra la base de datos.
 */
public class LoginHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(LoginHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

                // 2. Leemos y procesamos los datos enviados por el cliente
                Map<String, String> data = readBody(exchange);
                String user = data.get("username");
                String pass = data.get("password");

                // Verificamos que lleguen ambos datos
                if (user == null || pass == null) {
                    // 400 Bad Request: Faltan datos
                    exchange.sendResponseHeaders(400, 0);
                    return;
                }

                // 3. Instanciamos el gestor de base de datos
                DatabaseManager db = new DatabaseManager();

                // 4. USAMOS EL METODO VALIDATEUSER QUE YA TENÍAS
                // Este metodo hashea la contraseña recibida y la compara con la de la BD
                boolean isValid = db.validateUser(user, pass);

                if (isValid) {
                    LOGGER.info("Login exitoso para el usuario: " + user);
                    String response = "¡Login correcto! Bienvenido " + user;
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (var os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    LOGGER.warning("Login fallido para el usuario: " + user);
                    String response = "Error: Credenciales incorrectas.";
                    exchange.sendResponseHeaders(401, response.getBytes().length);
                    try (var os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }

            } else {
                // Si intentan usar GET, PUT, etc. devolvemos error 405 (Method Not Allowed)
                exchange.sendResponseHeaders(405, 0);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error interno en el LoginHandler: " + e.getMessage());
            e.printStackTrace();
            // 500 Internal Server Error
            exchange.sendResponseHeaders(500, 0);
        }

    }

    /**
     * Metodo auxiliar para leer el cuerpo de la petición.
     * Convierte el flujo de bytes "user=pepe&password=123" en un Mapa.
     */
    private Map<String, String> readBody(HttpExchange exchange) {
        try {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> map = new HashMap<>();

            if (body.isEmpty()) return map;

            String[] partes = body.split("&");
            for (String parte : partes) {
                String[] datos = parte.split("=");
                if (datos.length == 2) {
                    map.put(datos[0], datos[1]);
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el cuerpo de la petición", e);
        }
    }
}

package servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PetitionHandler implements HttpHandler {
    private Logger LOGGER = Logger.getLogger(PetitionHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            //Obtenemos el tipo de la peticion, en mayúsculas para evitar errores
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "GET" -> handleGET(exchange);
                case "POST" -> handlePOST(exchange);
                case "PUT" -> handlePUT(exchange);
                case "DELETE" -> handleDELETE(exchange);
                default -> exchange.sendResponseHeaders(405, 0);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo en el manejador de peticiones intentalo de nuevo:\n" + e.getMessage());
            e.printStackTrace();
        } finally {
            exchange.close();
        }


    }


    /**
     * Gestiona una solicitud HTTP GET para listar los usuarios en el navegador.
     * Envía una respuesta en formato HTML con la lista de usuarios.
     *
     * @param exchange El objeto HTTP Exchange.
     * @throws IOException Si hay un error de E/S.
     */
    private void handleGET(HttpExchange exchange) throws IOException {
        DatabaseManager db = new DatabaseManager();
        List<String> users = db.getAllUsers();

        String response = """
                USUARIOS REGISTRADOS:
                ----------------------
                """;
        //Comprobacion de que hay usuarios registrados
        if (users.isEmpty()) {
            response += "No hay usuarios registrados.";
        } else {
            for (String user : users) {
                response += user + "\n";
            }
        }

        exchange.getResponseHeaders().set("Content-Type", "text/plain");

        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);

        //Para escribir en el flujo de salida
        try (var os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }


    }

    // Metodo POST del usuario

    /**
     * Gestiona una solicitud HTTP POST para registrar un nuevo usuario en el sistema.
     * <p>
     * Este método lee el cuerpo de la solicitud para extraer los campos "nombre de usuario" y "contraseña".
     * Si alguno de estos campos falta o no es válido, envía una respuesta 400 "Solicitud incorrecta".
     * Si el usuario se registra correctamente en la base de datos, envía una respuesta 201 "Creado". Si el registro falla porque el usuario ya existe,
     * envía una respuesta 409 "Conflicto".
     *
     * @param exchange El objeto de intercambio HTTP que contiene los detalles de la solicitud y la respuesta.
     * @throws IOException Si se produce un error de E/S al procesar la solicitud o la respuesta.
     */
    private void handlePOST(HttpExchange exchange) throws IOException {

        Map<String, String> data = readBody(exchange);
        String user = data.get("username");
        String pass = data.get("password");

        if (user == null || pass == null) {
            exchange.sendResponseHeaders(400, 0);
            return;
        }

        DatabaseManager db = new DatabaseManager();
        boolean registered = db.userRegister(user, pass);

        if (registered) {
            String response = "Usuario registrado correctamente.";
            exchange.sendResponseHeaders(201, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            String response = "Error: El usuario ya existe.";
            exchange.sendResponseHeaders(409, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }

    /**
     * Gestiona una solicitud HTTP DELETE para eliminar un usuario del sistema según el nombre de usuario proporcionado.
     * <p>
     * El método lee el cuerpo de la solicitud para extraer el nombre de usuario. Si no se proporciona,
     * envía una respuesta 400 "Solicitud incorrecta". Si el usuario se elimina correctamente de la base de datos,
     * envía una respuesta 200 "Aceptar". Si no se encuentra el usuario, envía una respuesta 404 "No encontrado".
     *
     * @param exchange El objeto HTTP Exchange que contiene los detalles de la solicitud y la respuesta.
     * @throws IOException Si se produce un error de E/S durante la gestión de la solicitud o la respuesta.
     */
    private void handleDELETE(HttpExchange exchange) throws IOException {
        Map<String, String> data = readBody(exchange);

        String user = data.get("username");

        if (user == null) {
            exchange.sendResponseHeaders(400, 0);
            return;
        }
        DatabaseManager db = new DatabaseManager();
        boolean deleted = db.deleteUser(user);

        if (deleted) {
            String response = "Usuario eliminado correctamente.";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            String response = "Error: El usuario no existe.";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }


    private void handlePUT(HttpExchange exchange) throws IOException {
        Map<String, String> data = readBody(exchange);

        String user = data.get("username");
        String oldPass = data.get("oldPassword");
        String newPass = data.get("newPassword");

        if (user == null || oldPass == null || newPass == null) {
            String response = "Error: Faltan datos (username, oldPassword o newPassword).";
            exchange.sendResponseHeaders(400, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        DatabaseManager db = new DatabaseManager();
        
        // Primero validamos la contraseña antigua
        if (!db.validateUser(user, oldPass)) {
            String response = "Error: La contraseña antigua no es correcta.";
            exchange.sendResponseHeaders(401, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        boolean updated = db.updateUser(user, newPass);

        if (updated) {
            String response = "Contraseña actualizada correctamente.";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            String response = "Error: No se pudo actualizar la contraseña (usuario no encontrado).";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

    }

    //Parsear el cuerpo a un mapa ejemplo "user=pepe&password=1234"

    /**
     * Lee el cuerpo de una solicitud HTTP del objeto HttpExchange proporcionado y lo analiza en un mapa de pares clave-valor.
     * Se espera que el cuerpo esté codificado en UTF-8 y conste de pares clave-valor separados por '&', con cada par
     * formateado como "clave=valor".
     *
     * @param exchange El objeto HttpExchange que contiene la solicitud HTTP, incluyendo el cuerpo que se leerá y analizará.
     * @return Un mapa que contiene los pares clave-valor analizados del cuerpo de la solicitud. Si el cuerpo está vacío, se devuelve un mapa vacío.
     * @throws RuntimeException Si se produce un error al leer o procesar el cuerpo de la solicitud.
     */
    private Map<String, String> readBody(HttpExchange exchange) {
        try {
            //Obtener flujo de datos del cliente en bytes
            InputStream is = exchange.getRequestBody();
            //Convertimos los bytes a String con el charset para aceptar tildes y demás caracteres
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            //Mapa clave valor para guardar los datos del body "user" -> "pepe"
            Map<String, String> map = new HashMap<>();

            if (body.isEmpty()) return map;

            //Separa las partes del cuerpo por el &
            String[] partes = body.split("&");
            for (String parte : partes) {
                String[] datos = parte.split("=");
                if (datos.length == 2) {
                    map.put(datos[0], datos[1]);
                }
            }

            return map;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

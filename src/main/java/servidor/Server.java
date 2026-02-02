package servidor;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            LOGGER.info("Iniciando servidor HTTP en el puerto " + PORT);
            //Se crea el servidor HTTP en el puerto 8080
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            //Registramos la ruta de registro y le añadimos la clase manejadora de las peticiones
            server.createContext("/users",new PetitionHandler());

            // Configuramos un ejecutor por defecto (permite concurrencia básica)
            server.setExecutor(null);

            LOGGER.info("Servidor HTTP iniciado en el puerto 8080...");
            server.start();

        }catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Error al iniciar el servidor: " + e.getMessage());
        }
    }
}

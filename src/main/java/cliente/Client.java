package cliente;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Client {

    private static final String BASE_URL = "http://localhost:8080/users";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Scanner scaner = new Scanner(System.in);


    public static void main(String[] args) {
        boolean running = true;
        System.out.println("------ CLIENTE HTTP INICIADO ------");

        while (running) {
            System.out.println("\nSeleccione una operación:");
            System.out.println("1. Registrar Usuario (POST)");
            System.out.println("2. Actualizar Contraseña (PUT)");
            System.out.println("3. Eliminar Usuario (DELETE)");
            System.out.println("4. Salir");
            System.out.print("> ");

            String option = scaner.next();

            try {
                switch (option) {
                    case "1" -> userRegister();
                    case "2" -> updateUser();
                    case "3" -> deleteUser();
                    case "4" -> {
                        running = false;
                        System.out.println("Cerrando cliente...");
                    }
                    default -> System.out.println("Opción no válida.");
                }

            } catch (Exception e) {

                System.err.println("Error de comunicacion con el servidor: " + e.getMessage());
            }


        }

    }

    private static void userRegister() throws Exception {
        System.out.print("Nombre de usuario: ");
        String username = scaner.next();
        System.out.print("Contraseña: ");
        String password = scaner.next();

        //Contruir el cuerpo de la peticion
        String body = "username=" + username + "&password=" + password;

        //Construir la peticion
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        //Enviar la peticion
        sendAndProcess(request, "Registro");

    }

    private static void updateUser() throws IOException, InterruptedException {

        System.out.println("Nombre del usuario a actualizar: ");
        String username = scaner.next();
        System.out.println("Nueva contraseña: ");
        String password = scaner.next();

        String body = "username=" + username + "&password=" + password;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        sendAndProcess(request, "Actualización");

    }


    private static void deleteUser() throws IOException, InterruptedException {
        System.out.print("Introduce usuario a eliminar: ");
        String user = scaner.next();

        // El servidor espera leer el body para saber a quién borrar
        String body = "username=" + user;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build();

        sendAndProcess(request, "Eliminación");

    }

    private static void sendAndProcess(HttpRequest request, String action) throws IOException, InterruptedException {
        System.out.println("Enviando petición al servidor...");

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int codigo = response.statusCode();

        System.out.println("--- Respuesta del Servidor ---");
        System.out.println("Código: " + codigo);

        // Interpretación de los códigos según tu Servidor
        if (codigo == 200 || codigo == 201) {
            System.out.println("Exito: " + action + " completada correctamente.");
        } else if (codigo == 409) {
            System.out.println("Error: Conflicto (El usuario ya existe).");
        } else if (codigo == 404) {
            System.out.println("Error: No encontrado (El usuario no existe).");
        } else if (codigo == 400) {
            System.out.println("Error: Petición incorrecta (Datos faltantes).");
        } else {
            System.out.println("Error desconocido.");
        }

    }


}

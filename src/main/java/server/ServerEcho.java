package server;

public class ServerEcho {
    private static ServerEcho serverEcho;
    private String pathToFolder; // путь к папке на сервере, где храняться все файлы клиентов
    private String message; // для передачи сообщений

    public String getPathToFolder() {
        return pathToFolder;
    }

    public void setPathToFolder(String pathToFolder) {
        this.pathToFolder = pathToFolder;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static ServerEcho getInstance() {
        if (serverEcho == null) {
            serverEcho = new ServerEcho();
        }
        return serverEcho;
    }
}

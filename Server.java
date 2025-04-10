import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        int proxyPort = 9090;
        ServerSocket listenerSocket = new ServerSocket(proxyPort);
        System.out.println("Network Proxy active on port " + proxyPort);

        while (true) {
            Socket remoteClient = listenerSocket.accept();
            System.out.println("Connected to client: " + remoteClient.getInetAddress());
            new Thread(new ClientHandler(remoteClient)).start();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket remoteClient;

    public ClientHandler(Socket socket) {
        this.remoteClient = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader clientReader = new BufferedReader(new InputStreamReader(remoteClient.getInputStream()));
                BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(remoteClient.getOutputStream()));
        ) {
            String inputLine;
            while ((inputLine = clientReader.readLine()) != null) {
                if (inputLine.trim().isEmpty()) continue;
                System.out.println("Request received: " + inputLine);
                String serverResponse = getWebResponse(inputLine);
                clientWriter.write(serverResponse);
                clientWriter.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getWebResponse(String webUrl) throws IOException {
        URL targetUrl = new URL(webUrl);
        HttpURLConnection httpConn = (HttpURLConnection) targetUrl.openConnection();
        httpConn.setRequestMethod("GET");

        BufferedReader webReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
        StringBuilder outputBuilder = new StringBuilder();
        String responseLine;
        while ((responseLine = webReader.readLine()) != null) {
            outputBuilder.append(responseLine).append("\n");
        }
        webReader.close();
        return outputBuilder.toString();
    }
}

import java.io.*;
import java.net.*;

public class shipServer {
    public static void main(String[] args) throws IOException {
        int localPort = 8080;
        String remoteHost = "server";
        int remotePort = 9090;


        Socket remoteSocket = new Socket(remoteHost, remotePort);
        BufferedWriter writerToProxy = new BufferedWriter(new OutputStreamWriter(remoteSocket.getOutputStream()));
        BufferedReader readerFromProxy = new BufferedReader(new InputStreamReader(remoteSocket.getInputStream()));


        ServerSocket httpListener = new ServerSocket(localPort);
        System.out.println("shipServer is active on port " + localPort);

        while (true) {
            Socket userSocket = httpListener.accept();
            new Thread(() -> manageClient(userSocket, writerToProxy, readerFromProxy)).start();
        }
    }

    private static void manageClient(Socket userSocket, BufferedWriter writerToProxy, BufferedReader readerFromProxy) {
        try (
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
                BufferedWriter clientOutput = new BufferedWriter(new OutputStreamWriter(userSocket.getOutputStream()));
        ) {
            String httpRequest = clientInput.readLine();
            if (httpRequest == null || !httpRequest.startsWith("GET")) {
                clientOutput.write("HTTP/1.1 400 Bad Request\r\n\r\n");
                clientOutput.flush();
                return;
            }

            String[] requestParts = httpRequest.split(" ");
            String targetUrl = requestParts[1];

            synchronized (writerToProxy) {
                writerToProxy.write(targetUrl + "\n");
                writerToProxy.flush();

                StringBuilder serverResponse = new StringBuilder();
                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = readerFromProxy.read(buffer)) != -1) {
                    serverResponse.append(buffer, 0, charsRead);
                    if (charsRead < buffer.length) break; 
                }

                clientOutput.write("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n");
                clientOutput.write(serverResponse.toString());
                clientOutput.flush();
            }
        } catch (IOException ioErr) {
            ioErr.printStackTrace();
        }
    }
}

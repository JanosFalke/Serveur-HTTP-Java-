import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Exemple de serveur HTTP avec celui fournit dans le JDK */
public class HttpSimpleServer {

    protected static int reqNb = 0;

    /* Simule un temps de traitement */
    private static long workLoad(long duration) {
        long startTime = System.currentTimeMillis();
        long currentTime = 0;
        try {
            // Boucle pour le durée (duration) spécifiée
            currentTime = System.currentTimeMillis();
            while ((currentTime - startTime) < duration) {
                // Toutes les 128ms, le thread s'endort pour une certaine durée...
                if ((currentTime % 128) == 0) {
                    Thread.sleep((long) Math.floor((0.1) * 10));
                }
                currentTime = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return currentTime;
    }

    protected static void HandleRequest(Socket s) {
        BufferedReader in;
        PrintWriter out;
        String request;

        try {
            String webServerAddress = s.getInetAddress().toString();
            System.out.println("New Connection:" + webServerAddress);
            // pour eviter les erreurs (Connection reset by peer) il faut augmenter la taille
            // du buffer et la on arrive a une erreur 'Out of space' pour les threads qui ne peuvent
            // plus allouer de BufferedReader (autour de 10 M/BufferedReader)
            in = new BufferedReader(new InputStreamReader(s.getInputStream()), 5000000);

            request = in.readLine();
            System.out.println("--- Client request (" + reqNb + "): " + request);
            reqNb++;

            out = new PrintWriter(s.getOutputStream(), true);
            out.println("HTTP/1.0 200");
            out.println("Content-type: text/html");
            out.println("Server-name: myserver");
            String response = "<html>"
                    + "<head>"
                    + "<title>My Web Server</title></head>"
                    + "<body><h1>Welcome to my Web Server!</h1>";
            //response += "Result = " + workLoad2().toString();
            response += "Result = " + workLoad(100L);
            response += "</body></html>";
            out.println("Content-length: " + response.length());
            out.println("");
            out.println(response);
            out.flush();
            out.close();
            s.close();
        } catch (IOException e) {
            System.out.println("Failed respond to client request: " + e.getMessage());
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    public static void main(String[] args) throws IOException {
        final int port = 8080;
        ServerSocket socket = new ServerSocket(port);
        socket.setReceiveBufferSize(30000000);
        while (true) {
            System.out.println("Waiting connection on " + port);
            final Socket connection = socket.accept();
            connection.setReceiveBufferSize(30000000);

            //Limiter à au maximum 50 threads simultanément (Testé avec ab d'Apache)
            //Donc s'il existe moins que 50 threads donc on va pouvoir créer un nouveau
            if (TraitementHttp.getNb() <= 50) {
                TraitementHttp thttp = new TraitementHttp(connection);
                Thread t = new Thread(thttp);
                t.start();

                //Après avoir fini le traitement (run) il faut décrementer le nombre de requetes
                // -> dans le run de TraitementHttp

            }

        }
    }

}

//Création d'une classe pour pouvoir traiter chaque thread individuellement
class TraitementHttp extends HttpSimpleServer implements Runnable {

    private Socket connection;

    TraitementHttp(Socket conn) {
        this.connection = conn;
    }

    public static int getNb() {
        return reqNb;
    }

    public void run() {
        //Il faut attendre que le run a fini avant de décrementer le nombre de requete
        synchronized (this){
            HandleRequest(connection);
        }
        HttpSimpleServer.reqNb--;    
            //Le traitement a fini et le thread est fini donc on decremente le compteur

    }
}

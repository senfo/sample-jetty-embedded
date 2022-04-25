import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

public class HelloWorld extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AsyncContext async = request.startAsync();
        ServletOutputStream output = response.getOutputStream();
        final String changeMe = "{ status: \"good\" }";
        final ByteBuffer content = ByteBuffer.wrap(changeMe.getBytes(StandardCharsets.UTF_8));

        response.setContentType("application/json");

        output.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
                while (output.isReady()) {
                    if (!content.hasRemaining()) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        async.complete();

                        return;
                    }

                    output.write(content.get());
                }
            }

            @Override
            public void onError(Throwable err) {
                getServletContext().log("Error in container health status", err);
                async.complete();
            }
        });
    }

    public static void main(String[] args) throws Exception {
        final Server server = new Server();
        final ServerConnector serverConnector = new ServerConnector(server);
        final ServletHandler servletHandler = new ServletHandler();

        serverConnector.setPort(8080);
        server.setConnectors(new Connector[] { serverConnector });
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(HelloWorld.class, "/api/health");

        server.start();
        server.join();
    }
}

package asepaper.pddlgenerator;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by eathkar on 06/06/16.
 */
public class PDDLWebService  extends AbstractHandler {
    @Override
    public void handle(
            String s,
            Request request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if (request.getMethod().compareTo("POST") == 0) {
            StringBuffer sb = new StringBuffer();
            BufferedReader bufferedReader = null;
            String content = "";

            try {
                bufferedReader =  request.getReader() ;
                char[] charBuffer = new char[128];
                int bytesRead;
                while ( (bytesRead = bufferedReader.read(charBuffer)) != -1 ) {
                    sb.append(charBuffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                throw ex;
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException ex) {
                        throw ex;
                    }
                }
            }

            try {
                JSONObject object = new JSONObject(sb.toString());
                String domainModel = object.get("domainModel").toString();
                String objectModel = object.get("problemModel").toString();

                PDDLGenerator gen = new PDDLGenerator();
                String data[] = gen.GeneratePlan(domainModel, objectModel, false);

                JSONObject returnObject = new JSONObject();
                returnObject.append("problemFile", data[0]);
                returnObject.append("domainFile", data[1]);

                constructResponse(
                        request,
                        httpServletResponse,
                        HttpServletResponse.SC_OK,
                        returnObject.toString());
                return;
            }

            catch (Exception ex){
                constructResponse(
                        request,
                        httpServletResponse,
                        HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Error when processing request (is JSON syntactically correct " +
                                "and are domainModel and problemModel elements in the file?). " +
                                "\nMore info:"+ex.toString());
                return;
            }
        }
        else {
            constructResponse(
                    request,
                    httpServletResponse,
                    HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Method "+request.getMethod()+" not supported, only POST methods are supported.");
            return;
        }
    }

    private void constructResponse(Request request, HttpServletResponse response,
                                   int responseType, String message) throws IOException{
        request.setHandled(true);
        response.setContentType("application/text");
        response.setStatus(responseType);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.getWriter().write(message);
        response.getWriter().flush();
        response.getWriter().close();
        System.out.println("Response to "+request.getMethod()+": "+responseType+" "+message);
    }
}

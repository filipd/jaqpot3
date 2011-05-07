package org.opentox.jaqpot3.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TrainGeneric extends JaqpotResource {

    public static final URITemplate template = new URITemplate("train", "generic", null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html><head><title>Jaqpot Login Page</title>");
        //formBuilder.append(GoogleAnalytics.getGAjs());
        formBuilder.append("</head>");
        formBuilder.append("<body>");
        formBuilder.append("<h1>Train Any model</h1>");


        formBuilder.append("<form method=\"POST\" action=\"#\">");
        formBuilder.append("<table>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Algorithm URI</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"" + Configuration.getBaseUri().augment("algorithm", "mlr") + "\" name=\"algorithm_uri\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Dataset URI</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/dataset/54\" name=\"dataset_uri\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Prediction Feature</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/feature/22202\" name=\"prediction_feature\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("<tr>");
        formBuilder.append("<td>Feature Service</td><td><input type=\"text\"  size=\"60\" maxlength=\"80\"  "
                + "value=\"http://apps.ideaconsult.net:8080/ambit2/feature\" name=\"feature_service\"></td>");
        formBuilder.append("</tr>");

        formBuilder.append("</table>");
        formBuilder.append("<input type=\"submit\" value=\"Train\">");

        formBuilder.append("</form><br/>");

        formBuilder.append("<p>Click <a href=\"..\">here</a> to go back to the main page</p>");
        formBuilder.append("</body></html>");
        return new StringRepresentation(formBuilder, MediaType.TEXT_HTML);
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        AuthenticationToken userToken = getUserToken();
        if (userToken == null) {
            toggleSeeOther("/login?redirect=" + getCurrentVRI());
        }
        IClientInput input = new ClientInput(entity);
        String ds = input.getFirstValue("dataset_uri");
        String pf = input.getFirstValue("prediction_feature");
        String alg = input.getFirstValue("algorithm_uri");
        IPostClient client = null;
        try {
            client = ClientFactory.createPostClient(new VRI(alg));
        } catch (URISyntaxException ex) {
            Logger.getLogger(TrainGeneric.class.getName()).log(Level.SEVERE, null, ex);
        }
        client.setMediaType(Media.TEXT_URI_LIST);
        System.out.println(ds);
        System.out.println(pf);
        client.addPostParameter("dataset_uri", ds);
        client.addPostParameter("prediction_feature", pf);
        client.authorize(userToken);
        try {
            client.post();
        } catch (ServiceInvocationException ex) {
        }
        String nextUri = null;
        try {
            nextUri = client.getResponseUriList().iterator().next().toString();
        } catch (ServiceInvocationException ex) {
            //return ex.asErrorReport().;
        }
        try {
            client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String htmlResponse = "<html>" +
                "<head>" +
                "  <title>Task Created</title>" +
                "</head>" +
                "<body>" +
                "<p>Message : </p>\n" +
                "<p><a href=\""+nextUri+"\">Task Created</a></p>" +
                "</body>" +
                "</html>";
        System.out.println(htmlResponse);
        toggleSuccess();
        return new StringRepresentation(htmlResponse, MediaType.TEXT_HTML);
    }
}
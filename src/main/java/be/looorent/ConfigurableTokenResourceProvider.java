package be.looorent;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
// import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;


// import java.util.ArrayList;
import java.util.List;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Lorent Lempereur
 */
public class ConfigurableTokenResourceProvider implements RealmResourceProvider {

    static final String ID = "execute-actions";
    private static final Logger LOG = Logger.getLogger(ConfigurableTokenResourceProvider.class);

    private final KeycloakSession session;

    ConfigurableTokenResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {}

    public static UriBuilder actionTokenProcessor(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return baseUriBuilder;
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getResetPasswordLink(// Parameters needed by this provider implementation
                                         @QueryParam("userId") String userId,
                                         // Parameters from Keycloak original method 'executeActionsEmail'
                                         @QueryParam("redirectUri") String redirectUri,
                                         @QueryParam("clientId") String clientId,
                                         @QueryParam("lifespan") Integer lifespan,
                                         List<String> actions) {
        try {
            // get realm, client, lifespan
            RealmModel realm = session.getContext().getRealm();
            if(lifespan == null) {
                lifespan = realm.getActionTokenGeneratedByAdminLifespan();
            }
            int expiration = Time.currentTime() + lifespan;
            System.out.println(expiration);
            ClientModel client = realm.getClientByClientId(clientId);

            // get user by email
            UserModel user = this.session.users().getUserById(realm, userId);

            // check if user email exists
            if (user.getEmail() == null) {
                throw new ConfigurableTokenException("User email missing");
            }
            // check if user is enabled
            if (!user.isEnabled()) {
                throw new ConfigurableTokenException("User is disabled");
            }
            // check if client id is not missing
            if (clientId == null) {
                throw new ConfigurableTokenException("Client id missing");
            }
            // check if client is found
            if (client == null) {
                throw new ConfigurableTokenException("Client doesn't exist");
            }
            // check if client is enabled
            if (!client.isEnabled()) {
                throw new ConfigurableTokenException("Client is not enabled");
            }
            // check if the redirect uri is valid
            String redirect;
            if (redirectUri != null) {
                redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
                if (redirect == null) {
                    throw new ConfigurableTokenException("Invalid redirect uri");
                }
            }

            // generate token
            ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), user.getEmail(), expiration, actions, redirectUri, client.getClientId());

            // get builder
            UriBuilder builder = actionTokenProcessor(session.getContext().getUri());
            builder.path("/realms/" + realm.getName() + "/login-actions/action-token");
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

            // get the link
            String link = builder.build(realm.getName()).toString();

            return Response.ok(link).build();
        } catch (ConfigurableTokenException e) {
            LOG.error("An error occurred when fetching an access token", e);
            ErrorRepresentation error = new ErrorRepresentation();
            error.setErrorMessage(e.getMessage());
            return Response.status(BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build();
        }
    }

    static class ConfigurableTokenException extends Exception {
        public ConfigurableTokenException(String message) {
            super(message);
        }
    }
}

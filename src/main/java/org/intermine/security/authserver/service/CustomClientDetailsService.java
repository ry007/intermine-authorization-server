package org.intermine.security.authserver.service;

import org.intermine.security.authserver.model.OauthClientDetails;
import org.intermine.security.authserver.repository.ClientDetailRepository;
import org.intermine.security.authserver.security.CustomPasswordEncoder;
import org.intermine.security.authserver.security.Encryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.crypto.keygen.KeyGenerators.secureRandom;

/**
 * This class extends spring default oauth2 client details
 * service and overrides the default methods according
 * to Intermine Authorization server requirements.
 *
 *
 * @author Rahul Yadav
 *
 */
@Service
public class CustomClientDetailsService extends JdbcClientDetailsService {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(CustomClientDetailsService.class);

    /**
     * An object of jpa repository to query oauth_client_details table in database.
     */
    @Autowired
    private ClientDetailRepository iOauthClientDetails;

    /**
     * <p>This is used for the dynamic client registration
     * purpose. Datasource is used to obtain database connections
     * without knowing about the connection details.
     *  </p>
     *
     * @param dataSource Object of DataSource
     */
    public CustomClientDetailsService(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * <p>Return an instance of custom password encoder
     * to encode client credentials.
     * </p>
     *
     * @return A new instance of CustomPasswordEncoder
     */
    private PasswordEncoder passwordEncoder() {
        return new CustomPasswordEncoder();
    }

    /**
     * <p>This method load a client from the database
     * using unique clientId of the client.
     * </p>
     *
     * @param clientId unique ClientId of client
     * @return An instance of Spring default ClientDetails
     */
    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        Optional<OauthClientDetails> oauthClientDetails = Optional.ofNullable(iOauthClientDetails.findByClientId(clientId));


        if (!oauthClientDetails.isPresent()) {
            throw new ClientRegistrationException("invalid_client");
        }

        OauthClientDetails client = oauthClientDetails.get();

		/*client.setAuthorizedGrantTypes(new HashSet<>(Arrays.asList("password", "authorization_code",
				"refresh_token", "implicit")));
		client.setScope(new HashSet<>(Arrays.asList("scope_read", "scope_write", "trust")));*/

        String resourceIds = client.getResourceIds().stream().collect(Collectors.joining(","));
        String scopes = client.getScope().stream().collect(Collectors.joining(","));
        String grantTypes = client.getAuthorizedGrantTypes().stream().collect(Collectors.joining(","));

        Logger.debug("RESOURCE_ID {}, SCOPE {}, GRANT_TYPES {}", resourceIds, scopes, grantTypes);

        return new BaseClientDetails(client);
    }

    /**
     * <p>This method load a client from the database
     * using name of the client.
     * </p>
     *
     * @param clientName Name of the client
     * @return Object of OauthClientDetails model class
     */
    public OauthClientDetails loadClientByClientName(String clientName) {
        OauthClientDetails oauthClientDetails = iOauthClientDetails.findByClientName(clientName);
        return oauthClientDetails;
    }

    /**
     * <p>This method load a client from the database
     * using unique website url of the client.
     * </p>
     *
     * @param websiteUrl Unique website url of the client
     * @return Object of OauthClientDetails model class
     */
    public OauthClientDetails loadClientByWebsiteUrl(String websiteUrl) {
        OauthClientDetails oauthClientDetails = iOauthClientDetails.findByWebsiteUrl(websiteUrl);
        return oauthClientDetails;
    }

    /**
     * <p>This method loads a list of clients which are registered
     * by a particular user.
     * </p>
     *
     * @param registeredBy owner of the clients
     * @return List of OauthClientDetails model objects
     */
    public List<OauthClientDetails> loadClientByUsername(String registeredBy) {
        List<OauthClientDetails> oauthClientDetails = iOauthClientDetails.findAllByRegisteredBy(registeredBy);
        return oauthClientDetails;
    }


    /**
     * <p>This method saves a new client in the table.
     * </p>
     *
     * @param clientDetails details of the client in the object
     */
    public HashMap<String, String> addCustomClientDetails(OauthClientDetails clientDetails) throws ClientAlreadyExistsException, NoSuchAlgorithmException {
        OauthClientDetails oauthClientDetail = new OauthClientDetails();
        oauthClientDetail.setClientName(clientDetails.getClientName());
        oauthClientDetail.setRegisteredRedirectUri(clientDetails.getRegisteredRedirectUri());
        oauthClientDetail.setWebsiteUrl(clientDetails.getWebsiteUrl());
        oauthClientDetail.setAccessTokenValiditySeconds(3600);
        oauthClientDetail.setRefreshTokenValiditySeconds(10000);
        oauthClientDetail.setRegisteredBy(clientDetails.getRegisteredBy());
        oauthClientDetail.setClientType(clientDetails.getClientType());
        oauthClientDetail.setScope(new HashSet<String>(Arrays.asList("openid", "profile", "email")));
        oauthClientDetail.setAuthorizedGrantTypes(new HashSet<String>(Arrays.asList("authorization_code", "password", "refresh_token", "implicit")));
        iOauthClientDetails.save(oauthClientDetail);
        HashMap<String, String> map = new HashMap<>();
        return map;
    }


    /**
     * <p>This method updates client redirect uri on user request.
     * </p>
     *
     * @param clientName The client whose redirect Uri is to update
     * @param redirectUri new redirect uri to update
     */
    public void updateClientRedirectUri(String clientName, String redirectUri) throws NoSuchClientException {
        OauthClientDetails oauthClientDetails = iOauthClientDetails.findByClientName(clientName);
        String tesss = redirectUri.substring(1, redirectUri.length() - 1);
        oauthClientDetails.setRegisteredRedirectUri(Collections.singleton(tesss));
        iOauthClientDetails.save(oauthClientDetails);
    }


    /**
     * <p>This method deletes a client from the database on the
     * client owner request.
     * </p>
     *
     * @param clientName name of the client to be delete
     */
    public void deleteClient(String clientName){
        iOauthClientDetails.deleteByClientName(clientName);
    }


    /**
     * <p>This method verifies a client on admin request
     * and generates required credentials for client and
     * save them in client details.
     * </p>
     *
     * @param clientName name of the client to be verify
     */
    public void verifyClient(String clientName) throws NoSuchAlgorithmException {
        OauthClientDetails oauthClientDetails = iOauthClientDetails.findByClientName(clientName);
        String currentClientId = Encryption.SHA1(secureRandom(16).generateKey()) + ".apps.intermine.com";
        oauthClientDetails.setClientId(currentClientId);
        String currentClientSecret = Encryption.SHA1(secureRandom(16).generateKey());
        oauthClientDetails.setClientSecret(passwordEncoder().encode(currentClientSecret));
        oauthClientDetails.setStatus(true);
        iOauthClientDetails.save(oauthClientDetails);
    }
}

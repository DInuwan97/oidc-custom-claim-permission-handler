package org.wso2.custom.claim;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.internal.OAuth2ServiceComponentHolder;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.openidconnect.DefaultOIDCClaimsCallbackHandler;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom OIDC Claim handler to return permissions.
 */
public class PermissionClaimHandler extends DefaultOIDCClaimsCallbackHandler {

    private static final String PERMISSION_CLAIM = "http://wso2.org/claims/permission";
    private static final Log log = LogFactory.getLog(PermissionClaimHandler.class);
    private static final String OAUTH2 = "oauth2";

    @Override
    public JWTClaimsSet handleCustomClaims(JWTClaimsSet.Builder jwtClaimsSetBuilder, OAuthTokenReqMessageContext
            tokenReqMessageContext) {

        AuthenticatedUser authenticatedUser = tokenReqMessageContext.getAuthorizedUser();
        if (authenticatedUser == null) {
            log.error("Authenticated user not found.");
        } else {
            String userName = authenticatedUser.toFullQualifiedUsername();
            String userTenantDomain = authenticatedUser.getTenantDomain();
            String spTenantDomain = tokenReqMessageContext.getOauth2AccessTokenReqDTO().getTenantDomain();
            String clientId = tokenReqMessageContext.getOauth2AccessTokenReqDTO().getClientId();

            handleUserPermissions(jwtClaimsSetBuilder, userName, userTenantDomain, spTenantDomain, clientId);
        }
        return super.handleCustomClaims(jwtClaimsSetBuilder, tokenReqMessageContext);
    }

    @Override
    public JWTClaimsSet handleCustomClaims(JWTClaimsSet.Builder jwtClaimsSetBuilder,
                                           OAuthAuthzReqMessageContext authzReqMessageContext) {

        AuthenticatedUser authenticatedUser = authzReqMessageContext.getAuthorizationReqDTO().getUser();
        if (authenticatedUser == null) {
            log.error("Authenticated user not found.");
        } else {
            String userName = authenticatedUser.toFullQualifiedUsername();
            String userTenantDomain = authenticatedUser.getTenantDomain();
            String spTenantDomain = authzReqMessageContext.getAuthorizationReqDTO().getTenantDomain();
            String clientId = authzReqMessageContext.getAuthorizationReqDTO().getConsumerKey();

            handleUserPermissions(jwtClaimsSetBuilder, userName, userTenantDomain, spTenantDomain, clientId);
        }
        return super.handleCustomClaims(jwtClaimsSetBuilder, authzReqMessageContext);
    }

    private void handleUserPermissions(JWTClaimsSet.Builder jwtClaimsSetBuilder, String userName,
                                       String userTenantDomain, String spTenantDomain,
                                       String clientId) {

        try {
            UserRealm realm = IdentityTenantUtil.getRealm(userTenantDomain, userName);
            if (realm == null) {
                throw new IdentityException("User realm is empty.");
            }
            ServiceProvider serviceProvider = getServiceProvider(spTenantDomain, clientId);
            ClaimMapping[] requestClaimMappings = getRequestedClaimMappings(serviceProvider);

            List<String> requestedClaimUris = getRequestedClaimUris(requestClaimMappings);
            AuthorizationManager authorizationManager = realm.getAuthorizationManager();
            String[] permissionList = null;
            for (String claimUri : requestedClaimUris) {
                if (claimUri.contains(PERMISSION_CLAIM)) {
                    String permissionRootPath = claimUri.replace("http://wso2.org/claims", "");
                    permissionList =
                            authorizationManager
                                    .getAllowedUIResourcesForUser(MultitenantUtils.getTenantAwareUsername(userName),
                                            permissionRootPath);
                }
            }

            if (ArrayUtils.isNotEmpty(permissionList)) {
                jwtClaimsSetBuilder.claim("permission", permissionList);
            }
        } catch (IdentityApplicationManagementException e) {
            log.error(
                    "Error while obtaining service provider for tenant domain: " + spTenantDomain + " client id: "
                            + clientId, e);
        } catch (UserStoreException e) {
            log.error("Error while retrieving user claim in local dialect for user: " + userName, e);
        } catch (IdentityException e) {
            log.error("Error while obtaining user realm for for user: " + userName + " in tenant domain: " +
                    userTenantDomain, e);
        }
    }

    private ServiceProvider getServiceProvider(String spTenantDomain,
                                               String clientId) throws IdentityApplicationManagementException {

        ApplicationManagementService applicationMgtService = OAuth2ServiceComponentHolder.getApplicationMgtService();
        String spName = applicationMgtService.getServiceProviderNameByClientId(clientId, OAUTH2, spTenantDomain);

        if (log.isDebugEnabled()) {
            log.debug("Retrieving service provider for clientId: " + clientId + " in tenantDomain: " + spTenantDomain);
        }
        return applicationMgtService.getApplicationExcludingFileBasedSPs(spName, spTenantDomain);
    }

    private ClaimMapping[] getRequestedClaimMappings(ServiceProvider serviceProvider) {

        if (serviceProvider.getClaimConfig() == null) {
            return new ClaimMapping[0];
        }
        return serviceProvider.getClaimConfig().getClaimMappings();
    }

    private List<String> getRequestedClaimUris(ClaimMapping[] requestedLocalClaimMap) {

        List<String> claimURIList = new ArrayList<>();
        for (ClaimMapping mapping : requestedLocalClaimMap) {
            if (mapping.isRequested()) {
                claimURIList.add(mapping.getLocalClaim().getClaimUri());
            }
        }
        return claimURIList;
    }
}

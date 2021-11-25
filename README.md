# Custom OIDC Permission Claim Handler

This is a custom claim permission handler for WSO2 Identity Server that is built to work with the /oauth2/token endpoint as opposed to the recommended /oauth2/authorize endpoint.

An advantage of this custom handler is that redirection is not required, since it used the ```password``` grant type and the /oauth2/token endpoint.

## 1 - Getting Started

1. Fork and clone the project.
2. Run ```mvn clean install``` in a terminal at the root directory of this project.
3. Find the built jar artifact file in the ```target``` folder.

## 2 - Steps to configure the custom claim permission handler

1. Add the ```org.wso2.oidc.custom.claim.handler-1.0.jar``` file into the ```<IS_HOME>/repository/components/lib``` folder.
2. Include the following configuration in the ```<IS_HOME>/repository/conf/deployment.toml``` file.
```
[oauth.oidc.extensions]
claim_callback_handler="org.wso2.oidc.custom.claim.OIDCPermissionClaimHandler"
```
3. Start the WSO2 Identity Server Instance.
4. Create a new 'Local Claim' to represent the user permissions.
   1. Go to ```Claims``` section in the management console, click ```Add``` and then ```Add Local Claim``` option.
   2. Decide on which level you need the permissions to be returned and create the claim considering the structure.
   3. Or you can retrieve all permissions assigned to a particular user identity by defining the Claim URI as 'http://wso2.org/claims/permission'.

More information about adding a local claim can be found here: https://is.docs.wso2.com/en/latest/learn/adding-claim-mapping/#add-local-claim.

5. Create an External Claim that is mapped to the local claim that was created in Step 2.
   1. Go to ```Claims``` section in the management console, click ```Add``` and then ```Add External Claim``` option.
   2. Select ```http://wso2.org/oidc/claim``` as the Dialect URI, provide a desired name for External Claim URI field and select the local claim URI that was created in Step 4.
6. Click on the ```List``` button under ```Manage``` and then ```OIDC Scopes```.
7. Locate the ```openid``` scope and click on ```Add Claims``` option.
8. Click on ```Add OIDC Claim``` and select the claim that was created from the dropdown and click on ```Add```.
9. The Service Provider can now be created. Click on ```Add``` under ```Service Providers``` and register the service provider with a suitable name.
10. Expand the ```Claim Configuration``` section in the Service Provider and add the claim that was created in Step 4 to the Requested Claims sub-section.
11. To create permissions specific to an application, expand the ```Role/Permission Configuration``` section and expand ```Permissions```.
12. Click on ```Add Permission``` and specify the desired service provider specific permission that you want to add. These permissions would be added to the permission tree under ```Applications```. 
13. Expand the ```Inbound Authentication Configuration``` section and then expand the ```OAuth/OpenID Connect Configuration```. Click ```Configure```.
14. Fill in the callback URL of the application and click on ```Add```
15. The ```OAuth Client Key``` and ```OAuth Client Secret``` values will be generated. Copy those values.
16. Make sure the user has been assigned with the roles that have the required application permissions.

## 3 - Steps to use the custom claim permission handler
1. Make an authorization request to WSO2 Identity Server using the /oauth2/token endpoint while ensuring that placeholder text are replaced with the values corresponding to your configuration.
```
curl -v -X POST --basic -u <OAuth_Client_Key>:<OAuth_Client_Secret> -H 'Content-Type: application/x-www-form-urlencoded;charset=UTF-8' -k -d 'grant_type=password&username=<username_of_user>&password=<password_of_user>&redirect_uri=<redirect_uri>&scope=openid' https://<hostname_of_WSO2_IS>:<port_of_WSO2_IS>/oauth2/token
```
2. The response will contain the following items:
   1. access_token
   2. refresh_token
   3. scope
   4. id_token
   5. token_type
   6. expires_in
3. Copy the ```id_token``` response and parse it using a tool like ```jwt.io``` to see the permissions in JSON format. A sample ```id_token``` payload will be shown below:

```
{
  "at_hash": "IBzF6JjI8bAE8saZwsfyJg",
  "aud": "G6v0WGDSZJZljauwCRoMh_RVAyca",
  "sub": "sanjula",
  "nbf": 1637822052,
  "azp": "G6v0WGDSZJZljauwCRoMh_RVAyca",
  "permissions": {
    "/permission": [
      "/admin/manage/identity/claimmgt/claim/delete",
      "/admin/manage/identity/defaultauthSeq",
      "/admin/manage/identity/template/mgt/list",
      "/admin/manage/identity/template/mgt/view",
      "/admin/manage/cors/origins/view",
      "/admin/manage/identity/entitlement",
      "/admin/manage/extensions",
      "/admin/manage/identity/user",
      "/admin/manage/identity/user/association/create",
      "/admin/login",
      "/admin/manage/identity/identitymgt",
      "/admin",
      "/admin/manage/identity/usermgt",
      "/applications/ABC_Company/manage",
      "/applications/ABC_Company/vip",
 
    ]
  },
  "amr": [
    "password"
  ],
  "iss": "https://localhost:9443/oauth2/token",
  "exp": 1637825652,
  "iat": 1637822052
}
```
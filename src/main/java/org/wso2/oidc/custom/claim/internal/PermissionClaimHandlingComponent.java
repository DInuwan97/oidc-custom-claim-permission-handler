package org.wso2.oidc.custom.claim.internal;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.core.service.RealmService;

public class PermissionClaimHandlingComponent {

    public static RealmService getRealmService() {

        RealmService realmService =
                (RealmService) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
                        (RealmService.class, null);
        return realmService;
    }

}

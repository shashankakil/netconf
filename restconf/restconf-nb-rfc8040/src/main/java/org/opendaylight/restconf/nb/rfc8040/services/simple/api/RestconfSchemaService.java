/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.services.simple.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.opendaylight.restconf.common.schema.SchemaExportContext;
import org.opendaylight.restconf.nb.rfc8040.Rfc8040;
import org.opendaylight.restconf.nb.rfc8040.utils.RestconfConstants;
import org.opendaylight.yangtools.yang.common.YangConstants;

/**
 * Retrieval of the YANG modules which server supports.
 *
 */
@Path("/")
public interface RestconfSchemaService extends UpdateHandlers {

    /**
     * Get schema of specific module.
     *
     * @param identifier
     *             path parameter
     * @return {@link SchemaExportContext}
     */
    @GET
    @Produces({ Rfc8040.MediaTypes.YIN + RestconfConstants.XML, YangConstants.RFC6020_YANG_MEDIA_TYPE })
    @Path("modules/{identifier:.+}")
    SchemaExportContext getSchema(@PathParam("identifier") String identifier);
}

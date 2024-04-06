/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.opendaylight.restconf.api.FormattableBody;
import org.opendaylight.restconf.api.MediaTypes;

@Provider
@Produces({ MediaTypes.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
public final class JsonFormattableBody extends FormattableBodyWriter {
    @Override
    void writeTo(final FormattableBody body, final OutputStream out) throws IOException {
        body.formatToJSON(out);
    }
}
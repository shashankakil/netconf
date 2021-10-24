/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.databind.jaxrs;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.nb.rfc8040.FilterParameter;
import org.opendaylight.restconf.nb.rfc8040.InsertParameter;
import org.opendaylight.restconf.nb.rfc8040.NotificationQueryParams;
import org.opendaylight.restconf.nb.rfc8040.PointParameter;
import org.opendaylight.restconf.nb.rfc8040.StartTimeParameter;
import org.opendaylight.restconf.nb.rfc8040.StopTimeParameter;
import org.opendaylight.restconf.nb.rfc8040.WriteDataParams;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;

@Beta
public final class QueryParams {
    private QueryParams() {
        // Utility class
    }

    public static @NonNull NotificationQueryParams newNotificationQueryParams(final UriInfo uriInfo) {
        StartTimeParameter startTime = null;
        StopTimeParameter stopTime = null;
        FilterParameter filter = null;
        boolean skipNotificationData = false;

        for (final Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            final String paramName = entry.getKey();
            final List<String> paramValues = entry.getValue();

            try {
                if (paramName.equals(StartTimeParameter.uriName())) {
                    startTime = optionalParam(StartTimeParameter::forUriValue, paramName, paramValues);
                    break;
                } else if (paramName.equals(StopTimeParameter.uriName())) {
                    stopTime = optionalParam(StopTimeParameter::forUriValue, paramName, paramValues);
                    break;
                } else if (paramName.equals(FilterParameter.uriName())) {
                    filter = optionalParam(FilterParameter::forUriValue, paramName, paramValues);
                } else if (paramName.equals("odl-skip-notification-data")) {
                    // FIXME: this should be properly encapsulated in SkipNotificatioDataParameter
                    skipNotificationData = Boolean.parseBoolean(optionalParam(paramName, paramValues));
                } else {
                    throw new RestconfDocumentedException("Bad parameter used with notifications: " + paramName);
                }
            } catch (IllegalArgumentException e) {
                throw new RestconfDocumentedException("Invalid " + paramName + " value: " + e.getMessage(), e);
            }
        }

        try {
            return NotificationQueryParams.of(startTime, stopTime, filter, skipNotificationData);
        } catch (IllegalArgumentException e) {
            throw new RestconfDocumentedException("Invalid query parameters: " + e.getMessage(), e);
        }
    }

    public static @NonNull WriteDataParams newWriteDataParams(final UriInfo uriInfo) {
        InsertParameter insert = null;
        PointParameter point = null;

        for (final Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            final String uriName = entry.getKey();
            final List<String> paramValues = entry.getValue();
            if (uriName.equals(InsertParameter.uriName())) {
                final String str = optionalParam(uriName, paramValues);
                if (str != null) {
                    insert = InsertParameter.forUriValue(str);
                    if (insert == null) {
                        throw new RestconfDocumentedException("Unrecognized insert parameter value '" + str + "'",
                            ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT);
                    }
                }
            } else if (PointParameter.uriName().equals(uriName)) {
                final String str = optionalParam(uriName, paramValues);
                if (str != null) {
                    point = PointParameter.forUriValue(str);
                }
            } else {
                throw new RestconfDocumentedException("Bad parameter for post: " + uriName,
                    ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT);
            }
        }

        try {
            return WriteDataParams.of(insert, point);
        } catch (IllegalArgumentException e) {
            throw new RestconfDocumentedException("Invalid query parameters: " + e.getMessage(), e);
        }
    }

    public static @Nullable String getSingleParameter(final MultivaluedMap<String, String> params, final String name) {
        final var values = params.get(name);
        return values == null ? null : optionalParam(name, values);
    }

    private static @Nullable String optionalParam(final String name, final List<String> values) {
        switch (values.size()) {
            case 0:
                return null;
            case 1:
                return requireNonNull(values.get(0));
            default:
                throw new RestconfDocumentedException("Parameter " + name + " can appear at most once in request URI",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    private static <T> @Nullable T optionalParam(final Function<String, @NonNull T> factory, final String name,
            final List<String> values) {
        final String str = optionalParam(name, values);
        return str == null ? null : factory.apply(str);
    }
}
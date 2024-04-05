/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.jersey.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.restconf.api.MediaTypes;
import org.opendaylight.restconf.common.errors.RestconfError;
import org.opendaylight.restconf.server.api.DatabindContext;
import org.opendaylight.restconf.server.api.PatchStatusContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.patch.rev170222.yang.patch.status.YangPatchStatus;

@Provider
@Produces(MediaTypes.APPLICATION_YANG_DATA_XML)
public class XmlPatchStatusBodyWriter extends AbstractPatchStatusBodyWriter {
    private static final String XML_NAMESPACE = YangPatchStatus.QNAME.getNamespace().toString();
    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    @Override
    void writeTo(final PatchStatusContext body, final OutputStream out) throws IOException {
        try {
            final var xmlWriter = XML_FACTORY.createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
            writeDocument(xmlWriter, body);
        } catch (final XMLStreamException e) {
            throw new IOException("Failed to write body", e);
        }
    }

    private static void writeDocument(final XMLStreamWriter writer, final PatchStatusContext body)
            throws XMLStreamException {
        writer.writeStartElement("", "yang-patch-status", XML_NAMESPACE);
        writer.writeStartElement("patch-id");
        writer.writeCharacters(body.patchId());
        writer.writeEndElement();

        if (body.ok()) {
            writer.writeEmptyElement("ok");
        } else {
            final var globalErrors = body.globalErrors();
            if (globalErrors != null) {
                reportErrors(body.databind(), globalErrors, writer);
            } else {
                writer.writeStartElement("edit-status");
                for (var patchStatusEntity : body.editCollection()) {
                    writer.writeStartElement("edit");
                    writer.writeStartElement("edit-id");
                    writer.writeCharacters(patchStatusEntity.getEditId());
                    writer.writeEndElement();

                    final var editErrors = patchStatusEntity.getEditErrors();
                    if (editErrors != null) {
                        reportErrors(body.databind(), editErrors, writer);
                    } else if (patchStatusEntity.isOk()) {
                        writer.writeEmptyElement("ok");
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
        writer.flush();
    }

    private static void reportErrors(final DatabindContext databind, final List<RestconfError> errors,
            final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("errors");

        for (var restconfError : errors) {
            writer.writeStartElement("error-type");
            writer.writeCharacters(restconfError.getErrorType().elementBody());
            writer.writeEndElement();

            writer.writeStartElement("error-tag");
            writer.writeCharacters(restconfError.getErrorTag().elementBody());
            writer.writeEndElement();

            // optional node
            final var errorPath = restconfError.getErrorPath();
            if (errorPath != null) {
                writer.writeStartElement("error-path");
                databind.xmlCodecs().instanceIdentifierCodec().writeValue(writer, errorPath);
                writer.writeEndElement();
            }

            // optional node
            final var errorMessage = restconfError.getErrorMessage();
            if (errorMessage != null) {
                writer.writeStartElement("error-message");
                writer.writeCharacters(errorMessage);
                writer.writeEndElement();
            }

            // optional node
            final var errorInfo = restconfError.getErrorInfo();
            if (errorInfo != null) {
                writer.writeStartElement("error-info");
                writer.writeCharacters(errorInfo);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }
}

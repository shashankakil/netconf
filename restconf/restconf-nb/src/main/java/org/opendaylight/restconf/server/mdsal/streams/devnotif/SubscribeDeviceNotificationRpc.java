/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.server.mdsal.streams.devnotif;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.restconf.api.ApiPath;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.common.errors.RestconfFuture;
import org.opendaylight.restconf.server.api.ServerException;
import org.opendaylight.restconf.server.spi.ApiPathCanonizer;
import org.opendaylight.restconf.server.spi.OperationInput;
import org.opendaylight.restconf.server.spi.RestconfStream;
import org.opendaylight.restconf.server.spi.RpcImplementation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.device.notification.rev240218.SubscribeDeviceNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.device.notification.rev240218.SubscribeDeviceNotificationInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.device.notification.rev240218.SubscribeDeviceNotificationOutput;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * RESTCONF implementation of {@link SubscribeDeviceNotification}.
 */
@Singleton
@Component(service = RpcImplementation.class)
public final class SubscribeDeviceNotificationRpc extends RpcImplementation {
    private static final NodeIdentifier DEVICE_NOTIFICATION_PATH_NODEID =
        NodeIdentifier.create(QName.create(SubscribeDeviceNotificationInput.QNAME, "path").intern());
    private static final NodeIdentifier DEVICE_NOTIFICATION_STREAM_NAME_NODEID =
        NodeIdentifier.create(QName.create(SubscribeDeviceNotificationInput.QNAME, "stream-name").intern());

    private final DOMMountPointService mountPointService;
    private final RestconfStream.Registry streamRegistry;

    @Inject
    @Activate
    public SubscribeDeviceNotificationRpc(@Reference final RestconfStream.Registry streamRegistry,
            @Reference final DOMMountPointService mountPointService) {
        super(SubscribeDeviceNotification.QNAME);
        this.mountPointService = requireNonNull(mountPointService);
        this.streamRegistry = requireNonNull(streamRegistry);
    }

    @Override
    public RestconfFuture<ContainerNode> invoke(final URI restconfURI, final OperationInput input) {
        final var body = input.input();
        final var pathLeaf = body.childByArg(DEVICE_NOTIFICATION_PATH_NODEID);
        if (pathLeaf == null) {
            return RestconfFuture.failed(new RestconfDocumentedException("No path specified", ErrorType.APPLICATION,
                ErrorTag.MISSING_ELEMENT));
        }
        final var pathLeafBody = pathLeaf.body();
        if (!(pathLeafBody instanceof YangInstanceIdentifier path)) {
            return RestconfFuture.failed(new RestconfDocumentedException("Unexpected path " + pathLeafBody,
                ErrorType.APPLICATION, ErrorTag.BAD_ELEMENT));
        }
        if (!(path.getLastPathArgument() instanceof NodeIdentifierWithPredicates listId)) {
            return RestconfFuture.failed(new RestconfDocumentedException(path + " does not refer to a list item",
                ErrorType.APPLICATION, ErrorTag.BAD_ELEMENT));
        }
        if (listId.size() != 1) {
            return RestconfFuture.failed(new RestconfDocumentedException(path + " uses multiple keys",
                ErrorType.APPLICATION, ErrorTag.INVALID_VALUE));
        }

        final ApiPath apiPath;
        try {
            apiPath = new ApiPathCanonizer(input.path().databind()).dataToApiPath(path);
        } catch (ServerException e) {
            return RestconfFuture.failed(e.toLegacy());
        }

        return streamRegistry.createStream(restconfURI, new DeviceNotificationSource(mountPointService, path),
            "All YANG notifications occuring on mount point /" + apiPath.toString())
            .transform(stream -> ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(SubscribeDeviceNotificationOutput.QNAME))
                .withChild(ImmutableNodes.leafNode(DEVICE_NOTIFICATION_STREAM_NAME_NODEID, stream.name()))
                .build());
    }
}

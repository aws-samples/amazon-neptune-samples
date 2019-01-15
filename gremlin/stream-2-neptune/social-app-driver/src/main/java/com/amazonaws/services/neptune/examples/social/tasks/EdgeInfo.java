package com.amazonaws.services.neptune.examples.social.tasks;

import java.nio.ByteBuffer;

public class EdgeInfo {
    private final String fromVertexId;
    private final String toVertexId;
    private final String creationDate;

    public EdgeInfo(String fromVertexId, String toVertexId, String creationDate) {
        this.fromVertexId = fromVertexId;
        this.toVertexId = toVertexId;
        this.creationDate = creationDate;
    }

    public ByteBuffer bytes(){
        return ByteBuffer.wrap(String.format(
                "{ \"fromId\" : \"%s\", \"toId\" : \"%s\", \"creationDate\" : \"%s\" }",
                fromVertexId,
                toVertexId,
                creationDate).getBytes());
    }

    public String partitionKey() {
        return fromVertexId;
    }
}

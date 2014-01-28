package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityObjectDetectionDelete extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;

    @Inject
    public EntityObjectDetectionDelete(
            final Graph graph,
            final EntityHelper entityHelper,
            final AuditRepository auditRepository) {
        this.graph = graph;
        this.entityHelper = entityHelper;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // TODO set visibility
        Visibility visibility = new Visibility("");
        JSONObject jsonObject = new JSONObject(getRequiredParameter(request, "objectInfo"));
        User user = getUser(request);

        // Delete just the relationship if vertex has more than one relationship otherwise delete vertex
        String graphVertexId = jsonObject.getString("graphVertexId");
        JSONObject obj = new JSONObject();
        Vertex vertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        obj.put("entityVertex", GraphUtil.toJson(vertex));

        Vertex artifactVertex = graph.getVertex(jsonObject.getString("artifactId"), user.getAuthorizations());
        ElementMutation<Vertex> artifactVertexMutation = artifactVertex.prepareMutation();
        Iterable<Edge> edges = artifactVertex.getEdges(vertex, Direction.BOTH, LabelName.ENTITY_HAS_IMAGE_RAW.toString(), user.getAuthorizations());
        if (edges.iterator().hasNext()) {
            while (edges.iterator().hasNext()) {
                graph.removeEdge(edges.iterator().next(), user.getAuthorizations());
            }
        } else {
            // TODO: replace "" when we implement commenting on ui
            auditRepository.auditEntity(AuditAction.DELETE.toString(), graphVertexId, artifactVertex.getId().toString(), jsonObject.getString("title"),
                    jsonObject.getString("_conceptType"), "", "", user);
            graph.removeVertex(graphVertexId, user.getAuthorizations());
            obj.put("remove", true);
        }

        // Delete the appropriate detected object json object from the array and reset the property
        JSONArray detectedObjects = new JSONArray(artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0).toString());
        boolean deleted = false;
        for (int i = 0; i < detectedObjects.length(); i++) {
            JSONObject detectedObject = detectedObjects.getJSONObject(i);
            if (detectedObject.has("graphVertexId") && detectedObject.get("graphVertexId").equals(jsonObject.get("graphVertexId"))) {
                detectedObjects.remove(i);
                deleted = true;
                break;
            }
        }
        if (!deleted) {
            throw new RuntimeException("Tag was not found in the list of detected objects");
        }
        artifactVertexMutation.setProperty(PropertyName.DETECTED_OBJECTS.toString(), detectedObjects.toString(), visibility);
        auditRepository.auditVertexElementMutation(artifactVertexMutation, artifactVertex, "", user);
        artifactVertex = artifactVertexMutation.save();
        graph.flush();

        JSONObject updatedArtifactVertex = entityHelper.formatUpdatedArtifactVertexProperty(artifactVertex.getId(), PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0));
        obj.put("updatedArtifactVertex", updatedArtifactVertex);

        //TODO: Overwrite old ElasticSearch index with new info

        respondWithJson(response, obj);
    }
}

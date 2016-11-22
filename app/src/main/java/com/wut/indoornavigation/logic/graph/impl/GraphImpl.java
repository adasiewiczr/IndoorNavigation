package com.wut.indoornavigation.logic.graph.impl;


import android.support.annotation.NonNull;

import com.wut.indoornavigation.logic.graph.Graph;
import com.wut.indoornavigation.logic.graph.HeuristicFunction;
import com.wut.indoornavigation.logic.graph.models.Edge;
import com.wut.indoornavigation.logic.graph.models.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GraphImpl implements Graph {
    private List<Vertex> vertices;
    private Map<Vertex, List<Edge>> edges;


    public GraphImpl(@NonNull List<Vertex> vertices) {
        this.vertices = new ArrayList<>();
        this.edges = new HashMap<>();

        int verticesCount = vertices.size();
        for(int i=0; i < verticesCount; i++){
            this.vertices.add(vertices.get(i));
        }
    }

    @Override
    public boolean addEdge(@NonNull Edge edge) {
        Vertex from = edge.getFrom();
        Vertex to = edge.getTo();

        if(vertices.contains(from) && vertices.contains(to)){
            if(!edges.containsKey(from)){
                List<Edge> outEdges = new ArrayList<>();
                outEdges.add(edge);
                edges.put(from, outEdges);

                return true;
            } else{
                List<Edge> fromOutEdges = edges.get(from);
                if(fromOutEdges.contains(edge)){
                    return false;
                }else{
                    fromOutEdges.add(edge);

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean addEdge(@NonNull Vertex start, @NonNull Vertex end, double weight) {
        Edge e = new Edge(start, end, weight);

        return addEdge(e);
    }

    @Override
    public int verticesCount() {
        return vertices.size();
    }

    @Override
    public List<Vertex> outVertices(int vertexId) {
        Vertex vertex = null;
        int verticesCount = vertices.size();
        for(int i=0; i < verticesCount; i++){
            Vertex v = vertices.get(i);
            if(v.getId() == vertexId){
                vertex = v;
                break;
            }
        }

        return outVertices(vertex);
    }

    @Override
    public List<Vertex> outVertices(Vertex vertex) {
        if(vertex != null) {
            List<Edge> outEdges = edges.get(vertex);
            List<Vertex> outVertices=  new ArrayList<>();
            if(outEdges == null || outEdges.size() == 0)
            {
                return outVertices;
            }

            for (Edge outEdge : outEdges) {
                outVertices.add(outEdge.getTo());
            }
            return outVertices;
        } else{
            return new ArrayList<>();
        }
    }

    @Override
    public List<Edge> outEdges(int vertexId) {
        Vertex vertex = null;
        int verticesCount = vertices.size();
        for(int i=0; i < verticesCount; i++){
            Vertex v = vertices.get(i);
            if(v.getId() == vertexId){
                vertex = v;
                break;
            }
        }

        if(vertex != null) {
            return edges.get(vertex);
        } else{
            return new ArrayList<>();
        }
    }

    @Override
    public List<Vertex> aStar(Vertex s, Vertex t, HeuristicFunction heuristicFunction) {
        int verticesCount = verticesCount();
        double[] distance = new double[verticesCount];
        int[] previous = new int[verticesCount];

        for (int i = 0; i < verticesCount; i++) {
            final int infinity = 100000;
            distance[i] = infinity;
            previous[i] = -1;
        }

        int sIndex = vertices.indexOf(s);

        distance[sIndex] = 0;

        List<Vertex> T = new ArrayList<>();
        for(int i=0; i < verticesCount; i++){
            T.add(vertices.get(i));
        }

        while (!T.isEmpty()) {
            Vertex u = T.get(0);
            int uIndex = vertices.indexOf(u);

            for (int i = 1; i < T.size(); i++) {
                Vertex iVertex = T.get(i);
                int iIndex = vertices.indexOf(iVertex);

                if (distance[iIndex] + heuristicFunction.Execute(iVertex, t) <= distance[uIndex] + heuristicFunction.Execute(iVertex, t)) {
                    u = T.get(i);
                    uIndex = vertices.indexOf(u);
                }
            }

            T.remove(u);
            if (u == t) {
                break;
            }

            List<Vertex> outVertices = outVertices(u);
            for (int w = 0; w < T.size(); ++w) {
                Vertex wVertex = T.get(w);
                int wIndex = vertices.indexOf(wVertex);

                if (outVertices.contains(wVertex)) {
                    double uwWeight = 0;

                    List<Edge> uOutEdges = edges.get(u);
                    for (Edge uOutEdge : uOutEdges) {
                        if(uOutEdge.getTo() == wVertex){
                            uwWeight = uOutEdge.getWeight();
                            break;
                        }
                    }

                    if (distance[wIndex] > distance[uIndex] + uwWeight){
                        distance[wIndex] = distance[uIndex] + uwWeight;
                        previous[wIndex] = uIndex;
                    }
                }
            }
        }

        int i = t.getId();

        return reproducePath(previous, i);
    }

    private List<Vertex> reproducePath(int[] previous, int targetIndex) {
        List<Vertex> result = new ArrayList<>();
        while(targetIndex != -1){
            result.add(0, vertices.get(targetIndex));
            targetIndex = previous[targetIndex];
        }

        return result;
    }

    private Vertex findVertex(int id){
        Vertex v = null;
        for (Vertex vertex : vertices) {
            if(vertex.getId() == id){
                v = vertex;
                break;
            }
        }

        return v;
    }

    @Override
    public List<Vertex> aStar(int s, int t, HeuristicFunction heuristicFunction) throws Exception {
        Vertex sVertex = findVertex(s);
        Vertex tVertex = findVertex(t);

        if(sVertex == null || tVertex == null){
            throw new Exception("One ore more vertices does not exist in graph.");
        }

        return aStar(sVertex, tVertex, heuristicFunction);
    }
}

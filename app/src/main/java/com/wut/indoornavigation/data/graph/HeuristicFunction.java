package com.wut.indoornavigation.data.graph;

import com.wut.indoornavigation.data.model.graph.Vertex;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 * Heuristic function handler class
 */
@Singleton
public class HeuristicFunction {

    @Inject
    public HeuristicFunction() {

    }

    /**
     * Calculates distance between two vertices in sense of heuristic function
     *
     * @param source      source vertex
     * @param destination target vertex
     * @return distance between vertices in sense of heuristic function
     */
    public double execute(Vertex source, Vertex destination) {
        final double xDistance = abs(source.getPosition().getX() - destination.getPosition().getX());
        final double yDistance = abs(source.getPosition().getY() - destination.getPosition().getY());
        final double zDistance = abs(source.getPosition().getZ() - destination.getPosition().getZ());

        return sqrt(pow(xDistance, 2) + pow(yDistance, 2) + pow(zDistance, 2));
    }
}

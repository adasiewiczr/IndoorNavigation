package com.wut.indoornavigation.data.mesh;

import com.wut.indoornavigation.data.graph.Graph;
import com.wut.indoornavigation.data.graph.HeuristicFunction;
import com.wut.indoornavigation.data.graph.UnionFind;
import com.wut.indoornavigation.data.graph.VertexComparator;
import com.wut.indoornavigation.data.graph.impl.GraphImpl;
import com.wut.indoornavigation.data.model.Building;
import com.wut.indoornavigation.data.model.Elevator;
import com.wut.indoornavigation.data.model.Floor;
import com.wut.indoornavigation.data.model.FloorObject;
import com.wut.indoornavigation.data.model.Point;
import com.wut.indoornavigation.data.model.Stairs;
import com.wut.indoornavigation.data.model.graph.Vertex;
import com.wut.indoornavigation.data.model.mesh.MeshResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Mesh {
    private static final int ID_SEED_INIT = -1;
    private static final double HORIZONTAL_VERTICAL_EDGE_WEIGHT = 0.5;
    private static final double DIAGONAL_EDGE_WEIGHT = 0.7;
    private static final int EDGE_ELEVATOR_WEIGHT = 5000;

    private static int idSeed;

    private Map<Integer, List<Vertex>> destinationVerticesDict;
    private Map<Integer, List<Vertex>> elevatorsVerticesDict;
    private Map<Integer, List<Vertex>> stairsVerticesDict;

    public MeshResult create(Building building) {
        idSeed = ID_SEED_INIT;
        destinationVerticesDict = new HashMap<>();
        elevatorsVerticesDict = new HashMap<>();
        stairsVerticesDict = new HashMap<>();

        HeuristicFunction heuristicFunction = new HeuristicFunction();
        UnionFind unionFind = new UnionFind();

        Graph graph = new GraphImpl(heuristicFunction, unionFind, new VertexComparator(heuristicFunction));

        for (Floor floor : building.getFloors()) {
            int floorNumber = floor.getNumber();
            FloorObject[][] enumMap = floor.getEnumMap();

            int x = 0;
            int y = 0;
            int width = enumMap.length;
            int height = enumMap[0].length;
            boolean[][] visited = new boolean[width][height];
            boolean[][] processedNeighbours = new boolean[width][height];

            boolean breakFlag = false;
            for (int i = 0; i < width; i++) {
                if(breakFlag){
                    break;
                }
                for (int j = 0; j < height; j++) {
                    visited[i][j] = true;
                    if (enumMap[i][j] == FloorObject.CORNER) {
                        x = i + 1;
                        y = j + 1;
                        if (x == enumMap.length || y == enumMap[0].length) {
                            return new MeshResult(graph, destinationVerticesDict);
                        }
                        breakFlag = true;
                        break;
                    }
                }
            }

            Vertex vertex = processCell(x, y, enumMap, floorNumber, visited, graph);
            processNeighbours(vertex, x, y, enumMap, floorNumber, visited,processedNeighbours, graph);
        }

        for (Floor floor : building.getFloors()) {
            Comparator<Vertex> by2dPosition = (v1, v2) -> {
                if (v2.getPosition().getY() - v1.getPosition().getY() == 0) {
                    return Math.round(v2.getPosition().getX() - v1.getPosition().getX());
                } else return Math.round(v2.getPosition().getY() - v1.getPosition().getY());
            };
            List<Vertex> floorDestinationVertices = destinationVerticesDict.get(floor.getNumber());
            List<Vertex> floorStairsVertices = stairsVerticesDict.get(floor.getNumber());
            List<Vertex> floorElevatorsVertices = elevatorsVerticesDict.get(floor.getNumber());

            if(floorDestinationVertices!=null){
                Collections.sort(floorDestinationVertices, by2dPosition);
            }
            if(floorStairsVertices!=null){
                Collections.sort(floorStairsVertices, by2dPosition);
            }
            if(floorElevatorsVertices!=null){
                Collections.sort(floorElevatorsVertices, by2dPosition);
            }
            if(floorDestinationVertices!=null){
                for (int i = 0; i < floorDestinationVertices.size(); i++) {
                    Vertex iFloorDestinationVertex = floorDestinationVertices.get(i);
                    iFloorDestinationVertex.setId(floor.getDoors().get(i).getId());
                }
            }
        }

        for (Floor floor : building.getFloors()) {
            int floorNumber = floor.getNumber();
            linkStairsOnFloor(building, graph, floor, floorNumber);
            linkElevatorsOnFloor(building, graph, floor, floorNumber);
        }

        unionFind.initialize(graph.verticesCount());

        return new MeshResult(graph, destinationVerticesDict);
    }

    private void linkElevatorsOnFloor(Building building, Graph graph, Floor floor, int floorNumber) {
        if(elevatorsVerticesDict.size()==0){
            return;
        }

        for (int i = 0; i < elevatorsVerticesDict.get(floorNumber).size(); i++) {
            Elevator elevator = floor.getElevators().get(i);
            if (elevator.getStart() != elevator.getEnd()) {
                for (int k = floorNumber - 1; k < floorNumber + 2; k += 2) {
                    if (building.getFloors().size() < k + 1 || elevatorsVerticesDict.size() < k + 1) {
                        continue;
                    }
                    Floor kFloor = null;
                    for (Floor f : building.getFloors()) {
                        if(f.getNumber() == k){
                            kFloor = f;
                            break;
                        }
                    }

                    if(kFloor==null){
                        continue;
                    }

                    List<Elevator> endFloorElevators = kFloor.getElevators();
                    List<Vertex> endFloorElevatorsGraphVertices = elevatorsVerticesDict.get(k);
                    int endVertexIndex = -1;
                    for (int j = 0; j < endFloorElevators.size(); j++) {
                        if (endFloorElevators.get(j).getId() == elevator.getId()) {
                            endVertexIndex = j;
                            break;
                        }
                    }

                    if (endVertexIndex == -1) {
                        throw new IllegalStateException("This algorithm is bugged as f*ck.");
                    }

                    Vertex startVertex = elevatorsVerticesDict.get(floorNumber).get(i);
                    Vertex endVertex = endFloorElevatorsGraphVertices.get(endVertexIndex);
                    if(startVertex.getId()!=endVertex.getId()){
                        if(!graph.containsEdge(startVertex.getId(), endVertex.getId())){
                            graph.addEdge(startVertex, endVertex, EDGE_ELEVATOR_WEIGHT);
                        }

                        if(!graph.containsEdge(endVertex.getId(), startVertex.getId())){
                            graph.addEdge(endVertex, startVertex, EDGE_ELEVATOR_WEIGHT);
                        }
                    }

                    break;
                }
            }
        }
    }

    private void linkStairsOnFloor(Building building, Graph graph, Floor floor, int floorNumber) {
        if(stairsVerticesDict.size()==0){
            return;
        }

        for (int i = 0; i < stairsVerticesDict.get(floorNumber).size(); i++) {
            Stairs stairs = floor.getStairs().get(i);
            if (stairs.getEndfloor() != floorNumber) {
                int k = stairs.getEndfloor();
                    if (building.getFloors().size() < k + 1 || stairsVerticesDict.size() < k + 1) {
                        continue;
                    }
                    Floor kFloor = null;
                    for (Floor f : building.getFloors()) {
                        if(f.getNumber() == k){
                            kFloor = f;
                            break;
                        }
                    }

                    if(kFloor==null){
                        continue;
                    }

                    List<Stairs> endFloorStairs = kFloor.getStairs();
                    List<Vertex> endFloorStairsGraphVertices = stairsVerticesDict.get(k);
                    int endVertexIndex = -1;
                    for (int j = 0; j < endFloorStairs.size(); j++) {
                        if (endFloorStairs.get(j).getId() == stairs.getId()) {
                            endVertexIndex = j;
                            break;
                        }
                    }

                    if (endVertexIndex == -1) {
                        throw new IllegalStateException("This algorithm is bugged as f*ck.");
                    }

                    Vertex startVertex = stairsVerticesDict.get(floorNumber).get(i);
                    Vertex endVertex = endFloorStairsGraphVertices.get(endVertexIndex);
                    if(startVertex.getId()!=endVertex.getId()){
                        if(!graph.containsEdge(startVertex.getId(), endVertex.getId())){
                            graph.addEdge(startVertex, endVertex, EDGE_ELEVATOR_WEIGHT);
                        }
                    }
            }
        }
    }

    private Vertex processCell(int x, int y, FloorObject[][] enumMap, int floorNumber, boolean[][] visited, Graph graph) {
        if (visited[x][y]) {
            return graph.getVertexByCoordinates((float)x / 2, (float)y / 2, floorNumber);
        }

        visited[x][y] = true;
        if (enumMap[x][y] == FloorObject.SPACE || enumMap[x][y] == FloorObject.DOOR || enumMap[x][y] == FloorObject.ROOM || enumMap[x][y] == FloorObject.STAIRS || enumMap[x][y] == FloorObject.ELEVATOR) {
            Point coordinates = new Point((float)x / 2, (float)y / 2, floorNumber);
            Vertex vertex = new Vertex(idSeed--, coordinates);
            graph.addVertex(vertex);

            List<Vertex> vertices = null;
            if (enumMap[x][y] == FloorObject.ROOM) {
                if (destinationVerticesDict.containsKey(floorNumber)) {
                    vertices = destinationVerticesDict.get(floorNumber);
                } else {
                    vertices = new ArrayList<>();
                    destinationVerticesDict.put(floorNumber, vertices);
                }
            }
            if (enumMap[x][y] == FloorObject.ELEVATOR) {
                if (elevatorsVerticesDict.containsKey(floorNumber)) {
                    vertices = elevatorsVerticesDict.get(floorNumber);
                } else {
                    vertices = new ArrayList<>();
                    elevatorsVerticesDict.put(floorNumber, vertices);
                }
            }
            if (enumMap[x][y] == FloorObject.STAIRS) {
                if (stairsVerticesDict.containsKey(floorNumber)) {
                    vertices = stairsVerticesDict.get(floorNumber);
                } else {
                    vertices = new ArrayList<>();
                    stairsVerticesDict.put(floorNumber, vertices);
                }
            }

            if (vertices != null) {
                vertices.add(vertex);
            }

            return vertex;
        }

        return null;
    }

    private void processNeighbours(Vertex vertex, int x, int y, FloorObject[][] enumMap, int floorNumber, boolean[][] visited, boolean[][] processedNeighbours, Graph graph) {
        final int width = enumMap.length;
        final int height = enumMap[0].length;

        int startPosX = (x - 1 < 0) ? x : x - 1;
        int startPosY = (y - 1 < 0) ? y : y - 1;
        int endPosX = (x + 1 > width - 1) ? x : x + 1;
        int endPosY = (y + 1 > height - 1) ? y : y + 1;

        for (int rowNum = startPosX; rowNum <= endPosX; rowNum++) {
            for (int colNum = startPosY; colNum <= endPosY; colNum++) {
                Vertex v = processCell(rowNum, colNum, enumMap, floorNumber, visited, graph);

                if (v != null) {
                    double weight;
                    if ((rowNum < x && colNum < y) || (rowNum > x && colNum < y) || (rowNum < x && colNum > y) || (rowNum > x && colNum > y)) {
                        weight = DIAGONAL_EDGE_WEIGHT;
                    } else {
                        weight = HORIZONTAL_VERTICAL_EDGE_WEIGHT;
                    }

                    if(v.getId()!= vertex.getId() && v.getPosition().getZ()==vertex.getPosition().getZ()){
                        graph.addEdge(v, vertex, weight);
                        graph.addEdge(vertex, v, weight);
                    }
                    if(v==vertex)
                    {
                        continue;
                    }

                    if(!processedNeighbours[rowNum][colNum]){//shouldNeighboursProcessingStart(v, rowNum, colNum, enumMap, floorNumber, visited, graph) &&
                        processedNeighbours[rowNum][colNum] = true;
                        processNeighbours(v, rowNum, colNum, enumMap, floorNumber, visited, processedNeighbours, graph);
                    }
                }
            }
        }
    }

    private boolean shouldNeighboursProcessingStart(Vertex v, int x, int y, FloorObject[][] enumMap, int floorNumber, boolean[][] visited, Graph graph) {
        final int width = enumMap.length;
        final int height = enumMap[0].length;

        int startPosX = (x - 1 < 0) ? x : x - 1;
        int startPosY = (y - 1 < 0) ? y : y - 1;
        int endPosX = (x + 1 > width - 1) ? x : x + 1;
        int endPosY = (y + 1 > height - 1) ? y : y + 1;

        for (int rowNum = startPosX; rowNum <= endPosX; rowNum++) {
            for (int colNum = startPosY; colNum <= endPosY; colNum++) {
                FloorObject sign = enumMap[rowNum][colNum];

                Vertex neighbour = graph.getVertexByCoordinates((float)rowNum/2, (float)colNum/2, floorNumber);
                if(sign == FloorObject.SPACE || sign == FloorObject.DOOR || enumMap[x][y] == FloorObject.ROOM || sign ==FloorObject.ELEVATOR || sign == FloorObject.STAIRS){
                    if(neighbour == null){
                        return true;//continue;
                    }
                    if(!graph.containsEdge(v.getId(), neighbour.getId()) && v.getId()!=neighbour.getId()){
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

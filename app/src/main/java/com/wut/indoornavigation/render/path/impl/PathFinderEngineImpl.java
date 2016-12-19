package com.wut.indoornavigation.render.path.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import com.wut.indoornavigation.R;
import com.wut.indoornavigation.data.graph.PathFinder;
import com.wut.indoornavigation.data.mesh.MeshProvider;
import com.wut.indoornavigation.data.model.Building;
import com.wut.indoornavigation.data.model.Floor;
import com.wut.indoornavigation.data.model.FloorObject;
import com.wut.indoornavigation.data.model.Point;
import com.wut.indoornavigation.data.model.Room;
import com.wut.indoornavigation.data.model.graph.Vertex;
import com.wut.indoornavigation.data.model.mesh.MeshResult;
import com.wut.indoornavigation.data.storage.BuildingStorage;
import com.wut.indoornavigation.render.RenderEngine;
import com.wut.indoornavigation.render.map.MapEngine;
import com.wut.indoornavigation.render.path.PathFinderEngine;
import com.wut.indoornavigation.render.path.PathSmoothingTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Path finder engine implementation
 */
public class PathFinderEngineImpl extends RenderEngine implements PathFinderEngine {
    private static final float STROKE_WIDTH = 10f;
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final SparseArray<Bitmap> pathBitmaps;
    private final MeshProvider meshProvider;
    private final BuildingStorage storage;
    private final PathSmoothingTool pathSmoothingTool;

    private MeshResult mesh;
    private Building building;

    public PathFinderEngineImpl(MeshProvider meshProvider, BuildingStorage storage, PathSmoothingTool pathSmoothingTool) {
        this.meshProvider = meshProvider;
        this.storage = storage;
        this.pathSmoothingTool = pathSmoothingTool;
        pathBitmaps = new SparseArray<>();
    }

    private void init(Context context) {
        building = storage.getBuilding();
        pathPaint.setColor(ContextCompat.getColor(context, R.color.pathColor));

        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(STROKE_WIDTH);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);

        pathPaint.setPathEffect(new CornerPathEffect( 360.0f));//100));
        pathPaint.setAntiAlias(true);

        getMapHeight(context);
        getMapWidth(context);
    }

    private void splitPathDueToFloorNumber(List<Point> points, Map<Integer, List<Point>> paths) {
        for (final Point point : points) {
            int floorNumber = (int) point.getZ();
            List<Point> bufor;
            if (paths.containsKey(floorNumber)) {
                bufor = paths.get(floorNumber);
            } else {
                bufor = new ArrayList<>();
                paths.put(floorNumber, bufor);
            }

            bufor.add(point);
        }
    }

    /**
     * Computes path between point and destination point on map
     *
     * @param source                 source point
     * @param destinationFloorNumber destination floor number
     * @param destinationVertexIndex destination vertex index in vertices list
     * @return list of scaled points (path)
     */
    private List<Point> computePath(Point source, int destinationFloorNumber, int destinationVertexIndex, int stepWidth, int stepHeight) {
        PathFinder pathFinder = mesh.getGraph();
        //TODO: provide source and use it
        Vertex start = mesh.getDestinationPoints().get(0).get(1);
        Vertex end = mesh.getDestinationPoints().get(destinationFloorNumber).get(destinationVertexIndex);

        List<Vertex> vertexPath = pathFinder.aStar(start, end);

        List<Point> result = new ArrayList<>(vertexPath.size());

        for (Vertex vertex : vertexPath) {
            Point position = vertex.getPosition();
            Point coordinates = calculateScaledPoint(stepWidth, stepHeight, position);
            result.add(coordinates);
        }

        return result;
    }

    @NonNull
    private Point calculateScaledPoint(int stepWidth, int stepHeight, Point position) {
        float xValue = position.getX() * 2 * stepWidth + stepWidth / 2;
        float yValue = position.getY() * 2 * stepWidth + 2 * stepHeight;

        return new Point(xValue, yValue, position.getZ());
    }

    @Override
    public void prepareMesh(Building building) {
        mesh = meshProvider.create(building);
    }

    @Override
    public void renderPath(MapEngine mapEngine, Context context, Point source, int destinationFloorNumber, int destinationVertexIndex) {
        init(context);

        final FloorObject[][] map = building.getFloors().get(0).getEnumMap();
        final int stepWidth = calculateStepWidth(map[0].length);
        final int stepHeight = calculateStepHeight(map.length);

        List<Point> points = computePath(source, destinationFloorNumber, destinationVertexIndex, stepWidth, stepHeight);

        Map<Integer, List<Point>> paths = new HashMap<>();
        splitPathDueToFloorNumber(points, paths);

        for (Floor floor : building.getFloors()) {
            int floorNumber = floor.getNumber();
            Path path = produceCurvedPath(paths.get(floorNumber));

            final Bitmap bitmap = mapEngine.getMapForFloor(floorNumber).copy(Bitmap.Config.ARGB_8888, true);
            final Canvas canvas = new Canvas(bitmap);

            canvas.drawPath(path, pathPaint);
            pathBitmaps.put(floorNumber, bitmap);
        }
    }

    @NonNull
    @Override
    public Bitmap getMapWithPathForFloor(int floorNumber) {
        final Bitmap bitmap = pathBitmaps.get(floorNumber);
        if (bitmap != null) {
            return bitmap;
        }
        throw new IllegalStateException("There is no map for floor: " + floorNumber);
    }

    @Override
    public int getRoomIndex(int roomNumber) {
        for (Floor floor : storage.getBuilding().getFloors()) {
            for (Room room : floor.getRooms()) {
                if (room.getNumber() == roomNumber) {
                    return floor.getRooms().indexOf(room);
                }
            }
        }

        throw new IllegalArgumentException("Room index not found for specified room number.");

    }

    @Override
    public int destinationFloorNumber(int floorIndex) {
        Floor floor = storage.getBuilding().getFloors().get(floorIndex);

        if (floor != null) {
            return floor.getNumber();
        }

        throw new IllegalArgumentException("Incorrect floor index.");
    }

    private Path produceCurvedPath(List<Point> points) {
        return pathSmoothingTool.produceSmoothPath(points);
    }
}

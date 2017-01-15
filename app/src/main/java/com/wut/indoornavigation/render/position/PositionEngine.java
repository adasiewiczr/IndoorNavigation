package com.wut.indoornavigation.render.position;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.wut.indoornavigation.R;
import com.wut.indoornavigation.data.model.Point;
import com.wut.indoornavigation.data.storage.BuildingStorage;
import com.wut.indoornavigation.render.RenderEngine;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PositionEngine extends RenderEngine {

    private final Paint userPositionPaint;
    private final BuildingStorage buildingStorage;
    private final int userPositionSize;

    @Inject
    PositionEngine(Context context, BuildingStorage buildingStorage) {
        this.buildingStorage = buildingStorage;
        userPositionPaint = new Paint();
        userPositionSize = (int) context.getResources().getDimension(R.dimen.user_position_size);
        userPositionPaint.setColor(ContextCompat.getColor(context, R.color.userPositionColor));
    }

    public Bitmap renderMapWithUserPosition(Bitmap map, Point userPosition) {
        final Bitmap bitmap = map.copy(Bitmap.Config.ARGB_8888, true);
        final Canvas canvas = new Canvas(bitmap);

        canvas.drawOval(getUserPositionOval(userPosition), userPositionPaint);
        return bitmap;
    }

    @NonNull
    private Point calculateScaledPoint(int stepWidth, int stepHeight, Point position) {
        final float xValue = position.getX() * 2 * stepWidth + stepWidth / 2;
        final float yValue = position.getY() * 2 * stepWidth + 2 * stepHeight;

        return new Point(xValue, yValue, position.getZ());
    }

    private RectF getUserPositionOval(Point userPosition) {
        final int stepWidth = calculateStepWidth(buildingStorage.getBuilding().getFloors().get(0).getEnumMap()[0].length);
        final int stepHeight = calculateStepWidth(buildingStorage.getBuilding().getFloors().get(0).getEnumMap().length);

        final Point scaledPoint = calculateScaledPoint(stepWidth, stepHeight, userPosition);

        return new RectF(scaledPoint.getX(), scaledPoint.getY(),
                scaledPoint.getX() + userPositionSize , scaledPoint.getY() + userPositionSize);
    }
}
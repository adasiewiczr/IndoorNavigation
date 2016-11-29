package com.wut.indoornavigation.di.module;

import android.content.Context;

import com.wut.indoornavigation.extensions.CanvasExtender;
import com.wut.indoornavigation.logic.graph.impl.HeuristicFunction;
import com.wut.indoornavigation.map.MapEngine;
import com.wut.indoornavigation.map.impl.MapEngineImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Application module
 */
@Module
public class ApplicationModule {

    private final Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    MapEngine provideMapEngine(CanvasExtender canvasExtender) {
        return new MapEngineImpl(context, canvasExtender);
    }

    @Singleton
    @Provides
    HeuristicFunction provideHeuristicFunction(HeuristicFunction heuristicFunction) {
        return new HeuristicFunction();
    }
}

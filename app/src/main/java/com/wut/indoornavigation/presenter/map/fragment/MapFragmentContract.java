package com.wut.indoornavigation.presenter.map.fragment;

import android.content.Context;
import android.graphics.Bitmap;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;

public interface MapFragmentContract {

    interface View extends MvpView {
        void showMap(Bitmap bitmap);
    }

    interface Presenter extends MvpPresenter<View> {


        String[] getFloorSpinnerData();

        String[] getRoomSpinnerData();

        void floorSelected(int position, int floorPosition);

        void roomSelected(Context context, int roomNumber, int floorIndex);
    }
}

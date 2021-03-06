package com.fekratoday.mashawer.screens.addtripscreen;

import android.content.Context;

import com.fekratoday.mashawer.model.beans.Trip;
import com.fekratoday.mashawer.model.database.TripDaoFirebase;
import com.fekratoday.mashawer.model.database.TripDaoSQL;

public class AddTripPresenterImpl implements AddTripContract {

    private TripDaoFirebase tripDaoFirebase;
    private TripDaoSQL tripDaoSQL;

    AddTripPresenterImpl(Context context) {
        tripDaoFirebase = new TripDaoFirebase();
        tripDaoSQL = new TripDaoSQL(context);
    }

    @Override
    public void addTripFirebase(Trip trip) {

        tripDaoFirebase.insertTrip(trip);
//        return inserted;
    }

    @Override
    public int addTripSQLite(Trip trip) {
        return tripDaoSQL.insertTrip(trip);
    }

    @Override
    public void updateTripFirebase(Trip trip) {
        tripDaoFirebase.updateTrip(trip);
    }

    @Override
    public boolean updateTripSQLite(Trip trip) {

        boolean done = tripDaoSQL.updateTrip(trip);
        return done;
    }
}

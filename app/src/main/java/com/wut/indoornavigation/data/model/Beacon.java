package com.wut.indoornavigation.data.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;

/**
 * Class representing beacon
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class Beacon extends BuildingObject {

    int major;
    int minor;
    int rssi;

    public Beacon(int id, int x, int y, int major, int minor) {
        super(id, x, y);
        this.major = major;
        this.minor = minor;
    }
}


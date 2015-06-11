package com.example.testuser.liftfloor;

/**
 * Created by denny on 6/11/15.
 */
class Avg {
    int count = 0;
    double sum = 0;

    void add(float v) {
        sum += v;
        count++;
    }

    double avg() {
        if (count < 1) {
            return 0;
        }
        return sum / count;
    }

    void reset() {
        count = 0;
        sum = 0;
    }
}

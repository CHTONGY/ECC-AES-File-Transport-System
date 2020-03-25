package com.yan.ecdsa.client;

import java.math.BigInteger;

public class Point {
    static BigInteger INFINITY = new BigInteger("9999999999999999999999999999999999999999999999999999999999999999");
    static Point POINT_AT_INFINITY = new Point(BigInteger.ZERO, INFINITY);
    private BigInteger x;
    private BigInteger y;

    public Point(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }

    public BigInteger getX() {
        return x;
    }

    public BigInteger getY() {
        return y;
    }

    @Override
    public boolean equals(Object object) {
        Point point = (Point) object;
        if (this.getY().equals(point.getY()) && this.getY().equals(INFINITY)) {
            return true;
        } else if (this.getX().equals(point.getX()) && this.getY().equals(point.getY())) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String res = "(" + x + ", " + y + ")";
        if(y.equals(INFINITY)) {
            res = "O      ";    // The infinity point O
        }
        return res;
    }

    public String toString(int base) {
        String res = "";
        res = "(" + x.toString(base) + ", " + y.toString(base) + ")";
        if(y.equals(INFINITY)) {
            res = "O      ";    // The infinity point O
        }
        return res;
    }
}

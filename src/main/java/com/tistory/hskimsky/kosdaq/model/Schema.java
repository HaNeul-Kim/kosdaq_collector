package com.tistory.hskimsky.kosdaq.model;

/**
 * Schemas for MR
 *
 * @author Haneul, Kim
 */
public class Schema {

    public enum Quote {
        code,
        date,
        start,
        high,
        low,
        finish,
        diffQuantity,
        diffRate,
        quantity
    }
}

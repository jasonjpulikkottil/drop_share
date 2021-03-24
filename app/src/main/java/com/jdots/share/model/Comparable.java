package com.jdots.share.model;

public interface Comparable
{
    boolean comparisonSupported();

    String getComparableName();

    long getComparableDate();

    long getComparableSize();
}

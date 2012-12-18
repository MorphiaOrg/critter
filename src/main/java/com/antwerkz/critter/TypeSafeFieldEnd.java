package com.antwerkz.critter;

import com.google.code.morphia.query.FieldEnd;

public class TypeSafeFieldEnd<T, V> {
  private FieldEnd<T> fieldEnd;

  public TypeSafeFieldEnd(FieldEnd<T> fieldEnd) {
    this.fieldEnd = fieldEnd;
  }

  public T exists() {
    return fieldEnd.exists();
  }

  public T doesNotExist() {
    return fieldEnd.doesNotExist();
  }

  public T greaterThan(V val) {
    return fieldEnd.greaterThan(val);
  }

  public T greaterThanOrEq(V val) {
    return fieldEnd.greaterThanOrEq(val);
  }

  public T lessThan(V val) {
    return fieldEnd.lessThan(val);
  }

  public T lessThanOrEq(V val) {
    return fieldEnd.lessThanOrEq(val);
  }

  public T equal(V val) {
    return fieldEnd.equal(val);
  }

  public T notEqual(V val) {
    return fieldEnd.notEqual(val);
  }

  public T startsWith(String prefix) {
    return fieldEnd.startsWith(prefix);
  }

  public T startsWithIgnoreCase(String prefix) {
    return fieldEnd.startsWithIgnoreCase(prefix);
  }

  public T endsWith(String suffix) {
    return fieldEnd.endsWith(suffix);
  }

  public T endsWithIgnoreCase(String suffix) {
    return fieldEnd.endsWithIgnoreCase(suffix);
  }

  public T contains(String string) {
    return fieldEnd.contains(string);
  }

  public T containsIgnoreCase(String suffix) {
    return fieldEnd.containsIgnoreCase(suffix);
  }

  public T hasThisOne(V val) {
    return fieldEnd.hasThisOne(val);
  }

  public T hasAllOf(Iterable<V> vals) {
    return fieldEnd.hasAllOf(vals);
  }

  public T hasAnyOf(Iterable<V> vals) {
    return fieldEnd.hasAnyOf(vals);
  }

  public T hasNoneOf(Iterable<V> vals) {
    return fieldEnd.hasNoneOf(vals);
  }

  public T in(Iterable<V> vals) {
    return fieldEnd.in(vals);
  }

  public T notIn(Iterable<V> vals) {
    return fieldEnd.notIn(vals);
  }

  public T hasThisElement(V val) {
    return fieldEnd.hasThisElement(val);
  }

  public T sizeEq(int val) {
    return fieldEnd.sizeEq(val);
  }

  public T near(double x, double y) {
    return fieldEnd.near(x, y);
  }

  public T near(double x, double y, boolean spherical) {
    return fieldEnd.near(x, y, spherical);
  }

  public T near(double x, double y, double radius) {
    return fieldEnd.near(x, y, radius);
  }

  public T near(double x, double y, double radius, boolean spherical) {
    return fieldEnd.near(x, y, radius, spherical);
  }

  public T within(double x, double y, double radius) {
    return fieldEnd.within(x, y, radius);
  }

  public T within(double x, double y, double radius, boolean spherical) {
    return fieldEnd.within(x, y, radius, spherical);
  }

  public T within(double x1, double y1, double x2, double y2) {
    return fieldEnd.within(x1, y1, x2, y2);
  }
}
/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter;

import com.google.code.morphia.query.FieldEnd;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.Shape;
import com.google.code.morphia.query.Shape.Point;
import static com.google.code.morphia.query.Shape.box;
import static com.google.code.morphia.query.Shape.center;
import static com.google.code.morphia.query.Shape.centerSphere;

public class TypeSafeFieldEnd<T, Q, V> {
  private T criteria;

  private final Query<Q> query;

  private FieldEnd<T> fieldEnd;

  private final String fieldName;

  @Deprecated
  public TypeSafeFieldEnd(Query<Q> query, String fieldName) {
    this.query = query;
    this.fieldName = fieldName;
    throw new RuntimeException("Old constructor");
  }

  public TypeSafeFieldEnd(T criteria, Query<Q> query, String fieldName) {
    this.criteria = criteria;
    this.query = query;
    this.fieldName = fieldName;
  }

  public Query<Q> query() {
    return query;
  }

  public T distinct() {
    query.getCollection().distinct(fieldName);
    return criteria;
  }

  public T order() {
    order(true);
    return criteria;
  }

  public T order(boolean ascending) {
    query.order((!ascending ? "-" : "") + fieldName);
    return criteria;
  }

  public T exists() {
    query.criteria(fieldName).exists();
    return criteria;
  }

  public T doesNotExist() {
    query.criteria(fieldName).doesNotExist();
    return criteria;
  }

  public T greaterThan(V val) {
    query.criteria(fieldName).greaterThan(val);
    return criteria;
  }

  public T greaterThanOrEq(V val) {
    query.criteria(fieldName).greaterThanOrEq(val);
    return criteria;
  }

  public T lessThan(V val) {
    query.criteria(fieldName).lessThan(val);
    return criteria;
  }

  public T lessThanOrEq(V val) {
    query.criteria(fieldName).lessThanOrEq(val);
    return criteria;
  }

  public T equal(V val) {
    query.criteria(fieldName).equal(val);
    return criteria;
  }

  public T notEqual(V val) {
    query.criteria(fieldName).notEqual(val);
    return criteria;
  }

  public T startsWith(String prefix) {
    query.criteria(fieldName).startsWith(prefix);
    return criteria;
  }

  public T startsWithIgnoreCase(String prefix) {
    query.criteria(fieldName).startsWithIgnoreCase(prefix);
    return criteria;
  }

  public T endsWith(String suffix) {
    query.criteria(fieldName).endsWith(suffix);
    return criteria;
  }

  public T endsWithIgnoreCase(String suffix) {
    query.criteria(fieldName).endsWithIgnoreCase(suffix);
    return criteria;
  }

  public T contains(String string) {
    query.criteria(fieldName).contains(string);
    return criteria;
  }

  public T containsIgnoreCase(String suffix) {
    query.criteria(fieldName).containsIgnoreCase(suffix);
    return criteria;
  }

  public T hasThisOne(V val) {
    query.criteria(fieldName).hasThisOne(val);
    return criteria;
  }

  public T hasAllOf(Iterable<V> vals) {
    query.criteria(fieldName).hasAllOf(vals);
    return criteria;
  }

  public T hasAnyOf(Iterable<V> vals) {
    query.criteria(fieldName).hasAnyOf(vals);
    return criteria;
  }

  public T hasNoneOf(Iterable<V> vals) {
    query.criteria(fieldName).hasNoneOf(vals);
    return criteria;
  }

  public T in(Iterable<V> vals) {
    query.criteria(fieldName).in(vals);
    return criteria;
  }

  public T notIn(Iterable<V> vals) {
    query.criteria(fieldName).notIn(vals);
    return criteria;
  }

  public T hasThisElement(V val) {
    query.criteria(fieldName).hasThisElement(val);
    return criteria;
  }

  public T sizeEq(int val) {
    query.criteria(fieldName).sizeEq(val);
    return criteria;
  }

  public T near(double x, double y) {
    query.criteria(fieldName).near(x, y);
    return criteria;
  }

  public T near(double x, double y, boolean spherical) {
    query.criteria(fieldName).near(x, y, spherical);
    return criteria;
  }

  public T near(double x, double y, double radius) {
    query.criteria(fieldName).near(x, y, radius);
    return criteria;
  }

  public T near(double x, double y, double radius, boolean spherical) {
    query.criteria(fieldName).near(x, y, radius, spherical);
    return criteria;
  }

  public T within(Shape shape) {
    query.criteria(fieldName).within(shape);
    return criteria;
  }

  @Deprecated
  public T within(double x, double y, double radius) {
    within(center(new Point(x, y), radius));
    return criteria;
  }

  @Deprecated
  public T within(double x, double y, double radius, boolean spherical) {
    within(centerSphere(new Point(x, y), radius));
    return criteria;
  }

  @Deprecated
  public T within(double x1, double y1, double x2, double y2) {
    within(box(new Point(x1, y1), new Point(x2, y2)));
    return criteria;
  }
}
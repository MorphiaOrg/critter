/*
 * Copyright (C) 2012-2017 Justin Lee <jlee@antwerkz.com>
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

import java.util.List;

import dev.morphia.query.Criteria;
import dev.morphia.query.Query;
import dev.morphia.query.Shape;
import dev.morphia.query.Shape.Point;
import static dev.morphia.query.Shape.box;
import static dev.morphia.query.Shape.center;
import static dev.morphia.query.Shape.centerSphere;

public class TypeSafeFieldEnd<T, V> {
  private T criteria;

  private final Query<?> query;

  private final String fieldName;

  public TypeSafeFieldEnd(T criteria, Query<?> query, String fieldName) {
    this.criteria = criteria;
    this.query = query;
    this.fieldName = fieldName;
  }

  public Query<?> query() {
    return query;
  }

  public List distinct() {
    return query.getCollection().distinct(fieldName);
  }

  public T order() {
    order(true);
    return criteria;
  }

  public T order(boolean ascending) {
    query.order((!ascending ? "-" : "") + fieldName);
    return criteria;
  }

  public Criteria exists() {
    return query.criteria(fieldName).exists();
  }

  public Criteria doesNotExist() {
    return query.criteria(fieldName).doesNotExist();
  }

  public Criteria greaterThan(V val) {
    return query.criteria(fieldName).greaterThan(val);
  }

  public Criteria greaterThanOrEq(V val) {
    return query.criteria(fieldName).greaterThanOrEq(val);
  }

  public Criteria lessThan(V val) {
    return query.criteria(fieldName).lessThan(val);
  }

  public Criteria lessThanOrEq(V val) {
    return query.criteria(fieldName).lessThanOrEq(val);
  }

  public Criteria equal(V val) {
    return query.criteria(fieldName).equal(val);
  }

  public Criteria notEqual(V val) {
    return query.criteria(fieldName).notEqual(val);
  }

  public Criteria startsWith(String prefix) {
    return query.criteria(fieldName).startsWith(prefix);
  }

  public Criteria startsWithIgnoreCase(String prefix) {
    return query.criteria(fieldName).startsWithIgnoreCase(prefix);
  }

  public Criteria endsWith(String suffix) {
    return query.criteria(fieldName).endsWith(suffix);
  }

  public Criteria endsWithIgnoreCase(String suffix) {
    return query.criteria(fieldName).endsWithIgnoreCase(suffix);
  }

  public Criteria contains(String string) {
    return query.criteria(fieldName).contains(string);
  }

  public Criteria containsIgnoreCase(String suffix) {
    return query.criteria(fieldName).containsIgnoreCase(suffix);
  }

  public Criteria hasThisOne(V val) {
    return query.criteria(fieldName).hasThisOne(val);
  }

  public Criteria hasAllOf(Iterable<V> vals) {
    return query.criteria(fieldName).hasAllOf(vals);
  }

  public Criteria hasAnyOf(Iterable<V> vals) {
    return query.criteria(fieldName).hasAnyOf(vals);
  }

  public Criteria hasNoneOf(Iterable<V> vals) {
    return query.criteria(fieldName).hasNoneOf(vals);
  }

  public Criteria in(Iterable<V> vals) {
    return query.criteria(fieldName).in(vals);
  }

  public Criteria notIn(Iterable<V> vals) {
    return query.criteria(fieldName).notIn(vals);
  }

  public Criteria hasThisElement(V val) {
    return query.criteria(fieldName).hasThisElement(val);
  }

  public Criteria sizeEq(int val) {
    return query.criteria(fieldName).sizeEq(val);
  }

  public Criteria near(double x, double y) {
    return query.criteria(fieldName).near(x, y);
  }

  public Criteria near(double x, double y, boolean spherical) {
    return query.criteria(fieldName).near(x, y, spherical);
  }

  public Criteria near(double x, double y, double radius) {
    return query.criteria(fieldName).near(x, y, radius);
  }

  public Criteria near(double x, double y, double radius, boolean spherical) {
    return query.criteria(fieldName).near(x, y, radius, spherical);
  }

  public Criteria within(Shape shape) {
    return query.criteria(fieldName).within(shape);
  }

  @Deprecated
  public Criteria within(double x, double y, double radius) {
    return within(center(new Point(x, y), radius));
  }

  @Deprecated
  public Criteria within(double x, double y, double radius, boolean spherical) {
    return within(centerSphere(new Point(x, y), radius));
  }

  @Deprecated
  public Criteria within(double x1, double y1, double x2, double y2) {
    return within(box(new Point(x1, y1), new Point(x2, y2)));
  }
}

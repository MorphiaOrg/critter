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

public class TypeSafeFieldEnd<T, Q, V> {

  private Query< Q> query;

  private FieldEnd<T> fieldEnd;

  public TypeSafeFieldEnd(Query< Q> query, FieldEnd<T> fieldEnd) {
    this.query = query;
    this.fieldEnd = fieldEnd;
  }

  public Query<Q> query() {
    return query;
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
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
package com.antwerkz.critter.criteria;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class BaseCriteria<T> {
    protected final Query<T> query;

    protected final Datastore ds;

    public BaseCriteria(Datastore ds, Class<T> klass) {
        query = ds.find(klass);
        this.ds = ds;
    }

    public Query<T> query() {
        return query;
    }

    public Datastore datastore() {
        return ds;
    }

    public WriteResult delete() {
        return ds.delete(query());
    }

    public WriteResult delete(WriteConcern wc) {
        return ds.delete(query(), wc);
    }

    public CriteriaContainer or(Criteria... criteria) {
        return query.or(criteria);
    }

    public CriteriaContainer and(Criteria... criteria) {
        return query.and(criteria);
    }
}

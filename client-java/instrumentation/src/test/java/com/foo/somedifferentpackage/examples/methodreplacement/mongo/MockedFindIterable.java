package com.foo.somedifferentpackage.examples.methodreplacement.mongo;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Collation;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockedFindIterable<TDocument> implements FindIterable<TDocument> {

    private final List<TDocument> elements = new ArrayList<>();

    @Override
    public FindIterable<TDocument> filter(Bson filter) {
        return null;
    }

    @Override
    public FindIterable<TDocument> limit(int limit) {
        return null;
    }

    @Override
    public FindIterable<TDocument> skip(int skip) {
        return null;
    }

    @Override
    public FindIterable<TDocument> maxTime(long maxTime, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public FindIterable<TDocument> maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public FindIterable<TDocument> modifiers(Bson modifiers) {
        return null;
    }

    @Override
    public FindIterable<TDocument> projection(Bson projection) {
        return null;
    }

    @Override
    public FindIterable<TDocument> sort(Bson sort) {
        return null;
    }

    @Override
    public FindIterable<TDocument> noCursorTimeout(boolean noCursorTimeout) {
        return null;
    }

    @Override
    public FindIterable<TDocument> oplogReplay(boolean oplogReplay) {
        return null;
    }

    @Override
    public FindIterable<TDocument> partial(boolean partial) {
        return null;
    }

    @Override
    public FindIterable<TDocument> cursorType(CursorType cursorType) {
        return null;
    }

    @Override
    public MongoCursor<TDocument> iterator() {
        return new MockedMongoCursor(elements.iterator());
    }

    @Override
    public MongoCursor<TDocument> cursor() {
        return null;
    }

    @Override
    public TDocument first() {
        return null;
    }

    @Override
    public <U> MongoIterable<U> map(Function<TDocument, U> mapper) {
        return null;
    }

    @Override
    public void forEach(Block<? super TDocument> block) {

    }

    @Override
    public <A extends Collection<? super TDocument>> A into(A target) {
        return null;
    }

    @Override
    public FindIterable<TDocument> batchSize(int batchSize) {
        return null;
    }

    @Override
    public FindIterable<TDocument> collation(Collation collation) {
        return null;
    }

    @Override
    public FindIterable<TDocument> comment(String comment) {
        return null;
    }

    @Override
    public FindIterable<TDocument> hint(Bson hint) {
        return null;
    }

    @Override
    public FindIterable<TDocument> max(Bson max) {
        return null;
    }

    @Override
    public FindIterable<TDocument> min(Bson min) {
        return null;
    }

    @Override
    public FindIterable<TDocument> maxScan(long maxScan) {
        return null;
    }

    @Override
    public FindIterable<TDocument> returnKey(boolean returnKey) {
        return null;
    }

    @Override
    public FindIterable<TDocument> showRecordId(boolean showRecordId) {
        return null;
    }

    @Override
    public FindIterable<TDocument> snapshot(boolean snapshot) {
        return null;
    }


    public void addElement(TDocument document) {
        elements.add(document);
    }

    class MockedMongoCursor implements MongoCursor<TDocument> {

        private final Iterator<TDocument> iterator;

        public MockedMongoCursor(Iterator<TDocument> it) {
            this.iterator = it;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public TDocument next() {
            return iterator.next();
        }

        @Override
        public TDocument tryNext() {
            return null;
        }

        @Override
        public ServerCursor getServerCursor() {
            return null;
        }

        @Override
        public ServerAddress getServerAddress() {
            return null;
        }
    }

}

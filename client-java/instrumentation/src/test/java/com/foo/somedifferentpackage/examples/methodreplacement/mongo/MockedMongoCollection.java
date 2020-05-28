package com.foo.somedifferentpackage.examples.methodreplacement.mongo;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;

public class MockedMongoCollection<TDocument> implements MongoCollection<TDocument> {

    private MongoNamespace namespace = null;
    private MockedFindIterable<TDocument> mockedFindIterable;

    public void setFindIterable(MockedFindIterable<TDocument> mockedFindIterable) {
        this.mockedFindIterable = mockedFindIterable;
    }

    @Override
    public MongoNamespace getNamespace() {
        return namespace;
    }

    public void setNamespace(MongoNamespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public Class<TDocument> getDocumentClass() {
        return null;
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return null;
    }

    @Override
    public ReadPreference getReadPreference() {
        return null;
    }

    @Override
    public WriteConcern getWriteConcern() {
        return null;
    }

    @Override
    public ReadConcern getReadConcern() {
        return null;
    }

    @Override
    public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
        return null;
    }

    @Override
    public MongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
        return null;
    }

    @Override
    public MongoCollection<TDocument> withReadPreference(ReadPreference readPreference) {
        return null;
    }

    @Override
    public MongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern) {
        return null;
    }

    @Override
    public MongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public long count(Bson filter) {
        return 0;
    }

    @Override
    public long count(Bson filter, CountOptions options) {
        return 0;
    }

    @Override
    public long count(ClientSession clientSession) {
        return 0;
    }

    @Override
    public long count(ClientSession clientSession, Bson filter) {
        return 0;
    }

    @Override
    public long count(ClientSession clientSession, Bson filter, CountOptions options) {
        return 0;
    }

    @Override
    public long countDocuments() {
        return 0;
    }

    @Override
    public long countDocuments(Bson filter) {
        return 0;
    }

    @Override
    public long countDocuments(Bson filter, CountOptions options) {
        return 0;
    }

    @Override
    public long countDocuments(ClientSession clientSession) {
        return 0;
    }

    @Override
    public long countDocuments(ClientSession clientSession, Bson filter) {
        return 0;
    }

    @Override
    public long countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return 0;
    }

    @Override
    public long estimatedDocumentCount() {
        return 0;
    }

    @Override
    public long estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return 0;
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public FindIterable<TDocument> find() {
        return mockedFindIterable;
    }

    @Override
    public <TResult> FindIterable<TResult> find(Class<TResult> tResultClass) {
        return (FindIterable<TResult>) mockedFindIterable;
    }

    @Override
    public FindIterable<TDocument> find(Bson filter) {
        return mockedFindIterable;
    }

    @Override
    public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> tResultClass) {
        return (FindIterable<TResult>) mockedFindIterable;
    }

    @Override
    public FindIterable<TDocument> find(ClientSession clientSession) {
        return mockedFindIterable;
    }

    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Class<TResult> tResultClass) {
        return (FindIterable<TResult>) mockedFindIterable;
    }

    @Override
    public FindIterable<TDocument> find(ClientSession clientSession, Bson filter) {
        return mockedFindIterable;
    }

    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Bson filter, Class<TResult> tResultClass) {
        return (FindIterable<TResult>) mockedFindIterable;
    }

    @Override
    public AggregateIterable<TDocument> aggregate(List<? extends Bson> pipeline) {
        return null;
    }

    @Override
    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public AggregateIterable<TDocument> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return null;
    }

    @Override
    public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public ChangeStreamIterable<TDocument> watch() {
        return null;
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(List<? extends Bson> pipeline) {
        return null;
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession) {
        return null;
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return null;
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public MapReduceIterable<TDocument> mapReduce(String mapFunction, String reduceFunction) {
        return null;
    }

    @Override
    public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public MapReduceIterable<TDocument> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
        return null;
    }

    @Override
    public <TResult> MapReduceIterable<TResult> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests) {
        return null;
    }

    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        return null;
    }

    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests) {
        return null;
    }

    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        return null;
    }

    @Override
    public void insertOne(TDocument tDocument) {

    }

    @Override
    public void insertOne(TDocument tDocument, InsertOneOptions options) {

    }

    @Override
    public void insertOne(ClientSession clientSession, TDocument tDocument) {

    }

    @Override
    public void insertOne(ClientSession clientSession, TDocument tDocument, InsertOneOptions options) {

    }

    @Override
    public void insertMany(List<? extends TDocument> tDocuments) {

    }

    @Override
    public void insertMany(List<? extends TDocument> tDocuments, InsertManyOptions options) {

    }

    @Override
    public void insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments) {

    }

    @Override
    public void insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments, InsertManyOptions options) {

    }

    @Override
    public DeleteResult deleteOne(Bson filter) {
        return null;
    }

    @Override
    public DeleteResult deleteOne(Bson filter, DeleteOptions options) {
        return null;
    }

    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter) {
        return null;
    }

    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return null;
    }

    @Override
    public DeleteResult deleteMany(Bson filter) {
        return null;
    }

    @Override
    public DeleteResult deleteMany(Bson filter, DeleteOptions options) {
        return null;
    }

    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter) {
        return null;
    }

    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(Bson filter, TDocument replacement) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(Bson filter, TDocument replacement, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(Bson filter, TDocument replacement, ReplaceOptions replaceOptions) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement, ReplaceOptions replaceOptions) {
        return null;
    }

    @Override
    public UpdateResult updateOne(Bson filter, Bson update) {
        return null;
    }

    @Override
    public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return null;
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateMany(Bson filter, Bson update) {
        return null;
    }

    @Override
    public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return null;
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return null;
    }

    @Override
    public TDocument findOneAndDelete(Bson filter) {
        return null;
    }

    @Override
    public TDocument findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter) {
        return null;
    }

    @Override
    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndReplace(Bson filter, TDocument replacement) {
        return null;
    }

    @Override
    public TDocument findOneAndReplace(Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement) {
        return null;
    }

    @Override
    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(Bson filter, Bson update) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return null;
    }

    @Override
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return null;
    }

    @Override
    public void drop() {

    }

    @Override
    public void drop(ClientSession clientSession) {

    }

    @Override
    public String createIndex(Bson keys) {
        return null;
    }

    @Override
    public String createIndex(Bson keys, IndexOptions indexOptions) {
        return null;
    }

    @Override
    public String createIndex(ClientSession clientSession, Bson keys) {
        return null;
    }

    @Override
    public String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions) {
        return null;
    }

    @Override
    public List<String> createIndexes(List<IndexModel> indexes) {
        return null;
    }

    @Override
    public List<String> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return null;
    }

    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return null;
    }

    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return null;
    }

    @Override
    public ListIndexesIterable<Document> listIndexes() {
        return null;
    }

    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public ListIndexesIterable<Document> listIndexes(ClientSession clientSession) {
        return null;
    }

    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(ClientSession clientSession, Class<TResult> tResultClass) {
        return null;
    }

    @Override
    public void dropIndex(String indexName) {

    }

    @Override
    public void dropIndex(String indexName, DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void dropIndex(Bson keys) {

    }

    @Override
    public void dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void dropIndex(ClientSession clientSession, String indexName) {

    }

    @Override
    public void dropIndex(ClientSession clientSession, Bson keys) {

    }

    @Override
    public void dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void dropIndexes() {

    }

    @Override
    public void dropIndexes(ClientSession clientSession) {

    }

    @Override
    public void dropIndexes(DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {

    }

    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace) {

    }

    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {

    }

    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {

    }

    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {

    }
}

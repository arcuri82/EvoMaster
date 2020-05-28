package org.evomaster.core.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.*
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.core.mongo.filter.ASTNodeFilter
import org.evomaster.core.mongo.filter.DocumentToASTFilterConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MongoHeuristicCalculatorTest {

    companion object {
        private fun toFilter(filter: Bson): ASTNodeFilter {
            val bsonDocument = filter.toBsonDocument(BsonDocument::class.java, MongoClient.getDefaultCodecRegistry())!!
            val document = DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
            return DocumentToASTFilterConverter().translate(document)
        }
    }

    @Test
    fun testEquality() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.eq("age", 0)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(32.0, aDistance)

        val anotherDocument = Document()
        anotherDocument.append("age", 18)

        val anotherDistance = filterDocument.accept(MongoHeuristicCalculator(), anotherDocument)
        assertEquals(18.0, anotherDistance)

    }

    @Test
    fun testMissingField() {
        val document = Document()

        val filterBson = Filters.eq("age", 0)
        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(Double.MAX_VALUE, distance)
    }

    @Test
    fun testDifferentValueTypes() {
        val document = Document()
        document.append("age", "32")

        val filterBson = Filters.eq("age", 0)
        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(Double.MAX_VALUE, distance)
    }

    @Test
    fun testAndFilter() {
        val document = Document()
        document.append("age", 32)
        document.append("numberOfChildren", 3)

        val filterBson = Filters.and(Filters.eq("age", 0),
                Filters.eq("numberOfChildren", 0))

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(35.0, distance)
    }

    @Test
    fun testOrFilter() {
        val document = Document()
        document.append("age", 32)
        document.append("numberOfChildren", 3)

        val filterBson = Filters.or(Filters.eq("age", 0),
                Filters.eq("numberOfChildren", 0))

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(3.0, distance)
    }

    @Test
    fun testLessThan() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.lt("age", 0)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(33.0, aDistance)
    }

    @Test
    fun testLessThanEquals() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.lte("age", 0)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(32.0, aDistance)
    }

    @Test
    fun testGreaterThan() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.gt("age", 40)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(9.0, aDistance)
    }

    @Test
    fun testGreaterThanEquals() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.gte("age", 40)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(8.0, aDistance)
    }

    @Test
    fun testNotEquals() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.ne("age", 32)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(1.0, aDistance)
    }

    @Test
    fun testFilterSize() {
        val aDocument = Document()
        aDocument.append("myArray", BsonArray(listOf()))

        val filterBson = Filters.size("myArray", 5)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(5.0, aDistance)
    }

    @Test
    fun testFilterSizeMissingFieldName() {
        val aDocument = Document()

        val filterBson = Filters.size("myArray", 5)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testFilterSizeMismatchTypes() {
        val aDocument = Document()
        aDocument.append("myArray", BsonInt32(32))

        val filterBson = Filters.size("myArray", 5)
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilter() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.`in`("age", listOf(10, 112, 15))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(17.0, aDistance)
    }

    @Test
    fun testInFilterEmptyList() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.`in`("age", listOf<Int>())
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilterMissingField() {
        val aDocument = Document()

        val filterBson = Filters.`in`("age", listOf<Int>())
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilterIncompatibleTypes() {
        val aDocument = Document()
        aDocument.append("age", 32)

        val filterBson = Filters.`in`("age", listOf("Hello", "World"))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilterBsonNullValue() {
        val aDocument = Document()
        aDocument.append("age", BsonNull())

        val filterBson = Filters.`in`("age", listOf(18, 21, 65))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilterNullValue() {
        val aDocument = Document()
        aDocument.append("age", null)

        val filterBson = Filters.`in`("age", listOf(18, 21, 65))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(Double.MAX_VALUE, aDistance)
    }

    @Test
    fun testInFilterNullValueAndNullElement() {
        val aDocument = Document()
        aDocument.append("age", null)

        val filterBson = Filters.`in`("age", listOf(null))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(0.0, aDistance)
    }

    @Test
    fun testAndFilterOverflow() {
        val document = Document()
        document.append("age", Double.MIN_VALUE)
        document.append("numberOfChildren", Double.MIN_VALUE)

        val filterBson = Filters.and(Filters.eq("age", Double.MAX_VALUE),
                Filters.eq("numberOfChildren", Double.MAX_VALUE))

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(Double.MAX_VALUE, distance)
    }

    @Test
    fun testExistsFilter() {
        val document = Document()
        document.append("n_me", "John")

        val filterBson = Filters.exists("name")

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(2.0, distance)
    }

    @Test
    fun testExistsFilterNotClose() {
        val document = Document()
        document.append("age", 18)

        val filterBson = Filters.exists("name")

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(65563.0, distance)
    }

    @Test
    fun testExistsFilterNoFields() {
        val document = Document()


        val filterBson = Filters.exists("name")

        val filterDocument = toFilter(filterBson)

        val distance = filterDocument.accept(MongoHeuristicCalculator(), document)

        assertEquals(Double.MAX_VALUE, distance)
    }

    @Test
    fun testInFilterString() {
        val aDocument = Document()
        aDocument.append("name", "John")

        val filterBson = Filters.`in`("name", listOf("Jack", "Jill", "Johnathan"))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(12.0, aDistance)
    }

    @Test
    fun testInFilterStringCloser() {
        val aDocument = Document()
        aDocument.append("name", "John")

        val filterBson = Filters.`in`("name", listOf("Johm", "Jill", "Jack"))
        val filterDocument = toFilter(filterBson)

        val aDistance = filterDocument.accept(MongoHeuristicCalculator(), aDocument)

        assertEquals(1.0, aDistance)
    }
}
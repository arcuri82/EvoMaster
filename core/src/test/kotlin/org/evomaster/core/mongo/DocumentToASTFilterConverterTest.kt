package org.evomaster.core.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.BsonDocument
import org.bson.BsonType
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.core.mongo.filter.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DocumentToASTFilterConverterTest {

    companion object {
        private fun toDocument(filter: Bson): Document {
            val bsonDocument = filter.toBsonDocument(BsonDocument::class.java, MongoClient.getDefaultCodecRegistry())!!
            return DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
        }
    }

    @Test
    fun convertEquals() {
        val eqFilter = Filters.eq("age", 0)
        val filterDocument = toDocument(eqFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
    }

    @Test
    fun convertGreaterThan() {
        val gtFilter = Filters.gt("age", 0)
        val filterDocument = toDocument(gtFilter)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is ComparisonFilter<*>)
        query as ComparisonFilter<Integer>
        assertEquals("age", query.fieldName)
        assertEquals(0, query.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.GREATER_THAN, query.operator)
    }

    @Test
    fun convertGreaterEqualsThan() {
        val gteFilter = Filters.gte("age", 0)
        val filterDocument = toDocument(gteFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.GREATER_THAN_EQUALS, filter.operator)

    }

    @Test
    fun convertLessThan() {
        val ltFilter = Filters.lt("age", 0)
        val filterDocument = toDocument(ltFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.LESS_THAN, filter.operator)
    }

    @Test
    fun convertLessThanEquals() {
        val lteFilter = Filters.lte("age", 0)
        val filterDocument = toDocument(lteFilter)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is ComparisonFilter<*>)
        query as ComparisonFilter<Integer>
        assertEquals("age", query.fieldName)
        assertEquals(0, query.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.LESS_THAN_EQUALS, query.operator)
    }

    @Test
    fun convertStringEquals() {
        val eqFilter = Filters.eq("name", "John")
        val filterDocument = toDocument(eqFilter)
        val parser = DocumentToASTFilterConverter()
        val filter = parser.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("name", filter.fieldName)
        assertEquals("John", filter.value)
    }

    @Test
    fun convertAnd() {
        val andFilter = Filters.and(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(andFilter)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is AndFilter)
        query as AndFilter

        val left = query.filters[0]
        val right = query.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertOr() {
        val orFilter = Filters.or(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(orFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is OrFilter)
        filter as OrFilter

        val left = filter.filters[0]
        val right = filter.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertAll() {
        val allFilter = Filters.all("colors", "Blue", "Red", "White"
        )
        val filterDocument = toDocument(allFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is AllFilter)
        filter as AllFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "Red", "White"), values)
    }

    @Test
    fun convertElemMatch() {
        val elemMatchFilter = Filters.elemMatch("statusLog", Filters.eq("status", "Submitted"))
        val filterDocument = toDocument(elemMatchFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ElemMatchFilter)
        filter as ElemMatchFilter

        val elemMatchFieldName = filter.fieldName
        assertEquals("statusLog", elemMatchFieldName)

        val elemMatchValue = filter.filter
        assertTrue(elemMatchValue is ComparisonFilter<*>)
        elemMatchValue as ComparisonFilter<*>
        assertEquals("status", elemMatchValue.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.EQUALS, elemMatchValue.operator)
        assertEquals("Submitted", elemMatchValue.value)
    }

    @Test
    fun convertSize() {
        val sizeFilter = Filters.size("entries", 10)
        val filterDocument = toDocument(sizeFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is SizeFilter)
        filter as SizeFilter

        assertEquals("entries", filter.fieldName)
        assertEquals(10, filter.size)
    }

    @Test
    fun convertNotEquals() {
        val neFilter = Filters.ne("name", "John")
        val filterDocument = toDocument(neFilter)
        val parser = DocumentToASTFilterConverter()
        val filter = parser.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("name", filter.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.NOT_EQUALS, filter.operator)
        assertEquals("John", filter.value)
    }

    @Test
    fun convertIn() {
        val inFilter = Filters.`in`("colors", "Blue", "White", "Red")
        val filterDocument = toDocument(inFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is InFilter)
        filter as InFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "White", "Red"), values)
    }

    @Test
    fun convertNotIn() {
        val ninFilter = Filters.nin("colors", "Blue", "White", "Red")
        val filterDocument = toDocument(ninFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is NotInFilter)
        filter as NotInFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "White", "Red"), values)
    }

    @Test
    fun convertNor() {
        val norFilter = Filters.nor(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(norFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is NorFilter)
        filter as NorFilter

        val left = filter.filters[0]
        val right = filter.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertExists() {
        val existsFilter = Filters.exists("age")
        val filterDocument = toDocument(existsFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ExistsFilter)
        filter as ExistsFilter

        assertEquals("age", filter.fieldName)
    }

    @Test
    fun convertRegex() {
        val regexFilter = Filters.regex("email", ".*,*", "")
        val filterDocument = toDocument(regexFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is RegexFilter)
        filter as RegexFilter

        assertEquals("email", filter.fieldName)
        assertEquals(".*,*", filter.pattern)
        assertEquals("", filter.options)

    }

    @Test
    fun convertText() {
        val searchFilter = Filters.text("search")
        val filterDocument = toDocument(searchFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is SearchFilter)
        filter as SearchFilter

        assertEquals("search", filter.search)

    }

    @Test
    fun convertWhere() {
        val whereFilter = Filters.where("javaScriptExpression")
        val filterDocument = toDocument(whereFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is WhereFilter)
        filter as WhereFilter

        assertEquals("javaScriptExpression", filter.javaScriptExpression)
    }

    @Test
    fun convertMod() {
        val whereFilter = Filters.mod("salary", 10L, 5L)
        val filterDocument = toDocument(whereFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ModFilter)
        filter as ModFilter

        assertEquals("salary", filter.fieldName)
        assertEquals(10L, filter.divisor)
        assertEquals(5L, filter.remainder)
    }

    @Test
    fun convertType() {
        val typeFilter = Filters.type("salary", BsonType.INT32)
        val filterDocument = toDocument(typeFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is TypeFilter)
        filter as TypeFilter

        assertEquals("salary", filter.fieldName)
        assertEquals(BsonType.INT32, filter.type)
    }

    @Test
    fun convertNot() {
        val typeFilter = Filters.not(Filters.lte("age", 0))
        val filterDocument = toDocument(typeFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is NotFilter)
        filter as NotFilter

        assertTrue(filter.filter is ComparisonFilter<*>)
    }

    @Test
    fun convertCompoundAnd() {
        val innerDocument = Document()
        innerDocument["\$gt"] = 18
        innerDocument["\$lt"] = 65

        val outerDocument = Document()
        outerDocument["age"] = innerDocument

        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(outerDocument)
        assertTrue(filter is AndFilter)
        filter as AndFilter

        assertTrue(filter.filters[0] is ComparisonFilter<*>)
        assertTrue(filter.filters[1] is ComparisonFilter<*>)

    }

    @Test
    fun testNotNull() {
        //{ "firstName" : { "$ne" : null}}
        val innerDocument = Document()
        innerDocument["\$ne"] = null
        val outerDocument = Document()
        outerDocument["firstName"] = innerDocument

        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(outerDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<*>

        assertEquals("firstName", filter.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.NOT_EQUALS, filter.operator)

        assertNull(filter.value)
    }

    @Test
    fun testIsNull() {
        //{ "firstName" : { } }
        val document = Document()
        document["firstName"] = Document()

        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(document)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<*>

        assertEquals("firstName", filter.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.EQUALS, filter.operator)
        assertNull(filter.value)
    }
}
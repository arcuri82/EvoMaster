package org.evomaster.core.search.gene

import org.evomaster.core.database.DbActionGeneBuilder
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.PetClinicCheckMain
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class GeneUtilsTest {

    @Test
    fun testPadding() {

        val x = 9
        val res = GeneUtils.padded(x, 2)

        assertEquals("09", res)
    }


    @Test
    fun testPaddingNegative() {

        val x = -2
        val res = GeneUtils.padded(x, 3)

        assertEquals("-02", res)

    }

    @Test
    fun testRepairDefaultDateGene() {
        val gene = DateGene("date")
        GeneUtils.repairGenes(listOf(gene))
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
    }

    @Test
    fun testRepairBrokenDateGene() {
        val gene = DateGene("date", IntegerGene("year", 1998), IntegerGene("month", 4), IntegerGene("day", 31))
        GeneUtils.repairGenes(gene.flatView())
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
        assertEquals(1998, gene.year.value)
        assertEquals(4, gene.month.value)
        assertEquals(30, gene.day.value)
    }

    @Test
    fun testRepairBrokenSqlTimestampGene() {
        val sqlTimestampGene = DbActionGeneBuilder().buildSqlTimestampGene("timestamp")
        sqlTimestampGene.date.apply {
            year.value = 1998
            month.value = 4
            day.value = 31
        }
        sqlTimestampGene.time.apply {
            hour.value = 23
            minute.value = 13
            second.value = 41
        }

        GeneUtils.repairGenes(sqlTimestampGene.flatView())
        sqlTimestampGene.apply {
            GregorianCalendar(this.date.year.value, this.date.month.value, this.date.day.value, this.time.hour.value, this.time.minute.value)
        }
        sqlTimestampGene.date.apply {
            assertEquals(1998, this.year.value)
            assertEquals(4, this.month.value)
            assertEquals(30, this.day.value)
        }
        sqlTimestampGene.time.apply {
            assertEquals(23, this.hour.value)
            assertEquals(13, this.minute.value)
            assertEquals(41, this.second.value)
        }
    }

    @Test
    fun testFlatViewWithExcludeDateGene() {
        val sqlTimestampGene = DbActionGeneBuilder().buildSqlTimestampGene("timestamp")
        sqlTimestampGene.date.apply {
            year.value = 1998
            month.value = 4
            day.value = 31
        }
        sqlTimestampGene.time.apply {
            hour.value = 23
            minute.value = 13
            second.value = 41
        }
        val excludePredicate = { gene: Gene -> (gene is DateGene) }
        sqlTimestampGene.flatView(excludePredicate).apply {
            assertFalse(contains(sqlTimestampGene.date.year))
            assertFalse(contains(sqlTimestampGene.date.month))
            assertFalse(contains(sqlTimestampGene.date.day))
            assert(contains(sqlTimestampGene.date))
            assert(contains(sqlTimestampGene.time))
            assert(contains(sqlTimestampGene.time.hour))
            assert(contains(sqlTimestampGene.time.minute))
            assert(contains(sqlTimestampGene.time.second))
        }
    }


    @Test
    fun testBooleanSelectionSimple() {

        val obj = ObjectGene("foo", listOf(StringGene("a", "hello"), IntegerGene("b", 42)))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        //without randomization, should be both on by default
        assertEquals("{a,b}", rep)
    }

    @Test
    fun testBooleanSectionSkip() {

        val obj = ObjectGene("foo", listOf(OptionalGene("a", StringGene("a", "hello")), IntegerGene("b", 42)))

        val selection = GeneUtils.getBooleanSelection(obj)

        val a = selection.fields.find { it.name == "a" } as BooleanGene
        a.value = false

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{b}", rep)
    }


    @Test
    fun testBooleanSectionTwoNestedObjects() {

        val obj = ObjectGene("Obj1", listOf(ObjectGene("Obj2", listOf(StringGene("a", "hello")))))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Obj2{a}}", rep)
    }

    @Test
    fun testBooleanSectionThreeNestedObjects() {

        val obj = ObjectGene("Obj1", listOf(ObjectGene("Obj2", listOf(ObjectGene("Obj3", listOf(StringGene("a", "hello")))))))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Obj2{Obj3{a}}}", rep)
    }


    @Test
    fun testBooleanSectionArray() {

        val array = ArrayGene("Array", ObjectGene("Obj1", listOf(StringGene("a", "hello"))))

        val selection = GeneUtils.getBooleanSelection(array)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionString() {

        val string = StringGene("a", "hello")
        val obj = ObjectGene("obj", listOf(string))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionInteger() {

        val objInteger = ObjectGene("foo", listOf(IntegerGene("a", 1)))

        val selection = GeneUtils.getBooleanSelection(objInteger)

        val a = selection.fields.find { it.name == "a" } as BooleanGene


        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionOptionalObject() {

        val objOpt = ObjectGene("foo", listOf(OptionalGene("Opti", StringGene("Opti", "hello"))))
        val selection = GeneUtils.getBooleanSelection(objOpt)

        val a = selection.fields.find { it.name == "Opti" } as BooleanGene
        //todo extra check
        a.value = true

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Opti}", rep)
    }

    @Test
    fun testBooleanSectionBoolean() {

        val objBoolean = ObjectGene("foo", listOf(BooleanGene("a")))

        val selection = GeneUtils.getBooleanSelection(objBoolean)

        val a = selection.fields.find { it.name == "a" } as BooleanGene


        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testRepaireBooleanSectionFF() {

        val objBoolean = ObjectGene("foo", listOf(BooleanGene("a", false), (BooleanGene("b", false))))

        GeneUtils.repairBooleanSelection(objBoolean)

        assertTrue(objBoolean.fields.any { it is BooleanGene && it.value == true })
    }

    @Test
    fun testRepairInPetclinic() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/QueryTypeGlobalPetsClinic.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        val pettypes = actionCluster.get("pettypes") as GraphQLAction
        assertTrue(pettypes.parameters[0] is GQReturnParam)
        assertTrue(pettypes.parameters[0].gene is ObjectGene)
        val objPetType = pettypes.parameters[0].gene as ObjectGene
        assertEquals(2, objPetType.fields.size)
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "name" })

        (objPetType.fields[0] as BooleanGene).value = false
        (objPetType.fields[1] as BooleanGene).value = false

        assertFalse(objPetType.fields.any{ it is BooleanGene && it.value})
        GeneUtils.repairBooleanSelection(objPetType)
        assertTrue(objPetType.fields.any{ it is BooleanGene && it.value})
    }


    @Test
    fun testUnionSelection(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B",ObjectGene("B", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val unionObj = ObjectGene("foo ${ObjectGene.interfaceTag}", listOf(a,b))

        val res = unionObj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{...onA{a1}...onB{b1,b2}}", res)//with out the name foo
    }

    @Test
    fun testNestedUnionSelection(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B",ObjectGene("B", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val optionalUnionObj = OptionalGene("foo ${ObjectGene.interfaceTag}",ObjectGene("foo ${ObjectGene.interfaceTag}", listOf(a,b)))

        val obj = ObjectGene("obj", listOf(optionalUnionObj))


        val res = obj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{foo{...onA{a1}...onB{b1,b2}}}", res)//with the name foo
    }



}
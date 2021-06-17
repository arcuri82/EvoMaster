package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class GraphQLActionBuilderTest {


    @Test
    fun testPetClinic() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/QueryTypeGlobalPetsClinic.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)

        assertEquals(15, actionCluster.size)

        val pettypes = actionCluster.get("pettypes") as GraphQLAction
        assertEquals(1, pettypes.parameters.size)
        assertTrue(pettypes.parameters[0] is GQReturnParam)
        assertTrue(pettypes.parameters[0].gene is ObjectGene)
        val objPetType = pettypes.parameters[0].gene as ObjectGene
        assertEquals(2, objPetType.fields.size)
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "name" })
        val gQlReturn = GQReturnParam(pettypes.parameters[0].name, pettypes.parameters[0].gene)
        val gQlInputcopy = gQlReturn.copy()
        assertEquals(gQlReturn.name, gQlInputcopy.name)
        assertEquals(gQlReturn.gene.name, gQlInputcopy.gene.name)
        /**/
        val vets = actionCluster.get("vets") as GraphQLAction
        assertEquals(1, vets.parameters.size)
        assertTrue(vets.parameters[0] is GQReturnParam)
        assertTrue(vets.parameters[0].gene is ObjectGene)
        val objVets = vets.parameters[0].gene as ObjectGene
        assertEquals(4, objVets.fields.size)
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "firstName" })
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "lastName" })
        assertTrue(objVets.fields.any { it is OptionalGene && it.name == "specialties" })

        val objSpecialty = (objVets.fields.first { it.name == "specialties" } as OptionalGene).gene as ObjectGene
        assertEquals(2, objSpecialty.fields.size)
        assertTrue(objSpecialty.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objSpecialty.fields.any { it is BooleanGene && it.name == "name" })
        /**/
        val owners = actionCluster.get("owners") as GraphQLAction
        assertEquals(3, owners.parameters.size)
        assertTrue(owners.parameters[0] is GQInputParam)
        assertTrue(owners.parameters[0].name == "filter")
        assertTrue((owners.parameters[0].gene as OptionalGene).gene is ObjectGene)
        val objOwnerFilter = (owners.parameters[0].gene as OptionalGene).gene as ObjectGene
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "firstName" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "lastName" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "address" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "city" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "telephone" })
        assertTrue(owners.parameters[1] is GQInputParam)
        assertTrue(owners.parameters[1].name == "orders")
        assertTrue(owners.parameters[2] is GQReturnParam)
        assertTrue(owners.parameters[2].gene is ObjectGene)
        /**/
        val owner = owners.parameters[2].gene as ObjectGene
        assertEquals(7, owner.fields.size)
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "firstName" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "lastName" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "address" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "city" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "telephone" })
        assertTrue(owner.fields.any { it is OptionalGene && it.name == "pets" })
        val objPet = ((owner.fields.first { it.name == "pets" }) as OptionalGene).gene as ObjectGene
        assertEquals(6, objPet.fields.size)
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "birthDate" })
        assertTrue(objPet.fields.any { it is OptionalGene && it.name == "type" })
        assertTrue(objPet.fields.any { it is OptionalGene && it.name == "visits" })
        assertTrue(objPet.fields[5] is OptionalGene)
        val objVisitConnection = (objPet.fields[5] as OptionalGene).gene as ObjectGene
        assertEquals(2, objVisitConnection.fields.size)
        assertTrue(objVisitConnection.fields[0] is BooleanGene)
        assertTrue(objVisitConnection.fields.any { it is BooleanGene && it.name == "totalCount" })
        assertTrue(objVisitConnection.fields.any { it is OptionalGene && it.name == "visits" })
        GeneUtils.repairBooleanSelection(owner); // this should not fail
        /**/
        val pet = actionCluster.get("pet") as GraphQLAction
        assertEquals(2, pet.parameters.size)
        assertTrue(pet.parameters[0] is GQInputParam)
        assertTrue(pet.parameters[0].gene is IntegerGene)
        assertTrue(pet.parameters[0].gene.name == "id")
        assertTrue(pet.parameters[1] is GQReturnParam)
        assertTrue(pet.parameters[1].gene is ObjectGene)
        val objPet2 = (pet.parameters[1].gene as ObjectGene)
        assertEquals(6, objPet2.fields.size)
        assertTrue(objPet2.fields.any { it is OptionalGene && it.name == "visits" })
        GeneUtils.repairBooleanSelection(objPet2); // this should not fail
        /**/
        val specialties = actionCluster.get("specialties") as GraphQLAction
        assertEquals(1, specialties.parameters.size)
        assertTrue(specialties.parameters[0] is GQReturnParam)
        assertTrue(specialties.parameters[0].gene is ObjectGene)

    }

    @Test
    fun aniListSchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/AniList.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)

        assertEquals(54, actionCluster.size)
        val page = actionCluster.get("Page") as GraphQLAction
        assertEquals(3, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue((page.parameters[1].gene as OptionalGene).gene is IntegerGene)

        assertTrue(page.parameters[1] is GQInputParam)
        assertTrue(page.parameters[2] is GQReturnParam)

        //primitive type that is not part of the search
        val genreCollection = actionCluster.get("GenreCollection") as GraphQLAction

        val mediaTagCollection = actionCluster.get("MediaTagCollection") as GraphQLAction
        assertTrue(mediaTagCollection.parameters[1].gene is ObjectGene)

        val objPage = page.parameters[2].gene as ObjectGene
        assertTrue(objPage.fields[0] is OptionalGene)
        val objPageInfo = (objPage.fields[0] as OptionalGene).gene as ObjectGene
        objPageInfo.fields.any({ it is BooleanGene && it.name == "Total" })
        assertTrue(objPageInfo.fields[0] is BooleanGene)
        /**/
        val media = actionCluster.get("Media") as GraphQLAction
        assertEquals(67, media.parameters.size)
        assertTrue((media.parameters[6].gene as OptionalGene).gene is EnumGene<*>)

        val objMedia = media.parameters[66].gene as ObjectGene
        assertTrue(objMedia.fields.any { it is BooleanGene && it.name == "type" })
        /**/
        val notification = actionCluster.get("Notification") as GraphQLAction
        assertEquals(4, notification.parameters.size)
        assertTrue(notification.parameters[0] is GQInputParam)
        assertTrue(notification.parameters[3] is GQReturnParam)


        assertTrue(notification.parameters[3].gene is ObjectGene)
        val unionObjectsNotificationUnion = notification.parameters[3].gene as ObjectGene
        assertEquals(14, unionObjectsNotificationUnion.fields.size)

        assertTrue(unionObjectsNotificationUnion.fields[0] is OptionalGene)
        assertTrue((unionObjectsNotificationUnion.fields[0] as OptionalGene).gene is ObjectGene)
        val objAiringNotification = (unionObjectsNotificationUnion.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(7, objAiringNotification.fields.size)
        assertTrue(objAiringNotification.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objAiringNotification.fields.any { it is OptionalGene && it.name == "media" })


        val objMediaa = (objAiringNotification.fields.first { it.name == "media" } as OptionalGene).gene as ObjectGene
        assertEquals(53, objMediaa.fields.size)
        assertTrue(objMediaa.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objMediaa.fields.any { it is BooleanGene && it.name == "modNotes" })

    }


    @Test
    fun bitquerySchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Bitquery.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(12, actionCluster.size)

        val algorand = actionCluster.get("algorand") as GraphQLAction
        assertEquals(2, algorand.parameters.size)
        assertTrue(algorand.parameters[0] is GQInputParam)
        assertTrue(algorand.parameters[1] is GQReturnParam)
        assertTrue(algorand.parameters[1].gene is ObjectGene)
        val objAlgorand = algorand.parameters[1].gene as ObjectGene
        assertEquals(7, objAlgorand.fields.size)
        assertTrue(objAlgorand.fields.any { it is OptionalGene && it.name == "address" })

    }

    @Test
    fun catalysisHubSchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/CatalysisHub.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(11, actionCluster.size)

    }

    @Test
    fun contentfulSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Contentful.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(22, actionCluster.size)

        val asset = actionCluster.get("asset") as GraphQLAction
        assertEquals(4, asset.parameters.size)
        assertTrue(asset.parameters[0] is GQInputParam)
        assertTrue(asset.parameters[1] is GQInputParam)
        assertTrue(asset.parameters[2] is GQInputParam)
        assertTrue(asset.parameters[3] is GQReturnParam)
        assertTrue(asset.parameters[0].gene is StringGene)
        assertTrue((asset.parameters[1].gene as OptionalGene).gene is BooleanGene)
        assertTrue(asset.parameters[3].gene is ObjectGene)
        /**/
        val categoryCollection = actionCluster.get("categoryCollection") as GraphQLAction
        assertEquals(7, categoryCollection.parameters.size)
        assertTrue(categoryCollection.parameters[0] is GQInputParam)
        assertTrue(categoryCollection.parameters[1] is GQInputParam)
        assertTrue(categoryCollection.parameters[2] is GQInputParam)
        assertTrue(categoryCollection.parameters[6] is GQReturnParam)
        assertTrue((categoryCollection.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue((categoryCollection.parameters[4].gene as OptionalGene).gene is ObjectGene)
        assertTrue((((categoryCollection.parameters[4].gene as OptionalGene).gene as ObjectGene).fields[6] as OptionalGene).gene is StringGene)


    }

    @Test
    fun countriesSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Countries.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(6, actionCluster.size)

        val continents = actionCluster.get("continents") as GraphQLAction
        assertEquals(2, continents.parameters.size)
        assertTrue(continents.parameters[0] is GQInputParam)
        assertTrue(continents.parameters[1] is GQReturnParam)
        assertTrue(continents.parameters[1].gene is ObjectGene)
        val objContinents = continents.parameters[1].gene as ObjectGene
        assertTrue(objContinents.fields[2] is OptionalGene)
        val objCountry = (objContinents.fields[2] as OptionalGene).gene as ObjectGene
        assertTrue(objCountry.fields[7] is OptionalGene)

    }

    @Test
    fun deutscheBahnSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/DeutscheBahn.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(7, actionCluster.size)

        val routing = actionCluster.get("routing") as GraphQLAction
        assertEquals(3, routing.parameters.size)
        assertTrue(routing.parameters[0] is GQInputParam)
        assertTrue(routing.parameters[2] is GQReturnParam)
        assertTrue(routing.parameters[2].gene is ObjectGene)

    }

    @Test
    fun digitransitHSLSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/DigitransitHSL.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(33, actionCluster.size)

        val node = actionCluster.get("node") as GraphQLAction
        assertEquals(2, node.parameters.size)
        assertTrue(node.parameters[1] is GQReturnParam)

        assertTrue(node.parameters[1].gene is ObjectGene)
        val interfaceObjectNode = node.parameters[1].gene as ObjectGene
        assertEquals(14, interfaceObjectNode.fields.size)

        assertTrue(interfaceObjectNode.fields[0] is OptionalGene)
        assertTrue((interfaceObjectNode.fields[0] as OptionalGene).gene is ObjectGene)
        val objAgency = (interfaceObjectNode.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(10, objAgency.fields.size)
        assertTrue(objAgency.fields.any { it is BooleanGene && it.name == "lang" })
        assertTrue(objAgency.fields.any { it is BooleanGene && it.name == "phone" })
    }

    @Test
    fun eHRISchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/EHRI.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(19, actionCluster.size)

    }

    @Test
    fun etMDBSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/EtMDB.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(24, actionCluster.size)

    }

    @Test
    fun everbaseSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Everbase.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(14, actionCluster.size)

    }

    @Disabled
    fun gitLabSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/GitLab.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        /*Important: They are 162 in the documentation but in the retrieved schema they are only 157
        So there is 5 mentioned in the documentation but not mentioned in the schema*/
        assertEquals(157, actionCluster.size)

    }

    @Test
    fun gitLabSchema04202021Test() {
        /*Important: This is the gitLab schema updates */
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/GitLab04022021.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(169, actionCluster.size)

    }

    @Test
    fun graphQLJobsSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/GraphQLJobs.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(15, actionCluster.size)

    }

    @Test
    fun HIVDBSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/HIVDB.json").readText()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(9, actionCluster.size)

    }

    @Test
    fun melodyRepoSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(2, actionCluster.size)

        val ppackage = actionCluster.get("package") as GraphQLAction
        assertEquals(2, ppackage.parameters.size)
        assertTrue(ppackage.parameters[0] is GQInputParam)
        assertTrue(ppackage.parameters[0].gene is StringGene)
        val objPackage = ppackage.parameters[1].gene as ObjectGene
        assertTrue(objPackage.fields.any { it is BooleanGene && it.name == "isMain" })
        assertTrue(objPackage.fields[2] is OptionalGene)
        val objVersion = (objPackage.fields[2] as OptionalGene).gene as ObjectGene
        objVersion.fields.any { it is BooleanGene && it.name == "name" }
        assertTrue(ppackage.parameters[1] is GQReturnParam)
        assertTrue(ppackage.parameters[1].gene is ObjectGene)

    }

    @Test
    fun reactFinlandSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/ReactFinland.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(12, actionCluster.size)

    }

    @Test
    fun travelgateXSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/TravelgateX.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(9, actionCluster.size)
        /**/
        val admin = actionCluster.get("admin") as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(admin.parameters[0] is GQReturnParam)
        assertTrue(admin.parameters[0].gene is ObjectGene)
        /**/
        val hotelX = actionCluster.get("hotelX") as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(hotelX.parameters[0] is GQReturnParam)
        /**/
        val logging = actionCluster.get("logging") as GraphQLAction
        assertEquals(1, logging.parameters.size)
        assertTrue(logging.parameters[0] is GQReturnParam)
        assertTrue(logging.parameters[0].gene is ObjectGene)
    }

    @Test
    fun universeSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Universe.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(87, actionCluster.size)
    }

    @Test
    fun recEgTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/recEg.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)
    }

    @Test
    fun spaceXTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/SpaceX.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(43, actionCluster.size)

        val coresUpcoming = actionCluster.get("coresUpcoming") as GraphQLAction
        assertEquals(6, coresUpcoming.parameters.size)
        assertTrue(coresUpcoming.parameters[0] is GQInputParam)
        assertTrue(coresUpcoming.parameters[1] is GQInputParam)
        assertTrue(coresUpcoming.parameters[2] is GQInputParam)
        assertTrue(coresUpcoming.parameters[5] is GQReturnParam)
        assertTrue((coresUpcoming.parameters[0].gene as OptionalGene).gene is ObjectGene)
        assertTrue(coresUpcoming.parameters[5].gene is ObjectGene)
        val objCore = coresUpcoming.parameters[5].gene as ObjectGene
        assertTrue(objCore.fields.any { it is BooleanGene && it.name == "water_landing" })

    }

    @Test
    fun bookTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/Book.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(3, actionCluster.size)
    }


    @Test
    fun interfaceEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/interfaceEg.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val interfaceObjectStore = stores.parameters[0].gene as ObjectGene
        //assertEquals(3, interfaceObjectStore.fields.size)// basic interface removed
        assertEquals(2, interfaceObjectStore.fields.size)

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlowerStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(2, objFlowerStore.fields.size)
        assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "name" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(3, objPotStore.fields.size)
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        //assertTrue(interfaceObjectStore.fields[2] is OptionalGene)
        //assertTrue((interfaceObjectStore.fields[2] as OptionalGene).gene is ObjectGene)
        //val objStore = (interfaceObjectStore.fields[2] as OptionalGene).gene as ObjectGene
        //assertEquals(2, objStore.fields.size)
        //assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        //assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Test
    fun interfaceInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/interfaceInternalEg.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore1 = stores.parameters[0].gene as ObjectGene
        assertEquals(1, objectStore1.fields.size)


        assertTrue(objectStore1.fields[0] is OptionalGene)
        assertTrue((objectStore1.fields[0] as OptionalGene).gene is ObjectGene)
        val interfaceObjectStore = (objectStore1.fields[0] as OptionalGene).gene as ObjectGene
      //  assertEquals(3, interfaceObjectStore.fields.size)
        assertEquals(2, interfaceObjectStore.fields.size)

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlowerStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(2, objFlowerStore.fields.size)
        assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "name" })


        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(3, objPotStore.fields.size)
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        //assertTrue(interfaceObjectStore.fields[2] is OptionalGene)
        //assertTrue((interfaceObjectStore.fields[2] as OptionalGene).gene is ObjectGene)
        //val objStore = (interfaceObjectStore.fields[2] as OptionalGene).gene as ObjectGene
        //assertEquals(2, objStore.fields.size)
        //assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        //assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Test
    fun unionInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalEg.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, objectStore.fields.size)
        assertTrue(objectStore.fields[0] is BooleanGene)
        assertTrue(objectStore.fields[1] is OptionalGene)
        assertTrue((objectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val unionObjBouquet = (objectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, unionObjBouquet.fields.size)
        assertTrue(unionObjBouquet.fields[0] is OptionalGene)
        assertTrue((unionObjBouquet.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlower = (unionObjBouquet.fields[0] as OptionalGene).gene as ObjectGene
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "color" })

        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })

    }

    @Test
    fun unionInternalRecEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, objectStore.fields.size)
        assertTrue(objectStore.fields[0] is BooleanGene)
        assertTrue(objectStore.fields[1] is OptionalGene)
        assertTrue((objectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val unionObjBouquet = (objectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, unionObjBouquet.fields.size)
        assertTrue(unionObjBouquet.fields[0] is OptionalGene)
        assertTrue((unionObjBouquet.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlower = (unionObjBouquet.fields[0] as OptionalGene).gene as ObjectGene
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlower.fields.any { it is OptionalGene && it.name == "color" })

        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })

    }

    @Test
    fun unionInternalRecEg2Test() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg2.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)


    }


    @Test
    fun enumInterfaceTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/enumInterface.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        assertEquals(1, actionCluster.size)

    }
}
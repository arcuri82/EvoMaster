package org.evomaster.e2etests.spring.rest.postgres.base


import com.foo.spring.rest.postgres.base.BaseController
import io.restassured.RestAssured
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class BaseManualTest : SpringRestPostgresTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            SpringRestPostgresTestBase.initKlass(BaseController())
        }
    }

    @Test
    fun testGet() {
        val url = "$baseUrlOfSut/api/basic"

        RestAssured.given().get(url)
                .then()
                .statusCode(400)


    }
}
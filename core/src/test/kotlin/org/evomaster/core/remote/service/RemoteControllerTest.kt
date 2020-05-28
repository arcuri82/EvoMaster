package org.evomaster.core.remote.service

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.internal.EMController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection

/**
 * Created by arcuri82 on 18-Oct-19.
 */
class RemoteControllerTest {

    companion object {
        const val FAKE_SWAGGER = "/swagger.json"
    }

    private class FakeRestController : EmbeddedSutController() {

        var running: Boolean = false

        override fun startSut(): String? {
            running = true
            return null
        }

        override fun isSutRunning(): Boolean {
            return running
        }

        override fun stopSut() {
            running = false
        }

        override fun getPackagePrefixesToCover(): String? {
            return null
        }

        override fun resetStateOfSUT() {}

        override fun getInfoForAuthentication(): List<AuthenticationDto>? {
            return null
        }

        override fun getConnection(): Connection? {
            return null
        }

        override fun getDatabaseDriverName(): String? {
            return null
        }

        override fun getProblemInfo(): ProblemInfo {
            return RestProblem(FAKE_SWAGGER, null)
        }

        override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
            return SutInfoDto.OutputFormat.JAVA_JUNIT_5
        }
    }

    private val driver = FakeRestController()
    private lateinit var remote: RemoteController

    @BeforeEach
    fun initClass() {
        if (driver.isSutRunning) {
            driver.stopSut()
        }
        driver.controllerPort = 0 //ephemeral
        driver.startTheControllerServer()

        remote = RemoteController("localhost", driver.controllerServerPort, false, false, false, false)
    }

    @AfterEach
    fun tearDown() {
        driver.stopSut()
        remote.close()
    }


    @Test
    fun testSutInfo() {

        val info = remote.getSutInfo()
        assertNotNull(info)
        assertEquals(FAKE_SWAGGER, info!!.restProblem.swaggerJsonUrl)
    }

    @Test
    fun testControllerInfo(){

        val info = remote.getControllerInfo()
        assertNotNull(info)
        assertEquals(FakeRestController::class.java.name, info!!.fullName)
    }

    @Test
    fun testSearchCommands(){

        remote.startANewSearch()
        remote.startSUT()

        remote.registerNewAction(ActionDto().apply { index = 0 })

        val results = remote.getTestResults()
        assertNotNull(results)
        assertEquals(0, results!!.targets.size)
    }

    @Test
    fun testTcpConnectionOne(){
        //must run with -ea

        EMController.resetConnectedClientsSoFar()
        assertTrue(EMController.getConnectedClientsSoFar().isEmpty())

        remote.startANewSearch()
        remote.startSUT()

        remote.registerNewAction(ActionDto().apply { index = 0 })

        assertEquals(1, EMController.getConnectedClientsSoFar().size)
    }

    @Test
    fun testTcpConnectionMulti(){
        //must run with -ea

        EMController.resetConnectedClientsSoFar()
        assertTrue(EMController.getConnectedClientsSoFar().isEmpty())

        remote.startANewSearch()
        remote.startSUT()

        for(i in 0..10_000) {
            remote.registerNewAction(ActionDto().apply { index = 0 })
        }
        assertEquals(1, EMController.getConnectedClientsSoFar().size)
    }
}
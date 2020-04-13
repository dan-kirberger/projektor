package projektor.testrun

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import projektor.ApplicationTestCase
import projektor.TestSuiteData
import projektor.incomingresults.randomPublicId
import projektor.server.api.TestRun
import projektor.util.ungzip
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@KtorExperimentalAPI
class GetTestRunCompressionApplicationTest : ApplicationTestCase() {
    @Test
    fun `when gzip accept header included should compress test run response`() {
        val publicId = randomPublicId()

        val testSuiteCount = 200

        withTestApplication(::createTestApplication) {
            handleRequest(HttpMethod.Get, "/run/$publicId") {
                testRunDBGenerator.createTestRun(
                        publicId,
                        (1..testSuiteCount).map {
                            TestSuiteData("reallyLongNameForTestSuite$it",
                                    listOf("testSuite${it}TestCase1", "testSuite${it}TestCase2"),
                                    listOf(),
                                    listOf()
                            )
                        }
                )

                addHeader(HttpHeaders.AcceptEncoding, "gzip")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)

                val responseBytes = response.byteContent
                assertNotNull(responseBytes)

                val uncompressedResponse = ungzip(responseBytes)

                val responseRun = objectMapper.readValue(uncompressedResponse, TestRun::class.java)
                assertNotNull(responseRun)

                expectThat(responseRun.id).isEqualTo(publicId.id)

                val testSuites = responseRun.testSuites
                assertNotNull(testSuites)
                expectThat(testSuites).hasSize(testSuiteCount)
            }
        }
    }

    @Test
    fun `when gzip accept header included but request tiny should not compress test run response`() {
        val publicId = randomPublicId()

        val testSuiteCount = 2

        withTestApplication(::createTestApplication) {
            handleRequest(HttpMethod.Get, "/run/$publicId") {
                testRunDBGenerator.createTestRun(
                        publicId,
                        (1..testSuiteCount).map {
                            TestSuiteData("shortTestSuite$it",
                                    listOf("testSuite${it}TestCase1", "testSuite${it}TestCase2"),
                                    listOf(),
                                    listOf()
                            )
                        }
                )

                addHeader(HttpHeaders.AcceptEncoding, "gzip")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)

                val responseRun = objectMapper.readValue(response.content, TestRun::class.java)
                assertNotNull(responseRun)

                expectThat(responseRun.id).isEqualTo(publicId.id)

                val testSuites = responseRun.testSuites
                assertNotNull(testSuites)
                expectThat(testSuites).hasSize(testSuiteCount)
            }
        }
    }
}

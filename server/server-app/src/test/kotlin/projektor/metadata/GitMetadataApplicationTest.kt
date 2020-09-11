package projektor.metadata

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import projektor.ApplicationTestCase
import projektor.incomingresults.randomPublicId
import projektor.server.api.metadata.TestRunGitMetadata
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@KtorExperimentalAPI
class GitMetadataApplicationTest : ApplicationTestCase() {
    @Test
    fun `should get Git metadata for test run`() {
        val publicId = randomPublicId()
        val anotherPublicId = randomPublicId()

        withTestApplication(::createTestApplication) {
            handleRequest(HttpMethod.Get, "/run/$publicId/metadata/git") {
                val testRun = testRunDBGenerator.createSimpleTestRun(publicId)
                testRunDBGenerator.addGitMetadata(testRun, "projektor/projektor", true, "main")

                val anotherTestRun = testRunDBGenerator.createSimpleTestRun(anotherPublicId)
                testRunDBGenerator.addGitMetadata(anotherTestRun, "projektor/another", true, "main")
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)

                val gitMetadata = objectMapper.readValue(response.content, TestRunGitMetadata::class.java)
                assertNotNull(gitMetadata)

                expectThat(gitMetadata) {
                    get { repoName }.isEqualTo("projektor/projektor")
                    get { orgName }.isEqualTo("projektor")
                    get { branchName }.isEqualTo("main")
                    get { isMainBranch }.isTrue()
                }
            }
        }
    }

    @Test
    fun `when no Git metadata for test run should return 204`() {
        val publicId = randomPublicId()

        withTestApplication(::createTestApplication) {
            handleRequest(HttpMethod.Get, "/run/$publicId/metadata/git") {
                testRunDBGenerator.createSimpleTestRun(publicId)
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }
}

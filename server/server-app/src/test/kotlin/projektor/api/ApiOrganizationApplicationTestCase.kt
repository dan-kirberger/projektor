package projektor.api

import io.ktor.http.*
import io.ktor.server.testing.*
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import projektor.ApplicationTestCase
import projektor.incomingresults.randomPublicId
import projektor.server.api.organization.OrganizationCurrentCoverage
import projektor.server.example.coverage.JacocoXmlLoader
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import kotlin.test.assertNotNull

class ApiOrganizationApplicationTestCase : ApplicationTestCase() {
    @Test
    fun `when three repos in org should find their coverage data`() {
        val orgName = RandomStringUtils.randomAlphabetic(12)

        val publicId1 = randomPublicId()
        val repo1 = "$orgName/repo1"
        val olderRunRepo1 = randomPublicId()
        val otherProjectRepo1 = randomPublicId()

        val publicId2 = randomPublicId()
        val repo2 = "$orgName/repo2"
        val olderRunRepo2 = randomPublicId()

        val publicId3 = randomPublicId()
        val repo3 = "$orgName/repo3"
        val olderRunRepo3 = randomPublicId()

        val anotherPublicId = randomPublicId()
        val anotherRepo = "another-org/repo"

        val noCodeCoveragePublicId = randomPublicId()
        val noCodeCoverageRepo = "$orgName/no-coverage"

        withTestApplication(::createTestApplication) {
            handleRequest(HttpMethod.Get, "/api/v1/org/$orgName/coverage/current") {

                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = olderRunRepo1,
                    coverageText = JacocoXmlLoader().serverApp(),
                    repoName = repo1,
                    projectName = "proj1"
                )
                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = publicId1,
                    coverageText = JacocoXmlLoader().serverApp(),
                    repoName = repo1,
                    projectName = "proj1"
                )
                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = otherProjectRepo1,
                    coverageText = JacocoXmlLoader().serverAppReduced(),
                    repoName = repo1,
                    projectName = "proj2"
                )

                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = olderRunRepo2,
                    coverageText = JacocoXmlLoader().jacocoXmlParser(),
                    repoName = repo2
                )
                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = publicId2,
                    coverageText = JacocoXmlLoader().jacocoXmlParser(),
                    repoName = repo2
                )

                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = olderRunRepo3,
                    coverageText = JacocoXmlLoader().junitResultsParser(),
                    repoName = repo3
                )
                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = publicId3,
                    coverageText = JacocoXmlLoader().junitResultsParser(),
                    repoName = repo3
                )

                testRunDBGenerator.createTestRunWithCoverageAndGitMetadata(
                    publicId = anotherPublicId,
                    coverageText = JacocoXmlLoader().serverAppReduced(),
                    repoName = anotherRepo
                )

                testRunDBGenerator.createSimpleTestRunInRepo(
                    publicId = noCodeCoveragePublicId,
                    repoName = noCodeCoverageRepo,
                    ci = true,
                    projectName = null
                )
            }.apply {
                expectThat(response.status()).isEqualTo(HttpStatusCode.OK)

                val organizationCoverage = objectMapper.readValue(response.content, OrganizationCurrentCoverage::class.java)
                assertNotNull(organizationCoverage)

                expectThat(organizationCoverage.repositories).hasSize(4)

                val repoNames = organizationCoverage.repositories.map { it.repo }

                val repositoryDatas1 = organizationCoverage.repositories.filter { it.repo == repo1 }
                expectThat(repositoryDatas1).hasSize(2)

                val repo1DataProj1 = repositoryDatas1.find { it.project == "proj1" }
                assertNotNull(repo1DataProj1)
                expectThat(repo1DataProj1.id).isEqualTo(publicId1.id)
                expectThat(repo1DataProj1.coveredPercentage).isEqualTo(JacocoXmlLoader.serverAppLineCoveragePercentage)

                val repo1DataProj2 = repositoryDatas1.find { it.project == "proj2" }
                assertNotNull(repo1DataProj2)
                expectThat(repo1DataProj2.id).isEqualTo(otherProjectRepo1.id)
                expectThat(repo1DataProj2.coveredPercentage).isEqualTo(JacocoXmlLoader.serverAppReducedLineCoveragePercentage)

                val repositoryData2 = organizationCoverage.repositories.find { it.repo == repo2 }
                assertNotNull(repositoryData2)

                expectThat(repositoryData2.id).isEqualTo(publicId2.id)
                expectThat(repositoryData2.coveredPercentage).isEqualTo(JacocoXmlLoader.jacocoXmlParserLineCoveragePercentage)

                val repositoryData3 = organizationCoverage.repositories.find { it.repo == repo3 }
                assertNotNull(repositoryData3)

                expectThat(repositoryData3.id).isEqualTo(publicId3.id)
                expectThat(repositoryData3.coveredPercentage).isEqualTo(JacocoXmlLoader.junitResultsParserLineCoveragePercentage)

                expectThat(repoNames).doesNotContain(noCodeCoverageRepo)
            }
        }
    }
}

package com.github.suusan2go.kotlinfillclass.inspections.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.junit.jupiter.api.TestInfo

object PluginTestUtils {
    fun createMyFixture(testInfo: TestInfo): CodeInsightTestFixture {
        val name = testInfo.testClass.get().name + "." + testInfo.testMethod.get().name
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder =
            factory.createLightFixtureBuilder(null, name)
        val fixture = fixtureBuilder.fixture

        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, createTempDirTestFixture())
        myFixture.testDataPath = getTestDataPath()
        return myFixture
    }

    private fun createTempDirTestFixture(): TempDirTestFixture {
        val policy = IdeaTestExecutionPolicy.current()
        return if (policy != null)
            policy.createTempDirTestFixture()
        else
            LightTempDirTestFixtureImpl(true)
    }

    private fun getTestDataPath(): String {
        val path = IdeaTestExecutionPolicy.getHomePathWithPolicy()
        return StringUtil.trimEnd(FileUtil.toSystemIndependentName(path), "/") + '/' +
                StringUtil.trimStart(FileUtil.toSystemIndependentName(""), "/")
    }
}
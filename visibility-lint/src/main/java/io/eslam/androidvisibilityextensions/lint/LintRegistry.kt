package io.eslam.androidvisibilityextensions.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.*
import com.google.auto.service.AutoService
import io.eslam.androidvisibilityextensions.lint.PackagePrivateUsageDetector.Companion.PackagePrivateIssue

@Suppress("UnstableApiUsage", "unused")
@AutoService(value = [IssueRegistry::class])
class LintRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val minApi: Int = 5
    override val issues: List<Issue> = listOf(PackagePrivateIssue)
    override val vendor = Vendor(
        vendorName = "JakeWharton/timber",
        identifier = "com.jakewharton.timber:timber:{version}",
        feedbackUrl = "https://github.com/JakeWharton/timber/issues",
    )
}

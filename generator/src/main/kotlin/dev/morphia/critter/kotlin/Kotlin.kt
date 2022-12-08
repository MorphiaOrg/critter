package dev.morphia.critter.kotlin

import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.glue.ktfmt.KtfmtFormatterFunc
import com.diffplug.spotless.glue.ktfmt.KtfmtStyle

class Kotlin: FormatterFactory() {
    override fun newFormatterStep(): FormatterStep {
        return FormatterStep.createNeverUpToDate("ktfmt", KtfmtFormatterFunc(KtfmtStyle.KOTLIN_LANG))
    }
}

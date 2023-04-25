package dev.morphia.critter.kotlin

import com.diffplug.spotless.FormatExceptionPolicyStrict
import com.diffplug.spotless.Formatter
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.LineEnding
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.absolute

abstract class FormatterFactory {
    fun newFormatter(): Formatter {
        return Formatter.builder()
            .encoding(Charset.forName("UTF-8"))
            .lineEndingsPolicy(LineEnding.UNIX.createPolicy())
            .exceptionPolicy(FormatExceptionPolicyStrict())
            .steps(listOf(newFormatterStep()))
            .rootDir(File(".").toPath().absolute())
            .build()
    }

    abstract fun newFormatterStep(): FormatterStep
}

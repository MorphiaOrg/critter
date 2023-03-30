import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability.NULLABLE
import dev.morphia.critter.kotlin.extensions.className
import dev.morphia.critter.kotlin.extensions.packageName
import dev.morphia.critter.kotlin.extensions.simpleName

fun KSTypeReference.nullable() = resolve().nullability == NULLABLE

fun KSTypeReference.packageName(): String {
    return resolve().declaration.packageName()
}

fun KSTypeReference.className(): String {
    return resolve().declaration.className()
}

fun KSTypeReference.simpleName(): String {
    return resolve().declaration.simpleName()
}

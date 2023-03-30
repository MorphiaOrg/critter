import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability.NULLABLE

fun KSTypeReference.nullable() = resolve().nullability == NULLABLE
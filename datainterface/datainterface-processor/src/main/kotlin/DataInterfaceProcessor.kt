import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

data class DataInterfaceImplementorParameter(val name: String, val type: KSDeclaration)
data class DataInterfaceImplementor(val declaration: KSDeclaration,
                                    val parameters: List<DataInterfaceImplementorParameter>)
{
    val parametersKey = parameters
        .sortedBy { it.name }.joinToString(separator = ",") { "${it.name}=${it.type.simpleName}" }
}

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class BuilderProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val dataInterfacesAll = resolver
            .getSymbolsWithAnnotation("com.github.assmirnov.datainterface.DataInterface")
            .filterIsInstance<KSClassDeclaration>().toList()
        val ret = dataInterfacesAll.filter{ !it.validate()}
        val dataInterfaces = dataInterfacesAll.filter { it.validate() }
        if (dataInterfaces.isEmpty()) return ret
        dataInterfaces.forEach{dataInterface ->
            val dataInterfaceImplementors = mutableListOf<DataInterfaceImplementor>()
            val files = resolver.getAllFiles().toList()
            files.forEach {
                it.declarations.forEach { d ->
                    logger.warn("Declaration $d")
                    if (d is KSClassDeclaration) {
                        d.getAllSuperTypes().forEach { s ->
                            if (s.declaration == dataInterface) {
                                if (d.primaryConstructor != null)
                                {
                                    val implementor = DataInterfaceImplementor(
                                        declaration = d,
                                        parameters = d.primaryConstructor!!.parameters.map {
                                                p -> DataInterfaceImplementorParameter(
                                            name=p.name!!.getShortName(),
                                            type = p.type.resolve().declaration
                                        )
                                        }
                                    )
                                    dataInterfaceImplementors.add(implementor)
                                }
                            }
                        }
                    }
                }
            }
            buildFile(dataInterface, dataInterfaceImplementors)
        }
        return ret
    }

    private fun buildFile(dataInterface: KSClassDeclaration,
                          implementors: List<DataInterfaceImplementor>){

        val dataInterfaceName = dataInterface.simpleName.getShortName()
        val dataInterfaceParameters = dataInterface.getAllProperties().map { it.simpleName.getShortName() }.toList()
        val implementorsMap = implementors.groupBy { it.parametersKey }
        val packageName = "com.github.assmirnov.datainterface"
        implementorsMap.values.forEachIndexed {index, sameParametersImplementors ->
            val parameters = sameParametersImplementors.first().parameters
            val uniqueParameters = parameters.filter { !(dataInterfaceParameters.contains(it.name))}
            val fileName= "${dataInterface.simpleName.asString()}Generated$index"
            val dependencies = sameParametersImplementors.map { it.declaration.containingFile!! }
            val importsImplementors = sameParametersImplementors.map { "${it.declaration.packageName.asString()}.${it.declaration.simpleName.asString()}"}
            val importsParameters = uniqueParameters.map { "${it.type.packageName.asString()}.${it.type.simpleName.asString()}"}
            val importsInterface = listOf("${dataInterface.packageName.asString()}.${dataInterface.simpleName.asString()}")
            val imports = importsParameters + importsImplementors + importsInterface
            val file = codeGenerator.createNewFile(Dependencies(true, *dependencies.toTypedArray()), packageName, fileName)
            file.appendText("package $packageName\n\n")
            imports.forEach {
                file.appendText("import $it\n")
            }
            if (uniqueParameters.isEmpty()){
                file.appendText("inline fun <reified T: $dataInterfaceName>from(i: $dataInterfaceName): T  {\n")
            } else {
                val functionParameters = uniqueParameters.joinToString(separator = ","){
                    "${it.name}: ${it.type.simpleName.asString()}"
                }
                file.appendText("inline fun <reified T: $dataInterfaceName>from(i: $dataInterfaceName, $functionParameters): T  {\n")
            }
            file.appendText("return when(T::class){\n")
            sameParametersImplementors.forEach{implementor ->
                val implementorName = implementor.declaration.simpleName.getShortName()
                val constructorParametersFromInterface = dataInterfaceParameters.joinToString(separator = ",") { "$it = i.$it" }
                val constructorParametersFromFunctionArgs = uniqueParameters.joinToString(separator = ",") { "${it.name} = ${it.name}" }
                val constructorParameters =
                    "$constructorParametersFromInterface, $constructorParametersFromFunctionArgs"
                file.appendText("$implementorName::class -> $implementorName($constructorParameters) as T\n")
            }
            file.appendText("else -> throw IllegalArgumentException(\"Cannot build \${T::class} from $dataInterfaceName with the provided parameters\")\n")
            file.appendText("}\n")
            file.appendText("}\n")
            file.close()
        }
    }
}

class DataInterfaceProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return BuilderProcessor(environment.codeGenerator, environment.logger)
    }
}

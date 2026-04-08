package me.thatonedevil.mageRPGParty.commands

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.parser.ParserDescriptor

data class CommandBootstrap(
    val commandManager: PaperCommandManager<CommandSender>,
    val annotationParser: AnnotationParser<CommandSender>,
    val loadedCount: Int
) {
    /** Registers more annotated command objects and returns how many command nodes were added. */
    fun register(vararg commandObjects: Any): Int =
        commandObjects.sumOf { annotationParser.parse(it).size }

    /** Registers a custom Cloud parser descriptor after bootstrap. */
    fun registerArgument(descriptor: ParserDescriptor<CommandSender, *>) {
        commandManager.parserRegistry().registerParser(descriptor)
    }

    /** Registers multiple custom Cloud parser descriptors after bootstrap. */
    fun registerArguments(vararg descriptors: ParserDescriptor<CommandSender, *>) {
        descriptors.forEach { registerArgument(it) }
    }
}

class CommandBootstrapScope internal constructor(
    private val manager: PaperCommandManager<CommandSender>
) {
    /** Registers a custom Cloud parser descriptor. */
    fun registerArgument(descriptor: ParserDescriptor<CommandSender, *>) {
        manager.parserRegistry().registerParser(descriptor)
    }

    /** Registers multiple custom Cloud parser descriptors. */
    fun registerArguments(vararg descriptors: ParserDescriptor<CommandSender, *>) {
        descriptors.forEach { registerArgument(it) }
    }
}

/**
 * Usage:
 * `val commands = bootstrapCommands(MyCommandObject, OtherCommandObject) { registerArgument(...) }`
 */
fun JavaPlugin.bootstrapCommands(
    vararg commandObjects: Any,
    configure: CommandBootstrapScope.() -> Unit = {}
): CommandBootstrap {
    val manager = PaperCommandManager.builder(CommandSenderMapper())
        .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
        .buildOnEnable(this)

    CommandBootstrapScope(manager).configure()

    val parser = AnnotationParser(manager, CommandSender::class.java).installCoroutineSupport()
    val loadedCount = commandObjects.sumOf { parser.parse(it).size }

    return CommandBootstrap(
        commandManager = manager,
        annotationParser = parser,
        loadedCount = loadedCount
    )
}


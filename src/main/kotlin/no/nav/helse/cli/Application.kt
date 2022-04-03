package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

internal val log = LoggerFactory.getLogger("no.nav.helse.cli.App")

private val commands = listOf<Command>(
    CurrentPartitionsCommand(),
    CurrentOffsetsCommand(),
    FlowrateCommand(),
    SetOffsetsCommand(),
    TopicFlowrateCommand(),
    ProduceCommand(),
    DeleteConsumerGroupCommand(),
    ObserveCommand(),
    ConsumersCommand(),
    MeasureCommand(),
    TraceCommand(),
    FollowCommand(),
    FollowTopicCommand(),
    CheckVersionCommand()
)

fun main(args: Array<String>) {
    app(args.toList())
}

private fun app(args: List<String>) {
    if (args.size < 2) return help()
    try {
        val config = resolveConfigFromProperties(args[0])
        val factory = ConsumerProducerFactory(config)
        val command: Command = commands.firstOrNull { it.name == args[1] } ?: return help()
        runAndVerifyCommand(factory, command, args.takeIf { it.size > 2 }?.subList(2, args.size) ?: emptyList())
    } catch (err: Exception) {
        println("Error: ${err.message}")
        err.printStackTrace(System.err)
        exitProcess(1)
    }
}

private fun runAndVerifyCommand(factory: ConsumerProducerFactory, command: Command, args: List<String>) {
    try {
        command.execute(factory, args)
        command.verify(factory)
    } catch (err: IllegalArgumentException) {
        println("Failed to execute ${command.name}: ${err.message}")
        command.usage()
        exitProcess(1)
    }
}

private fun help() {
    println("Error: Please supply path to a valid Java properties file and a command name")
    println("Example:")
    println("java -jar app.jar config.properties myCommand")
    println("Available commands are:")
    commands.forEach { println("- ${it.name}") }
    exitProcess(-1)
}

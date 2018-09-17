package io.bitcode.discord

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.security.auth.login.LoginException

class SaimBot @Throws(LoginException::class)
constructor() : Runnable {

    private val logger = LoggerFactory.getLogger(SaimBot::class.java)

    private lateinit var jda:JDA
    private lateinit var props: Properties

    override fun run() {
        props = Properties()
        val propsFile = File("bot.properties")
        if(!propsFile.exists()) {
            logger.error("THIS CANNOT WORK WITHOUT PROPERTIES")
            System.exit(1)
        }
        props.load(FileInputStream("bot.properties"))

        logger.info("Props: $props")
        //Create bot client
        jda = JDABuilder(AccountType.BOT)
                .setToken(props.getProperty("bot.token"))
                .build()

        //Wait for it to log in
        jda.awaitReady()

        //Find guild
        val guild = getOperatingGuild() ?: throw IllegalStateException("That guild is not accessible.")

        //For each member
        for ((index, member) in guild.members.withIndex()) {

            //Don't pm bots (including itself)
            if(member.user.isBot) {
                logger.warn("Member ${member.effectiveName}#${member.user.discriminator} is a bot, so skipping.")
                continue
            }
            logger.info("(${index + 1}/${guild.members.size})Sending message to ${member.effectiveName}#${member.user.discriminator}")
            try {
                //Send the message
                if(props.getProperty("dryrun", "true")!!.toBoolean()) {
                    continue
                }
                sendTemplateMessage(member)
            }catch (ex: Exception) {
                logger.error("Failed sending message to ${member.effectiveName}#${member.user.discriminator}", ex)
            }
        }

        jda.shutdown()
    }

    private fun getOperatingGuild(): Guild? {
        return jda.getGuildById(props.getProperty("guild.id").toLong())
    }

    private fun sendTemplateMessage(member: Member) {
        val user = member.user
        val privateChannel = user.openPrivateChannel().submit().get()
                ?: throw IllegalAccessException("Could not open a private channel with user ${user.name}#${user.discriminator}")
        privateChannel.sendMessage("""
            Hello ${user.name},

            You have been invited to join the new server that has been merged with this current one!

            If you don't play RuneScape, don't worry! You can get the "Never played RS" role and many level roles!

            New server link: https://discord.gg/FUVrk6Z
            Website link: https://www.rsaddicts.com
        """.trimIndent()).submit().whenComplete { _, _ ->
            privateChannel.close().submit()
        }
    }

    companion object {

        @Throws(LoginException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sb = SaimBot()
            try {
                sb.run()
            } catch (ex: Throwable) {
                sb.logger.error("Failure detected", ex)
                sb.jda.shutdown()
            }
        }
    }
}

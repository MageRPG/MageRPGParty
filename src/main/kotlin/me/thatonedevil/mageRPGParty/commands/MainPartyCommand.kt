package me.thatonedevil.mageRPGParty.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import me.thatonedevil.devilLib.commands.DevilCommand
import me.thatonedevil.devilLib.utils.Utils.noMessage
import me.thatonedevil.devilLib.utils.Utils.yesMessage
import me.thatonedevil.mageRPGParty.MageRPGParty.Companion.partyManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class MainPartyCommand : DevilCommand {

    override val name = "party"
    override val playerOnly = true

    val TEAM_SIZE = 6


    override fun arguments(builder: LiteralArgumentBuilder<CommandSourceStack>): LiteralArgumentBuilder<CommandSourceStack> {
        return builder
            .then(Commands.literal("create").executes { create(it) })
            .then(
                Commands.literal("invite")
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { invite(it) }
                    )
            )
            .then(Commands.literal("accept").executes { accept(it) })
            .then(Commands.literal("decline").executes { decline(it) })
            .then(Commands.literal("leave").executes { leave(it) })
            .then(
                Commands.literal("kick")
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { kick(it) }
                    )
            )
            .then(Commands.literal("disband").executes { disband(it) })
            .then(Commands.literal("info").executes { info(it) })
    }

    override fun execute(
        sender: CommandSender,
        ctx: CommandContext<CommandSourceStack>
    ) {
        (sender as Player).noMessage("<color:#FF5555>Usage: <color:#d45252>/party <create|invite|accept|decline|leave|kick|disband|info>")
    }

    private fun create(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player

        if (partyManager.createParty(player.uniqueId) == null) {
            player.noMessage("<color:#FF5555>You are already in a <color:#d45252>party!")
            return DevilCommand.FAILURE
        }

        player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")
        return DevilCommand.SUCCESS
    }

    private fun invite(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        val target = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
            .resolve(ctx.source)
            .firstOrNull() ?: run {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return DevilCommand.FAILURE
        }

        if (target.uniqueId == player.uniqueId) {
            player.noMessage("<color:#FF5555>You cannot invite <color:#d45252>yourself!")
            return DevilCommand.FAILURE
        }

        val party = partyManager.getParty(player.uniqueId)

        if (party == null) {
            partyManager.createParty(player.uniqueId) ?: run {
                player.noMessage("<color:#FF5555>Failed to create <color:#d45252>party!")
                return DevilCommand.FAILURE
            }
            player.yesMessage("<color:#77DD77>Party created <color:#35cd35>successfully!")

            if (!partyManager.inviteToParty(player.uniqueId, target.uniqueId)) {
                player.noMessage("<color:#FF5555>Could not invite player! They may already be in a <color:#d45252>party <color:#FF5555>or have a <color:#d45252>pending invite<color:#FF5555>.")
                return DevilCommand.FAILURE
            }

            return DevilCommand.SUCCESS
        }

        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return DevilCommand.FAILURE
        }

        if (party.size >= TEAM_SIZE) {
            player.noMessage("<color:#FF5555>Your party is <color:#d45252>full<color:#FF5555>! Maximum size is <color:#d45252>$TEAM_SIZE <color:#FF5555>players.")
            return DevilCommand.FAILURE
        }

        if (!partyManager.inviteToParty(player.uniqueId, target.uniqueId)) {
            player.noMessage("<color:#FF5555>Could not invite player! They may already be in a <color:#d45252>party <color:#FF5555>or have a <color:#d45252>pending invite<color:#FF5555>.")
            return DevilCommand.FAILURE
        }

        return DevilCommand.SUCCESS
    }

    private fun accept(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requirePendingInvite(player)) return DevilCommand.FAILURE

        if (!partyManager.acceptInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to accept invite! It may have <color:#d45252>expired<color:#FF5555>.")
            return DevilCommand.FAILURE
        }

        player.yesMessage("<color:#77DD77>Party invite accepted <color:#35cd35>successfully!")
        return DevilCommand.SUCCESS
    }

    private fun decline(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requirePendingInvite(player)) return DevilCommand.FAILURE

        if (!partyManager.declineInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to decline invite! It may have <color:#d45252>expired<color:#FF5555>.")
            return DevilCommand.FAILURE
        }

        player.yesMessage("<color:#77DD77>Party invite declined <color:#35cd35>successfully!")
        return DevilCommand.SUCCESS
    }

    private fun leave(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requireInParty(player)) return DevilCommand.FAILURE

        val party = partyManager.getParty(player.uniqueId) ?: return DevilCommand.FAILURE

        if (party.leader == player.uniqueId) {
            if (!partyManager.disbandParty(player.uniqueId)) return DevilCommand.FAILURE
            player.yesMessage("<color:#77DD77>You have disbanded the party.")
        } else {
            if (!partyManager.leaveParty(player.uniqueId)) return DevilCommand.FAILURE
            player.yesMessage("<color:#77DD77>You have left the party <color:#35cd35>successfully!")
        }

        return DevilCommand.SUCCESS
    }

    private fun kick(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requireInParty(player)) return DevilCommand.FAILURE

        val party = partyManager.getParty(player.uniqueId) ?: return DevilCommand.FAILURE

        if (party.leader != player.uniqueId) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return DevilCommand.FAILURE
        }

        if (party.size <= 2) {
            player.noMessage("<color:#FF5555>Cannot kick from a <color:#d45252>2-person party<color:#FF5555>! Use <color:#d45252>/party disband <color:#FF5555>instead.")
            return DevilCommand.FAILURE
        }

        val target = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
            .resolve(ctx.source)
            .firstOrNull() ?: run {
            player.noMessage("<color:#FF5555>Player not <color:#d45252>found!")
            return DevilCommand.FAILURE
        }

        if (target.uniqueId == player.uniqueId) {
            player.noMessage("<color:#FF5555>You cannot kick <color:#d45252>yourself<color:#FF5555>! Use <color:#d45252>/party leave <color:#FF5555>instead.")
            return DevilCommand.FAILURE
        }

        if (!party.isMember(target.uniqueId)) {
            player.noMessage("<color:#FF5555>That player is not in your party!")
            return DevilCommand.FAILURE
        }

        if (!partyManager.kickFromParty(player.uniqueId, target.uniqueId)) {
            player.noMessage("<color:#FF5555>Failed to kick player!")
            return DevilCommand.FAILURE
        }

        player.yesMessage("<color:#77DD77>Successfully kicked <color:#35cd35>${target.name} <color:#77DD77>from the party!")
        target.noMessage("<color:#FF5555>You have been kicked from the party!")
        return DevilCommand.SUCCESS
    }

    private fun disband(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requireInParty(player)) return DevilCommand.FAILURE

        if (!partyManager.disbandParty(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You are not the <color:#d45252>leader <color:#FF5555>of the party!")
            return DevilCommand.FAILURE
        }

        player.yesMessage("<color:#77DD77>Party disbanded <color:#35cd35>successfully!")
        return DevilCommand.SUCCESS
    }

    private fun info(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender as Player
        if (!requireInParty(player)) return DevilCommand.FAILURE

        player.sendMessage(partyManager.partyInfo(player.uniqueId))
        return DevilCommand.SUCCESS
    }

    private fun requireInParty(player: Player): Boolean {
        if (!partyManager.isInParty(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You are not in a party! Create one with <color:#d45252>/party create")
            return false
        }
        return true
    }

    private fun requirePendingInvite(player: Player): Boolean {
        if (!partyManager.hasPendingInvite(player.uniqueId)) {
            player.noMessage("<color:#FF5555>You don't have any pending <color:#d45252>party invites!")
            return false
        }
        return true
    }
}
package dev.lumas.events.trickortreat

import it.ajneb97.api.InteractionsAPI
import org.bukkit.Location
import org.bukkit.entity.Player

object InteractionsHook {

    private val manager get() = InteractionsAPI.getPlugin().conversationManager

    fun conversations(): List<String> = manager.conversationEntities.map { it.fileName }

    fun points(file: String): List<DialoguePoint> = manager.getConversationEntityByName(file)?.conversations.orEmpty()
        .flatMap { conversation -> conversation.dialogues.map { DialoguePoint(file, conversation.name, it.name) } }

    fun isTalking(player: Player) = InteractionsAPI.getPlayerConversation(player) != null

    fun conversationOf(player: Player): String? = InteractionsAPI.getPlayerConversation(player)?.conversationEntity?.fileName

    fun pointOf(player: Player): DialoguePoint? = runCatching {
        val conversation = InteractionsAPI.getPlayerConversation(player) ?: return null
        DialoguePoint(conversation.conversationEntity.fileName, conversation.currentConversation.name, conversation.currentDialogue.name)
    }.getOrNull()

    fun start(player: Player, file: String, location: Location): Boolean {
        val conversation = manager.getConversationEntityByName(file) ?: return false
        return manager.preStartConversation(player, conversation, location, true, 0)
    }
}

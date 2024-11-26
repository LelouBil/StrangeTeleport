package net.leloubil.strangeteleport.data

import net.leloubil.strangeteleport.Strangeteleport
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

import thedarkcolour.kotlinforforge.neoforge.forge.getValue


object Attachments {
    val REGISTRY: DeferredRegister<AttachmentType<*>> = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Strangeteleport.MOD_ID)

    val PLAYER_LINK_ATTACHMENT: AttachmentType<PlayerLinkData> by REGISTRY.register("player_link") { ->
        AttachmentType.builder { -> PlayerLinkData.EmptyLinkData }.build()
    }

    val PLAYER_SELECT_BLOCKS_ATTACHMENT: AttachmentType<PlayerSelectBlocksData> by REGISTRY.register("player_link_select_blocks") { ->
        AttachmentType.builder { -> PlayerSelectBlocksData.EmptySelectBlocksData }.build()
    }

}

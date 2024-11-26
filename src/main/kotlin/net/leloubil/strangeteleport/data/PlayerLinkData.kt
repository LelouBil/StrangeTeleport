package net.leloubil.strangeteleport.data

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player

data class PlayerLinkData(val previousBlock: BlockPos?) {
    companion object {
        val EmptyLinkData: PlayerLinkData = PlayerLinkData(null)
    }
}

data class PlayerSelectBlocksData(val linkBlockPos: BlockPos?){
    companion object {
        val EmptySelectBlocksData: PlayerSelectBlocksData = PlayerSelectBlocksData(null)
    }
}

var Player.linkData: PlayerLinkData
    get() = this.getData(Attachments.PLAYER_LINK_ATTACHMENT)
    set(value) {
        this.setData(Attachments.PLAYER_LINK_ATTACHMENT, value)
    }

var Player.linkSelectBlocksData: PlayerSelectBlocksData
    get() = this.getData(Attachments.PLAYER_SELECT_BLOCKS_ATTACHMENT)
    set(value) {
        this.setData(Attachments.PLAYER_SELECT_BLOCKS_ATTACHMENT, value)
    }

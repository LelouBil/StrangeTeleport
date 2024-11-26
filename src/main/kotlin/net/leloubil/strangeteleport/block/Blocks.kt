package net.leloubil.strangeteleport.block

import net.leloubil.strangeteleport.Strangeteleport
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.neoforged.neoforge.registries.DeferredRegister

import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object Blocks {
    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(Strangeteleport.MOD_ID)

    val GHOST_BLOCK: GhostBlock by REGISTRY.registerBlock("ghost_block", ::GhostBlock, BlockBehaviour.Properties.of()
        .dynamicShape()
        .pushReaction(PushReaction.BLOCK)
    )

    val LINK_BLOCK: LinkBlock by REGISTRY.registerBlock("link_block", ::LinkBlock, BlockBehaviour.Properties.of())
}

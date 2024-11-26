package net.leloubil.strangeteleport.blockentities

import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.block.Blocks
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType

import net.neoforged.neoforge.registries.DeferredRegister


import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object BlockEntities {
    val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Strangeteleport.MOD_ID)


    val LINK_BLOCK_ENTITY: BlockEntityType<LinkBlockEntity> by REGISTRY.register(
        "link_block_entity"
    ) { ->
        BlockEntityType(
            ::LinkBlockEntity,
            Blocks.LINK_BLOCK
        )
    }

    val GHOST_BLOCK_ENTITY: BlockEntityType<GhostBlockEntity> by REGISTRY.register(
        "ghost_block_entity"
    ) { ->
        BlockEntityType(
            ::GhostBlockEntity,
            Blocks.GHOST_BLOCK
        )
    }
}


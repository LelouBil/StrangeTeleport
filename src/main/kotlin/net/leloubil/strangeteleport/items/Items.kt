package net.leloubil.strangeteleport.items

import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.block.Blocks
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object Items {
    val REGISTRY: DeferredRegister.Items = DeferredRegister.createItems(Strangeteleport.MOD_ID)

    val LINK_BLOCK_ITEM by REGISTRY.registerSimpleBlockItem("link_block") {
        Blocks.LINK_BLOCK
    }
}

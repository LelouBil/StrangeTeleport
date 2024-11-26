package net.leloubil.strangeteleport.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

fun Level.linkBlockAt(pos: BlockPos): LinkBlockEntity? = getBlockEntity(pos, BlockEntities.LINK_BLOCK_ENTITY).orElse(null)

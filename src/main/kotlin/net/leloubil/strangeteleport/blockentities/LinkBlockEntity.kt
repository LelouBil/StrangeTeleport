package net.leloubil.strangeteleport.blockentities

import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.block.Blocks
import net.leloubil.strangeteleport.blockentities.BlockEntities.LINK_BLOCK_ENTITY
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState


data class RelativeBlockPos(val forwardbackward: Int, val updown: Int, val leftright: Int){
    companion object {
        val CODEC = BlockPos.CODEC.xmap(
            { RelativeBlockPos(it.x,it.y,it.z) },
            { BlockPos(it.forwardbackward,it.updown,it.leftright) }
        )
    }
    // west = -x
    // east = x
    // up = y
    // down = -y
    // north = z
    // south = -z
    fun makeAbsolute(other: BlockPos,direction: Direction): BlockPos {
        return when(direction){
            Direction.NORTH -> BlockPos(other.x + leftright,other.y + updown,other.z - forwardbackward)
            Direction.SOUTH -> BlockPos(other.x - leftright,other.y + updown,other.z + forwardbackward)
            Direction.EAST -> BlockPos(other.x + forwardbackward,other.y + updown,other.z + leftright)
            Direction.WEST -> BlockPos(other.x - forwardbackward,other.y + updown,other.z - leftright)
            else -> throw IllegalArgumentException("Invalid direction")
        }
    }
}

// west = -x
// east = x
// up = y
// down = -y
// north = z
// south = -z
fun BlockPos.makeRelative(other: BlockPos,direction: Direction): RelativeBlockPos {
    return when(direction){
        Direction.NORTH -> RelativeBlockPos(other.z - z,y - other.y,x - other.x)
        Direction.SOUTH -> RelativeBlockPos(z - other.z,y - other.y,other.x - x)
        Direction.EAST -> RelativeBlockPos(x - other.x,y - other.y, z - other.z)
        Direction.WEST -> RelativeBlockPos(other.x - x,y - other.y,other.z - z)
        else -> throw IllegalArgumentException("Invalid direction")
    }
}


class LinkBlockEntity(pos: BlockPos, private val state: BlockState) : BlockEntity(LINK_BLOCK_ENTITY, pos, state) {

    var linkedTo: BlockPos? = null
        set(value) {
            field = value
            setChanged()
        }
    private var blocksToReplicate: Set<RelativeBlockPos> = emptySet()
        private set(value) {
            field = value
            setChanged()
        }

    public fun containsBlockToReplicate(pos: BlockPos): Boolean {
        return blocksToReplicate.contains(pos.makeRelative(worldPosition,state.getValue(HorizontalDirectionalBlock.FACING)))
    }

    public fun addBlockToReplicate(pos: BlockPos){
        val facing = state.getValue(HorizontalDirectionalBlock.FACING)
        val makeRelative = pos.makeRelative(worldPosition, facing)
        blocksToReplicate += makeRelative
        level?.linkBlockAt(linkedTo!!)?.summonGhostBlock(pos,makeRelative,facing)
    }

    public fun removeBlockToReplicate(pos: BlockPos){
        val makeRelative = pos.makeRelative(worldPosition, state.getValue(HorizontalDirectionalBlock.FACING))
        blocksToReplicate -= makeRelative
        level?.linkBlockAt(linkedTo!!)?.removeGhostBlock(makeRelative)
    }

    private fun summonGhostBlock(original: BlockPos,pos: RelativeBlockPos, otherfacing: Direction) {
        val thisfacing = state.getValue(HorizontalDirectionalBlock.FACING)
        val ghostPos = pos.makeAbsolute(worldPosition, thisfacing)
        level?.setBlockAndUpdate(ghostPos,Blocks.GHOST_BLOCK.defaultBlockState())
        level?.getBlockEntity(ghostPos)?.let {
            if(it is GhostBlockEntity) {
                it.target_block = original
                it.own_forward = thisfacing
                it.target_foward = otherfacing
            }
        }
    }

    private fun removeGhostBlock(pos: RelativeBlockPos) {
        level?.removeBlock(pos.makeAbsolute(worldPosition,state.getValue(HorizontalDirectionalBlock.FACING)),false)
    }



    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        linkedTo = tag.get(::linkedTo.name)?.let {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, it).ifError { err ->
                Strangeteleport.LOGGER.error("Error parsing link data : ${err.message()})")
            }.result().orElse(null)
        }
        blocksToReplicate = tag.get(::blocksToReplicate.name)?.let {
            RelativeBlockPos.CODEC.listOf().parse(NbtOps.INSTANCE, it).ifError { err ->
                Strangeteleport.LOGGER.error("Error parsing blocks to replicate : ${err.message()})")
            }.result().orElse(null).toSet()
        } ?: emptySet()

    }


    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        linkedTo?.let {
            tag.put(::linkedTo.name, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, it)
                .ifError { err ->
                    Strangeteleport.LOGGER.error("Error saving link data : ${err.message()})")
                }
                .result().orElse(null))
        }
        tag.put(::blocksToReplicate.name,RelativeBlockPos.CODEC.listOf().encodeStart(NbtOps.INSTANCE,blocksToReplicate.toList())
            .ifError { err ->
                Strangeteleport.LOGGER.error("Error saving blocks to replicate : ${err.message()})")
            }
            .result().orElse(null))
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = CompoundTag().also {
        saveAdditional(it, registries)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

}


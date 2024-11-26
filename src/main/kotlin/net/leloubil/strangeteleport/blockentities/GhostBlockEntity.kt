package net.leloubil.strangeteleport.blockentities

import arrow.core.partially4
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.block.Blocks
import net.leloubil.strangeteleport.blockentities.BlockEntities.GHOST_BLOCK_ENTITY
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import org.apache.logging.log4j.LogManager


data class RelativeVec(val forwardbackward: Double, val updown: Double, val leftright: Double) {
    companion object {
        val CODEC = Vec3.CODEC.xmap(
            { RelativeVec(it.x, it.y, it.z) },
            { Vec3(it.forwardbackward, it.updown, it.leftright) }
        )
    }

    // west = -x
    // east = x
    // up = y
    // down = -y
    // north = z
    // south = -z
    fun makeAbsolute(other: Vec3, direction: Direction): Vec3 {
        return when (direction) {
            Direction.NORTH -> Vec3(other.x + leftright, other.y + updown, other.z - forwardbackward)
            Direction.SOUTH -> Vec3(other.x - leftright, other.y + updown, other.z + forwardbackward)
            Direction.EAST -> Vec3(other.x + forwardbackward, other.y + updown, other.z + leftright)
            Direction.WEST -> Vec3(other.x - forwardbackward, other.y + updown, other.z - leftright)
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
fun Vec3.makeRelative(other: Vec3, direction: Direction): RelativeVec {
    return when (direction) {
        Direction.NORTH -> RelativeVec(other.z - z, y - other.y, x - other.x)
        Direction.SOUTH -> RelativeVec(z - other.z, y - other.y, other.x - x)
        Direction.EAST -> RelativeVec(x - other.x, y - other.y, z - other.z)
        Direction.WEST -> RelativeVec(other.x - x, y - other.y, other.z - z)
        else -> throw IllegalArgumentException("Invalid direction")
    }
}

@EventBusSubscriber(modid = Strangeteleport.MOD_ID)
object GhostBlockEventHandler {
    private val LOGGER = LogManager.getLogger()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onPlayerBlockInteract(event: PlayerInteractEvent.RightClickBlock) {
        if (event.level.getBlockState(event.pos).block != Blocks.GHOST_BLOCK) return
        val ent = event.level.getBlockEntity(event.pos) as? GhostBlockEntity ?: return
        event.useBlock = TriState.FALSE
        event.useItem = TriState.FALSE
        val targetBlock = ent.target_block
        val targetForward = ent.target_foward
        val ownForward = ent.own_forward
        if (event.level.getBlockState(targetBlock).block == Blocks.GHOST_BLOCK) {
            LOGGER.info("Evaded a loop")
            return
        }

        val hitDir = event.hitVec.direction
        val sp = event.entity
        val newDirection = if (hitDir == Direction.UP || hitDir == Direction.DOWN) hitDir else when (targetForward) {
            ownForward -> {
                hitDir
            }

            ownForward.opposite -> {
                hitDir.opposite
            }

            ownForward.clockWise -> {
                hitDir.clockWise
            }

            ownForward.counterClockWise -> {
                hitDir.counterClockWise
            }

            else -> {
                LOGGER.error("Invalid direction")
                return
            }
        }
        // get entities 4 blocks around event.pos.center
        event.level.getEntities(
            null,
            AABB.ofSize(event.pos.center, 5.0, 5.0, 5.0)
        ).filter { it.isAlive }
            .forEach {
            relativeMoveEntity(it, event.pos.center, targetBlock.center, ownForward, targetForward)
        }

        val newHitVec = event.hitVec.withPosition(targetBlock).withDirection(newDirection)
        if (!event.level.isClientSide) {
            event.isCanceled = true
            event.cancellationResult =
                (sp as ServerPlayer).gameMode.useItemOn(sp, event.level, event.itemStack, event.hand, newHitVec)
//            event.cancellationResult = InteractionResult.SUCCESS_SERVER
        } else {

            event.isCanceled = true
            event.cancellationResult = InteractionResult.SUCCESS
        }
        return
    }

    private fun relativeMoveEntity(
        entity: Entity,
        oldPivotPosition: Vec3,
        newPivotPosition: Vec3,
        oldForward: Direction,
        newForward: Direction
    ) {

        val relPos = entity.position().makeRelative(oldPivotPosition, oldForward)
        val newEntityPos = relPos.makeAbsolute(newPivotPosition, newForward)

        val entityRot = entity.rotationVector.y
        val newPlayerRot = when (newForward) {
            oldForward -> {
                entityRot
            }

            oldForward.opposite -> {
                (entityRot - 180) % 360
            }

            oldForward.clockWise -> {
                (entityRot + 90) % 360
            }

            oldForward.counterClockWise -> {
                (entityRot - 90) % 360
            }

            else -> {
                LOGGER.error("Invalid direction")
                return
            }
        }
        val newEntitySpeed =
            entity.deltaMovement.makeRelative(Vec3.ZERO, oldForward).makeAbsolute(Vec3.ZERO, newForward)

        entity.moveTo(newEntityPos.x, newEntityPos.y, newEntityPos.z, newPlayerRot, entity.rotationVector.x)
        entity.deltaMovement = newEntitySpeed
        entity.hurtMarked = true
    }
}


class GhostBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(GHOST_BLOCK_ENTITY, pos, state) {

    private lateinit var _target_block: BlockPos
    private lateinit var _target_foward: Direction
    private lateinit var _own_forward: Direction

    var target_block: BlockPos
        get() = _target_block
        set(value) {
            _target_block = value
            setChanged()
        }

    var target_foward: Direction
        get() = _target_foward
        set(value) {
            _target_foward = value
            setChanged()
        }

    var own_forward: Direction
        get() = _own_forward
        set(value) {
            _own_forward = value
            setChanged()
        }

    public fun hasTarget(): Boolean {
        return this::_target_block.isInitialized
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        val res = tag.get(::target_block.name)?.let {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, it).result().orElse(null)
        }
        if (res != null) {
            _target_block = res
        }

        val res2 = tag.get(::target_foward.name)?.let {
            BlockStateProperties.HORIZONTAL_FACING.codec().parse(NbtOps.INSTANCE, it).result().orElse(null)
        }
        if (res2 != null) {
            target_foward = res2
        }

        val res3 = tag.get(::own_forward.name)?.let {
            BlockStateProperties.HORIZONTAL_FACING.codec().parse(NbtOps.INSTANCE, it).result().orElse(null)
        }
        if (res3 != null) {
            own_forward = res3
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put(::target_block.name, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, target_block).result().orElseThrow())
        tag.put(
            ::target_foward.name,
            BlockStateProperties.HORIZONTAL_FACING.codec().encodeStart(NbtOps.INSTANCE, target_foward).result()
                .orElseThrow()
        )
        tag.put(
            ::own_forward.name,
            BlockStateProperties.HORIZONTAL_FACING.codec().encodeStart(NbtOps.INSTANCE, own_forward).result()
                .orElseThrow()
        )
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = CompoundTag().also {
        saveAdditional(it, registries)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

}

class GhostBlockEntityRenderer(private val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<GhostBlockEntity> {
    override fun render(
        blockEntity: GhostBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if(!blockEntity.hasTarget()) return
        val targetBlock = blockEntity.target_block
        val target_forward = blockEntity.target_foward
        val own_forward = blockEntity.own_forward
        val targetEntity = blockEntity.level!!.getBlockEntity(targetBlock)
        if (targetEntity != null) {
            val rotation = when (target_forward) {
                own_forward -> 0f
                own_forward.opposite -> 180f
                own_forward.clockWise -> 90f
                own_forward.counterClockWise -> -90f
                else -> 0f
            } // y axis
            poseStack.rotateAround(Axis.YP.rotationDegrees(rotation), 0.5f, 0.5f, 0.5f)
//            poseStack.translate(0.5, 0.5, 0.5)
            context.blockEntityRenderDispatcher.getRenderer(targetEntity)
                ?.render(targetEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay)
        }
    }

}

@EventBusSubscriber(modid = Strangeteleport.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object BEREventHandler {

    @SubscribeEvent
    fun registerEntityRenderer(evt: EntityRenderersEvent.RegisterRenderers) {
        evt.registerBlockEntityRenderer(GHOST_BLOCK_ENTITY, ::GhostBlockEntityRenderer)
    }
}

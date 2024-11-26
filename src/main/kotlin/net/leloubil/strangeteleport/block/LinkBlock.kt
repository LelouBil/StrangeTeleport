package net.leloubil.strangeteleport.block

import arrow.core.partially1
import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.blockentities.LinkBlockEntity
import net.leloubil.strangeteleport.blockentities.linkBlockAt
import net.leloubil.strangeteleport.data.PlayerLinkData
import net.leloubil.strangeteleport.data.PlayerSelectBlocksData
import net.leloubil.strangeteleport.data.linkData
import net.leloubil.strangeteleport.data.linkSelectBlocksData
import net.leloubil.strangeteleport.flip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import org.apache.logging.log4j.LogManager


@EventBusSubscriber(modid = Strangeteleport.MOD_ID)
object LinkBlockEventHandler {
    private val LOGGER = LogManager.getLogger()
    @SubscribeEvent()
    fun onPlayerBlockInteract(event: PlayerInteractEvent.RightClickBlock) {
        if(event.level.isClientSide) return
        if(event.level.getBlockState(event.pos).block == Blocks.LINK_BLOCK) {
            return
        }
        val player = event.entity
        val linkBlockPos = player.linkSelectBlocksData.linkBlockPos ?: return
        player.linkSelectBlocksData = PlayerSelectBlocksData.EmptySelectBlocksData
        val blockEntity = event.level.linkBlockAt(linkBlockPos) ?: return
        event.cancellationResult = InteractionResult.SUCCESS_SERVER
        event.useBlock = TriState.FALSE
        event.useItem = TriState.FALSE
        if(event.pos.distChessboard(linkBlockPos) > 3){
            return
        }
        if(blockEntity.containsBlockToReplicate(event.pos)) {
            // removing
            blockEntity.removeBlockToReplicate(event.pos)
            player.displayClientMessage(
                Component.literal("Removed block at ${event.pos} from replication list"),
                true
            )
            return
        }else{
            // adding
            blockEntity.addBlockToReplicate(event.pos)
            player.displayClientMessage(
                Component.literal("Added block at ${event.pos} to replication list"),
                true
            )
            return
        }

    }
}

class LinkBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        private val LOGGER = LogManager.getLogger()
        private val FACING = HorizontalDirectionalBlock.FACING
    }

    private val shape = listOf(
        box(7.0, 7.0, 1.0, 9.0, 9.0, 15.0),
        box(1.0, 7.0, 7.0, 15.0, 9.0, 9.0),
        box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
    ).reduce(Shapes::join.flip().partially1(BooleanOp.OR))

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = shape

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val facing = context.horizontalDirection
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return super.getVisualShape(state, level, pos, context)
    }



    override fun newBlockEntity(pos: BlockPos, state: BlockState) = LinkBlockEntity(pos, state)


    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            LOGGER.info("Player ${player.name} clicked on a link aaa block at $pos")
            val currentEntity = level.linkBlockAt(pos) ?: return InteractionResult.PASS
            if (currentEntity.linkedTo != null) {
                val linkSelectBlocksData = player.linkSelectBlocksData
                if (linkSelectBlocksData.linkBlockPos == null) {
                    player.linkSelectBlocksData = linkSelectBlocksData.copy(linkBlockPos = pos)
                    player.displayClientMessage(
                        Component.literal("Entering selection mode for block at $pos"),
                        true
                    )
                    return InteractionResult.SUCCESS_SERVER
                }
                return InteractionResult.PASS
            }
            val linkData = player.linkData
            if (linkData.previousBlock == null) {
                player.linkData = linkData.copy(previousBlock = pos)
                LOGGER.info("Player ${player.name} set the previous block to $pos")
                player.displayClientMessage(
                    Component.literal("Set the previous block to $pos"),
                    true
                )
                return InteractionResult.SUCCESS_SERVER
            } else {
                if (linkData.previousBlock == pos) {
                    player.displayClientMessage(
                        Component.literal("Cannot link a block to itself"),
                        true
                    )
                    player.linkData = PlayerLinkData.EmptyLinkData
                    return InteractionResult.FAIL
                }
                val prevBlockE = level.linkBlockAt(linkData.previousBlock) ?: return InteractionResult.PASS
                player.linkData = PlayerLinkData.EmptyLinkData
                doLink(player, prevBlockE, currentEntity)
                return InteractionResult.SUCCESS_SERVER
            }
        }
        return InteractionResult.PASS
    }

    private fun doLink(linker: Player, blockEntityA: LinkBlockEntity, blockEntityB: LinkBlockEntity) {

        blockEntityA.linkedTo = blockEntityB.blockPos
        blockEntityB.linkedTo = blockEntityA.blockPos
        linker.displayClientMessage(
            Component.literal("Linked blocks at ${blockEntityA.blockPos} and ${blockEntityB.blockPos}"),
            true
        )
    }


    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        LOGGER.info("Removing link block at $pos")
        if (!level.isClientSide) {
            val blockEntity = level.linkBlockAt(pos) ?: return
            val linkedTo = blockEntity.linkedTo ?: return
            val linkedToEntity = level.linkBlockAt(linkedTo) ?: return
            linkedToEntity.linkedTo = null
        }
        super.onRemove(state, level, pos, newState, movedByPiston)

    }
}


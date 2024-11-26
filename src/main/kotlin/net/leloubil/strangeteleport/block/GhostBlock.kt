package net.leloubil.strangeteleport.block

import net.leloubil.strangeteleport.Strangeteleport
import net.leloubil.strangeteleport.blockentities.GhostBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.BlockModelShaper
import net.minecraft.client.renderer.block.model.BakedOverrides
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.model.data.ModelData
import net.neoforged.neoforge.client.model.data.ModelProperty
import net.neoforged.neoforge.common.util.TriState

class GhostBlock(prop: Properties) : Block(prop), EntityBlock {
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {

        return InteractionResult.SUCCESS
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = GhostBlockEntity(pos, state)

    private fun getTargetBlock(level: BlockGetter, pos: BlockPos): BlockState? {
        val ghostBlockEntity = level.getBlockEntity(pos) as? GhostBlockEntity ?: return null
        if (!ghostBlockEntity.hasTarget()) return null
        val targetPos = ghostBlockEntity.target_block
        return level.getBlockState(targetPos)
    }

    override fun getInteractionShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape {
        return getTargetBlock(level, pos)?.getInteractionShape(level, pos) ?: Shapes.empty()
    }


    override fun hasDynamicLightEmission(state: BlockState): Boolean {
        return true
    }

    override fun getLightEmission(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        return getTargetBlock(level, pos)?.getLightEmission(level, pos) ?: 0
    }


    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        val ent = level.getBlockEntity(pos) as? GhostBlockEntity ?: return
        if (!ent.hasTarget()) return
        var targetBlock = getTargetBlock(level, pos)
        if (targetBlock?.getNullableValue(HorizontalDirectionalBlock.FACING) != null) {
            val originalFacing = targetBlock.getValue(HorizontalDirectionalBlock.FACING)
            targetBlock = targetBlock.setValue(
                HorizontalDirectionalBlock.FACING, when (ent.target_foward) {
                    ent.own_forward -> originalFacing
                    ent.own_forward.opposite -> originalFacing.opposite
                    ent.own_forward.clockWise -> originalFacing.counterClockWise
                    ent.own_forward.counterClockWise -> originalFacing.clockWise
                    else -> originalFacing
                }
            )
        }
        targetBlock?.block?.animateTick(targetBlock, level, pos, random)
    }


    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return getTargetBlock(level, pos)?.getVisualShape(level, pos, context) ?: Shapes.empty()
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return getTargetBlock(level, pos)?.getCollisionShape(level, pos, context) ?: Shapes.empty()
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return getTargetBlock(level, pos)?.getShape(level, pos, context) ?: Shapes.empty()
    }

    override fun getOcclusionShape(state: BlockState): VoxelShape {
        return Shapes.empty()
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return super.getRenderShape(state)
    }

}

@EventBusSubscriber(modid = Strangeteleport.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object GhostBlockListener {

    @SubscribeEvent
    fun onModelBake(event: ModelEvent.ModifyBakingResult) {
        for (possibleState in Blocks.GHOST_BLOCK.stateDefinition.possibleStates) {
            val mrl = BlockModelShaper.stateToModelLocation(possibleState)
            val existingModel = event.models[mrl]!!
            val wrappedModel = GhostBlockBakedModel(existingModel)
            event.models[mrl] = wrappedModel
        }
    }

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        ItemBlockRenderTypes.setRenderLayer(Blocks.GHOST_BLOCK, RenderType.solid())
    }
}


class GhostBlockBakedModel(private val notCamouflaged: BakedModel) : BakedModel {
    companion object {
        val COPIED_BLOCK: ModelProperty<BlockState> = ModelProperty()
        val COPIED_DATA: ModelProperty<ModelData> = ModelProperty()
        val OWN_FORWARD: ModelProperty<Direction> = ModelProperty()
        val TARGET_FORWARD: ModelProperty<Direction> = ModelProperty()
    }

    data class ActualModel(val model: BakedModel, val state: BlockState, val data: ModelData)

    private fun getActualModel(data: ModelData): ActualModel {
        val notCamouflaged = ActualModel(this.notCamouflaged, Blocks.GHOST_BLOCK.defaultBlockState(), data)
        val copiedBlock = data.get(COPIED_BLOCK) ?: return notCamouflaged
        val ownForward = data.get(OWN_FORWARD) ?: return notCamouflaged
        val targetForward = data.get(TARGET_FORWARD) ?: return notCamouflaged
        val mc = Minecraft.getInstance()
        val renderer = mc.blockRenderer
        val blockModel = renderer.getBlockModel(copiedBlock)
//        val context = ItemDisplayContext.valueOf("strangeteleport:ghost_block_context")
//        val transform = ItemTransform(Vector3f(45f, 0f, 45f), Vector3f(0f, 0f, 0f), Vector3f(1f, 1f, 1f))
        return ActualModel(blockModel, copiedBlock, data.get(COPIED_DATA) ?: return notCamouflaged)
    }


    override fun getQuads(state: BlockState?, direction: Direction?, random: RandomSource): MutableList<BakedQuad> =
        throw UnsupportedOperationException()

    override fun getQuads(
        state: BlockState?,
        side: Direction?,
        rand: RandomSource,
        data: ModelData,
        renderType: RenderType?
    ): MutableList<BakedQuad> {
        val actualModel = getActualModel(data)
        return actualModel.model.getQuads(actualModel.state, side, rand, actualModel.data, renderType)
    }

    override fun useAmbientOcclusion(): Boolean = notCamouflaged.useAmbientOcclusion()

    override fun getParticleIcon(): TextureAtlasSprite = notCamouflaged.getParticleIcon()

    override fun getParticleIcon(data: ModelData): TextureAtlasSprite {
        val actualModel = getActualModel(data)
        return actualModel.model.getParticleIcon(actualModel.data)
    }

    override fun isGui3d(): Boolean = notCamouflaged.isGui3d

    override fun usesBlockLight(): Boolean = notCamouflaged.usesBlockLight()

    override fun isCustomRenderer(): Boolean = notCamouflaged.isCustomRenderer

    override fun overrides(): BakedOverrides = notCamouflaged.overrides()

    override fun useAmbientOcclusion(state: BlockState, data: ModelData, renderType: RenderType): TriState {
        val actualModel = getActualModel(data)
        return actualModel.model.useAmbientOcclusion(actualModel.state, actualModel.data, renderType)
    }

    override fun getTransforms(): ItemTransforms = notCamouflaged.getTransforms()

    override fun getModelData(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        modelData: ModelData
    ): ModelData {
        val ghostBlockEntity = level.getBlockEntity(pos) as? GhostBlockEntity
            ?: return notCamouflaged.getModelData(level, pos, state, modelData)
        if (!ghostBlockEntity.hasTarget()) return notCamouflaged.getModelData(level, pos, state, modelData)
        val targetPos = ghostBlockEntity.target_block
        val targetForward = ghostBlockEntity.target_foward
        val ownForward = ghostBlockEntity.own_forward
        var targetBlockState = level.getBlockState(targetPos)
        val facing = targetBlockState.getNullableValue(HorizontalDirectionalBlock.FACING)
        if (facing != null) {
            val newFacing = when (targetForward) {
                ownForward -> facing
                ownForward.opposite -> facing.opposite
                ownForward.clockWise -> facing.counterClockWise
                ownForward.counterClockWise -> facing.clockWise
                else -> facing
            }
            targetBlockState = targetBlockState.setValue(HorizontalDirectionalBlock.FACING, newFacing)
        }

        return ModelData.builder()
            .with(COPIED_BLOCK, targetBlockState)
            .with(OWN_FORWARD, ownForward)
            .with(TARGET_FORWARD, targetForward)
            .with(COPIED_DATA, level.getModelData(targetPos))
            .build()
    }

}

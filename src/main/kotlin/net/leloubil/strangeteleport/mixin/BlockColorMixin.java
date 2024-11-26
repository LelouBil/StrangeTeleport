package net.leloubil.strangeteleport.mixin;

import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockColors.class)
public class BlockColorMixin {

//    @ModifyArg(method = "getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I", at = @At(value = "HEAD"), index = 1)
//    private BlockState getColorEdited(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
//        // Do something
//        if (state.getBlock() == Blocks.INSTANCE.getGHOST_BLOCK()){
//            BlockEntity blockEntity = level.getBlockEntity(pos);
//            if (blockEntity instanceof GhostBlockEntity) {
//                GhostBlockEntity ghostBlockEntity = (GhostBlockEntity) blockEntity;
//                if(ghostBlockEntity.hasTarget()){
//                    return level.getBlockState(ghostBlockEntity.getTarget_block());
//                }
//            }
//        }
//        return state;
//    }
}

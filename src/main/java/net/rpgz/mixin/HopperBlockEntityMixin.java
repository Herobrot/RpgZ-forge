package net.rpgz.mixin;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.rpgz.access.IInventoryAccess;
import net.rpgz.init.ConfigInit;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Unique
    private static int rpgZ_forge$ticking = 0;

    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getItemsAtAndAbove(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Ljava/util/List;"), cancellable = true)
    private static void extractMixin(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> info) {
        if (ConfigInit.get().hopper_extracting) {
            rpgZ_forge$ticking++;
            if (rpgZ_forge$ticking >= 20) {
                BlockPos pos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
                AABB box = new AABB(pos).expandTowards(0.0D, 1.0D, 0.0D);
                List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box,
                        EntitySelector.NO_SPECTATORS);
                if (!list.isEmpty()) {
                    for (LivingEntity livingEntity : list) {
                        if (livingEntity.isDeadOrDying()) {
                            if (((IInventoryAccess) livingEntity).rpgZ_forge$getDropsInventory() != null) {
                                Direction direction = Direction.DOWN;
                                info.setReturnValue(
                                        !isEmptyContainer(((IInventoryAccess) livingEntity).rpgZ_forge$getDropsInventory(), direction) && Objects.requireNonNull(getSlots(((IInventoryAccess) livingEntity).rpgZ_forge$getDropsInventory(),
                                                direction)).anyMatch((i) -> tryTakeInItemFromSlot(hopper,
                                                        ((IInventoryAccess) livingEntity).rpgZ_forge$getDropsInventory(), i,
                                                        direction)));
                            }
                        }
                    }
                }
                rpgZ_forge$ticking = 0;
            }
        }
    }

    @Shadow
    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container inventory, int slot, Direction side) {
        return false;
    }

    @Shadow
    private static boolean isEmptyContainer(Container inv, Direction facing) {
        return false;
    }

    @Shadow
    private static IntStream getSlots(Container inventory, Direction side) {
        return null;
    }
}
package net.rpgz.access;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public interface IInventoryAccess {

    SimpleContainer rpgZ_forge$getDropsInventory();
    void rpgZ_forge$setDropsInventory(SimpleContainer inventory);

    void rpgZ_forge$addingInventoryItems(ItemStack stack);
}
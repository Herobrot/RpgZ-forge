package net.rpgz.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class InGameHudMixin {
  @Shadow
  @Final
  @Mutable
  protected final Minecraft minecraft;

  public InGameHudMixin(Minecraft mc) {
    this.minecraft = mc;
  }

  @Inject(method = "render", at = @At(value = "TAIL"))
  private void renderIngameGuiMixin(GuiGraphics pGuiGraphics, float f, CallbackInfo info) {
    this.rpgZ_forge$renderLootBag(pGuiGraphics);
    System.out.println("Graphics");
  }

  @Unique
  private void rpgZ_forge$renderLootBag(GuiGraphics guiComponent) {
    if (this.minecraft.hitResult != null && this.minecraft.hitResult.getType() == HitResult.Type.ENTITY) {
      Entity entity = ((EntityHitResult) this.minecraft.hitResult).getEntity();
      if (entity instanceof Mob deadBody) {
          if (deadBody.deathTime > 20) {
          int scaledWidth = this.minecraft.getWindow().getGuiScaledWidth();
          int scaledHeight = this.minecraft.getWindow().getGuiScaledHeight();
          guiComponent.blit(new ResourceLocation("rpgz:textures/sprite/loot_bag.png"), (scaledWidth / 2), (scaledHeight / 2) - 16, 0.0F, 0.0F, 16, 16, 16,
              16);
        }
      }
    }

  }

}
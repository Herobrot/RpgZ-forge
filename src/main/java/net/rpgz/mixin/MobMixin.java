package net.rpgz.mixin;

import java.util.List;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rpgz.access.IInventoryAccess;
import net.rpgz.init.ConfigInit;
import net.rpgz.init.TagInit;
import net.rpgz.ui.LivingEntityScreenHandler;

@Mixin(Mob.class)
@SuppressWarnings("resource")
public abstract class MobMixin extends LivingEntity implements IInventoryAccess {

	@Unique
	SimpleContainer rpgZ_forge$dropInventory = new SimpleContainer(9);

	public MobMixin(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}

	// -------------------------------------------------------------------------
	// Helpers de config — evita repetir .get() por todo el código
	// -------------------------------------------------------------------------

	@Unique private boolean cfg_drop_unlooted()             { return ConfigInit.get().drop_unlooted; }
	@Unique private int     cfg_drop_after_ticks()          { return ConfigInit.get().drop_after_ticks; }
	@Unique private int     cfg_despawn_corps_after_ticks() { return ConfigInit.get().despawn_corps_after_ticks; }
	@Unique private boolean cfg_despawn_immediately()       { return ConfigInit.get().despawn_immediately_when_empty; }
	@Unique private boolean cfg_surfacing_in_water()        { return ConfigInit.get().surfacing_in_water; }

	@Unique
	private boolean rpgZ_forge$isExcludedByConfig() {
		String entityId = this.getType().toString()
				.replace("entity.", "")
				.replace(".", ":");
		return ConfigInit.get().excluded_entities.contains(entityId);
	}

	// -------------------------------------------------------------------------
	// Lógica del mod
	// -------------------------------------------------------------------------

	@Override
	public ItemEntity spawnAtLocation(@NotNull ItemStack pItem) {
		if (this.isDeadOrDying()) {
			this.rpgZ_forge$addingInventoryItems(pItem);
			return null;
		}
		return super.spawnAtLocation(pItem);
	}

	@Override
	public void die(@NotNull DamageSource pDamageSource) {
		if (this.isVehicle()) {
			for (int i = 0; i < this.getPassengers().size(); i++) {
				this.getPassengers().get(i).removeVehicle();
			}
		}
		super.die(pDamageSource);
	}

	/** Detiene la rotación de la cabeza tras la muerte. */
	@Inject(method = "tickHeadTurn", at = @At("HEAD"), cancellable = true)
	public void updateDistance(float bodyRotation, float headRotation, CallbackInfoReturnable<Float> info) {
		if (this.deathTime > 0) {
			info.setReturnValue(0.0F);
		}
	}

	/** Evita que el cadáver se queme bajo el sol. */
	@Inject(method = "isSunBurnTick()Z", at = @At("HEAD"), cancellable = true)
	private void isInDaylightMixin(CallbackInfoReturnable<Boolean> info) {
		if (this.isDeadOrDying()) {
			info.setReturnValue(false);
		}
	}

	@Override
	public void rpgZ_forge$addingInventoryItems(ItemStack stack) {
		if (!stack.isEmpty() && !this.level().isClientSide)
			this.rpgZ_forge$dropInventory.addItem(stack);
	}

	@Override
	public @NotNull InteractionResult interactAt(@NotNull Player player, @NotNull Vec3 hitPos, @NotNull InteractionHand hand) {
		if (this.deathTime > 20) {
			if (!this.level().isClientSide) {
				// Pala: recoge todo y elimina el cadáver
				if (player.getItemInHand(hand).getItem() instanceof ShovelItem) {
					if (!this.rpgZ_forge$dropInventory.isEmpty())
						for (int i = 0; i < this.rpgZ_forge$dropInventory.getContainerSize(); i++)
							player.getInventory().placeItemBackInInventory(this.rpgZ_forge$dropInventory.getItem(i));
					this.rpgZ_forge$dropInventory.clearContent();
					if (!cfg_despawn_immediately()) {
						this.rpgZ_forge$despawnParticlesServer();
						this.remove(RemovalReason.KILLED);
					}
					return InteractionResult.SUCCESS;
				}
				// Inventario no vacío: shift = recoge todo, click normal = abre UI
				if (!this.rpgZ_forge$dropInventory.isEmpty()) {
					if (player.isShiftKeyDown()) {
						for (int i = 0; i < this.rpgZ_forge$dropInventory.getContainerSize(); i++)
							player.getInventory().placeItemBackInInventory(this.rpgZ_forge$dropInventory.getItem(i));
						this.rpgZ_forge$dropInventory.clearContent();
					} else {
						player.openMenu(new SimpleMenuProvider(
								(syncId, inv, p) -> new LivingEntityScreenHandler(syncId, p.getInventory(), this.rpgZ_forge$dropInventory),
								Component.literal("")));
					}
					return InteractionResult.SUCCESS;
				}
			}
            return InteractionResult.SUCCESS;
		}
		return super.interactAt(player, hitPos, hand);
	}

	@Unique
	private void rpgZ_forge$despawnParticlesServer() {
		for (int i = 0; i < 20; ++i) {
			double d = this.random.nextGaussian() * 0.025D;
			double e = this.random.nextGaussian() * 0.025D;
			double f = this.random.nextGaussian() * 0.025D;
			double x = Mth.nextDouble(random, this.getBoundingBox().minX - 0.5D, this.getBoundingBox().maxX) + 0.5D;
			double y = Mth.nextDouble(random, this.getBoundingBox().minY,         this.getBoundingBox().maxY) + 0.5D;
			double z = Mth.nextDouble(random, this.getBoundingBox().minZ - 0.5D, this.getBoundingBox().maxZ) + 0.5D;
			((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, x, y, z, 0, d, e, f, 0.01D);
		}
	}

	@Override
	public SimpleContainer rpgZ_forge$getDropsInventory() { return rpgZ_forge$dropInventory; }

	@Override
	public void rpgZ_forge$setDropsInventory(SimpleContainer inventory) { this.rpgZ_forge$dropInventory = inventory; }

	// -------------------------------------------------------------------------
	// tickDeath
	// -------------------------------------------------------------------------

	@Unique
	private int rpgZ_forge$forceDroppedAt = -1;

	@Unique
	private void rpgZ_forge$forceDropItems() {
		List<ItemStack> items = this.rpgZ_forge$dropInventory.removeAllItems();
		for (ItemStack stack : items) {
			// Llamar super directamente, saltando el override del mixin
			super.spawnAtLocation(stack);
		}
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;

		if (this.deathTime == 1) {
			if (this.isOnFire()) this.clearFire();
			if (this.getVehicle() != null) this.stopRiding();
		}

		/*
		if (this.deathTime >= 20 && !level().isClientSide) {

			// LOG: estado cada 20 ticks para no saturar
			if (this.deathTime % 20 == 0) {
				System.out.println("[RpgZ-DEBUG] deathTime=" + this.deathTime
						+ " | inventoryEmpty=" + this.rpgZ_forge$dropInventory.isEmpty()
						+ " | drop_unlooted=" + cfg_drop_unlooted()
						+ " | drop_after_ticks=" + cfg_drop_after_ticks()
						+ " | despawn_after=" + cfg_despawn_corps_after_ticks()
						+ " | despawn_immediately=" + cfg_despawn_immediately()
						+ " | forceDroppedAt=" + rpgZ_forge$forceDroppedAt);
			}
		}
		*/

		if (this.deathTime >= 20) {
			AABB newBoundingBox = new AABB(
					this.getX() - (this.getBbWidth() / 3.0F),
					this.getY() - (this.getBbWidth() / 3.0F),
					this.getZ() - (this.getBbWidth() / 3.0F),
					this.getX() + (this.getBbWidth() / 1.5F),
					this.getY() + (this.getBbWidth() / 1.5F),
					this.getZ() + (this.getBbWidth() / 1.5F));

			boolean isSmallOrWide = (this.getDimensions(Pose.STANDING).height < 1.0F
					&& this.getDimensions(Pose.STANDING).width < 1.0F)
					|| (this.getDimensions(Pose.STANDING).width / this.getDimensions(Pose.STANDING).height) > 1.395F;

			if (isSmallOrWide) {
				this.setBoundingBox(newBoundingBox);
			} else {
				this.setBoundingBox(newBoundingBox.move(
						this.calculateViewVector(0F, this.yBodyRot).yRot(-30.0F)));
			}

			if (!level().isClientSide) {
				AABB box = this.getBoundingBox();
				BlockPos blockPos = BlockPos.containing(box.minX + 0.001D, box.minY + 0.001D, box.minZ + 0.001D).above();
				BlockPos blockPos2 = BlockPos.containing(box.maxX - 0.001D, box.maxY - 0.001D, box.maxZ - 0.001D);

				AABB checkBox = new AABB(box.maxX, box.maxY, box.maxZ,
						box.maxX + 0.001D, box.maxY + 0.001D, box.maxZ + 0.001D);
				AABB checkBoxTwo = new AABB(box.minX, box.maxY, box.minZ,
						box.minX + 0.001D, box.maxY + 0.001D, box.minZ + 0.001D);
				AABB checkBoxThree = new AABB(
						box.maxX - (box.getXsize() / 3D), box.maxY, box.maxZ - (box.getZsize() / 3D),
						box.maxX + 0.001D - (box.getXsize() / 3D), box.maxY + 0.001D, box.maxZ + 0.001D - (box.getZsize() / 3D));

				if (this.level().hasChunksAt(blockPos, blockPos2) && !this.rpgZ_forge$dropInventory.isEmpty()) {
					boolean insideBlock = rpgZ_forge$isInsideBlock(checkBox, checkBoxTwo, checkBoxThree);
					boolean forceDrop = this.isBaby()
							|| (cfg_drop_unlooted() && this.deathTime > cfg_drop_after_ticks());
					boolean excluded = this.getType().is(TagInit.EXCLUDED_ENTITIES) || rpgZ_forge$isExcludedByConfig();

					if (insideBlock || forceDrop || excluded) {
						this.rpgZ_forge$forceDropItems();
						// Registrar el tick en que se hizo el drop forzado
						if (forceDrop || excluded) {
							rpgZ_forge$forceDroppedAt = this.deathTime;
						}
					}
				}
			}
		}

		boolean recentlyDropped = rpgZ_forge$forceDroppedAt >= 0
				&& this.deathTime < rpgZ_forge$forceDroppedAt + 20;

		boolean emptyAndImmediate = !this.level().isClientSide
				&& this.deathTime >= 20
				&& this.rpgZ_forge$dropInventory.isEmpty()
				&& cfg_despawn_immediately()
				&& !recentlyDropped;

		boolean timedOut = this.deathTime >= cfg_despawn_corps_after_ticks();

		if (emptyAndImmediate || timedOut) {
			if (!this.level().isClientSide)
				this.rpgZ_forge$despawnParticlesServer();
			this.remove(RemovalReason.KILLED);
		}
	}


	/** Comprueba si el cadáver está dentro de un bloque sólido usando tres puntos de muestra. */
	@Unique
	private boolean rpgZ_forge$isInsideBlock(AABB checkBox, AABB checkBoxTwo, AABB checkBoxThree) {
		boolean one   = !StreamSupport.stream(this.level().getBlockCollisions(this, checkBox).spliterator(),   false).allMatch(VoxelShape::isEmpty);
		boolean two   = !StreamSupport.stream(this.level().getBlockCollisions(this, checkBoxTwo).spliterator(), false).allMatch(VoxelShape::isEmpty);
		boolean three = !StreamSupport.stream(this.level().getBlockCollisions(this, checkBoxThree).spliterator(), false).allMatch(VoxelShape::isEmpty);
		return (one || three) && (two || three);
	}

	// -------------------------------------------------------------------------
	// aiStep — controla la física del cadáver
	// -------------------------------------------------------------------------

	@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
	private void livingTickMixin(CallbackInfo info) {
		if (this.deathTime <= 19) return;

		AABB box = this.getBoundingBox();
		BlockPos blockPos = BlockPos.containing(box.getCenter().x(), box.minY, box.getCenter().z());

		if (this.level().getBlockState(blockPos).isAir()) {
			// Caída en el aire
			if ((LivingEntity) this instanceof FlyingMob) {
				this.setPosRaw(this.getX(), this.getY() - 0.25D, this.getZ());
			} else if (this.getDeltaMovement().y > 0) {
				double dy = Math.min(this.getDeltaMovement().y, 0.8D);
				this.setPosRaw(this.getX(), this.getY() - dy, this.getZ());
			} else if (this.getDeltaMovement().y < 0) {
				double dy = Math.max(this.getDeltaMovement().y, -0.8D);
				double extra = this.getDeltaMovement().y > -0.2D ? -0.4D : 0.0D;
				this.setPosRaw(this.getX(), this.getY() + dy + extra, this.getZ());
			} else {
				this.setPosRaw(this.getX(), this.getY() - 0.1D, this.getZ());
			}
		} else {
			// Flotación en agua
			if (this.level().containsAnyLiquid(box.move(0.0D, box.getYsize(), 0.0D))) {
				if (cfg_surfacing_in_water()) {
					this.setPosRaw(this.getX(), this.getY() + 0.03D, this.getZ());
				}
				if (this.canStandOnFluid(this.level().getFluidState(this.blockPosition()))) {
					this.setPosRaw(this.getX(), this.getY() + 0.03D, this.getZ());
				} else if (this.level().containsAnyLiquid(box.move(0.0D, -box.getYsize() + (box.getYsize() / 5), 0.0D))
						&& !cfg_surfacing_in_water()) {
					this.setPosRaw(this.getX(), this.getY() - 0.05D, this.getZ());
				}
			}
		}

		info.cancel();
	}
}
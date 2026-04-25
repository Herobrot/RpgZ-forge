package net.rpgz.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundInit {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "rpgz");

    public static final RegistryObject<SoundEvent> LOOT_SOUND_EVENT =
            SOUND_EVENTS.register("loot", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation("rpgz", "loot")));

    public static final RegistryObject<SoundEvent> COIN_LOOT_SOUND_EVENT =
            SOUND_EVENTS.register("coin_loot", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation("rpgz", "coin_loot")));
}
package net.rpgz.forge;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.rpgz.config.RpgzConfig;
import net.rpgz.init.ConfigInit;
import net.rpgz.init.SoundInit;
import net.rpgz.init.TagInit;

@Mod(value = Rpgz.MOD_ID)
public class Rpgz {

	public static Rpgz instance;
	public static final String MOD_ID = "rpgz";
	public static final String INVENTORY_KEY = "UnionInventory";
	private static final String NETWORK_PROTOCOL_VERSION = "1";
	public static boolean debugMode = false;

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			location("main"),
			() -> NETWORK_PROTOCOL_VERSION,
			NETWORK_PROTOCOL_VERSION::equals,
			NETWORK_PROTOCOL_VERSION::equals
	);

	public static void debug(String message) {
		if (debugMode) System.out.println("[RpgZ-Forge] " + message);
	}

	public static void warn(String message) {
		if (debugMode) System.out.println("[RpgZ-Forge] " + message);
	}

	public static boolean disableConfig() { return false; }
	public static boolean drawMainMenuButton() { return true; }

	public Rpgz() {
		instance = this;
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ConfigInit.init();
		TagInit.init();

		SoundInit.SOUND_EVENTS.register(modEventBus);

		modEventBus.addListener(this::setup);
		modEventBus.addListener(this::clientSetup);
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void setup(final FMLCommonSetupEvent event) {}

	private void clientSetup(final FMLClientSetupEvent event) {
		ModList.get().getModContainerById(MOD_ID).ifPresent(container ->
				container.registerExtensionPoint(
						ConfigScreenHandler.ConfigScreenFactory.class,
						() -> new ConfigScreenHandler.ConfigScreenFactory(
								(mc, screen) -> AutoConfig.getConfigScreen(RpgzConfig.class, screen).get()
						)
				)
		);
	}

	public static ResourceLocation location(String name) {
		return new ResourceLocation(MOD_ID, name);
	}
}
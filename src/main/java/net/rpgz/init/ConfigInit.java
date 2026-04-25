package net.rpgz.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.rpgz.config.RpgzConfig;

public class ConfigInit {

  public static void init() {
    AutoConfig.register(RpgzConfig.class, GsonConfigSerializer::new);
  }

  public static RpgzConfig get() {
    return AutoConfig.getConfigHolder(RpgzConfig.class).getConfig();
  }
}
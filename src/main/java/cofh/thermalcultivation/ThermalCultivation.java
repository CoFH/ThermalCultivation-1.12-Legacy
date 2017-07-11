package cofh.thermalcultivation;

import cofh.CoFHCore;
import cofh.core.init.CoreProps;
import cofh.core.util.ConfigHandler;
import cofh.thermalcultivation.init.TCBlocks;
import cofh.thermalcultivation.init.TCItems;
import cofh.thermalcultivation.init.TCProps;
import cofh.thermalcultivation.proxy.Proxy;
import cofh.thermalfoundation.ThermalFoundation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod (modid = ThermalCultivation.MOD_ID, name = ThermalCultivation.MOD_NAME, version = ThermalCultivation.VERSION, dependencies = ThermalCultivation.DEPENDENCIES, updateJSON = ThermalCultivation.UPDATE_URL)
public class ThermalCultivation {

	public static final String MOD_ID = "thermalcultivation";
	public static final String MOD_NAME = "Thermal Cultivation";

	public static final String VERSION = "0.1.0";
	public static final String VERSION_MAX = "1.0.0";
	public static final String VERSION_GROUP = "required-after:" + MOD_ID + "@[" + VERSION + "," + VERSION_MAX + ");";
	public static final String UPDATE_URL = "https://raw.github.com/cofh/version/master/" + MOD_ID + "_update.json";

	public static final String DEPENDENCIES = CoFHCore.VERSION_GROUP + ThermalFoundation.VERSION_GROUP;
	public static final String MOD_GUI_FACTORY = "cofh.thermalcultivation.gui.GuiConfigTCFactory";

	@Instance (MOD_ID)
	public static ThermalCultivation instance;

	@SidedProxy (clientSide = "cofh.thermalcultivation.proxy.ProxyClient", serverSide = "cofh.thermalcultivation.proxy.Proxy")
	public static Proxy proxy;

	public static final Logger LOG = LogManager.getLogger(MOD_ID);
	public static final ConfigHandler CONFIG = new ConfigHandler(VERSION);
	public static final ConfigHandler CONFIG_CLIENT = new ConfigHandler(VERSION);

	public static CreativeTabs tabCommon;

	public ThermalCultivation() {

		super();
	}

	/* INIT */
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		CONFIG.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/common.cfg"), true));
		CONFIG_CLIENT.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/client.cfg"), true));

		TCProps.preInit();

		TCBlocks.preInit();
		TCItems.preInit();

		/* Register Handlers */
		registerHandlers();

		proxy.preInit(event);
	}

	@EventHandler
	public void initialize(FMLInitializationEvent event) {

		proxy.initialize(event);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		proxy.postInit(event);
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event) {

		CONFIG.cleanUp(false, true);
		CONFIG_CLIENT.cleanUp(false, true);

		LOG.info(MOD_NAME + ": Load Complete.");
	}

	/* HELPERS */
	private void registerHandlers() {

		MinecraftForge.EVENT_BUS.register(proxy);
	}

}

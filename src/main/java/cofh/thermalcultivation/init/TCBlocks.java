package cofh.thermalcultivation.init;

import cofh.core.util.core.IInitializer;
import cofh.thermalcultivation.block.BlockSoil;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class TCBlocks {

	public static final TCBlocks INSTANCE = new TCBlocks();

	private TCBlocks() {

	}

	public static void preInit() {

		blockSoil = new BlockSoil();

		initList.add(blockSoil);

		for (IInitializer init : initList) {
			init.preInit();
		}
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/* EVENT HANDLING */
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {

		for (IInitializer init : initList) {
			init.initialize();
		}
	}

	private static ArrayList<IInitializer> initList = new ArrayList<>();

	/* REFERENCES */
	public static BlockSoil blockSoil;

}

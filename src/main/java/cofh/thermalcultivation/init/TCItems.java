package cofh.thermalcultivation.init;

import cofh.core.util.core.IInitializer;
import cofh.thermalcultivation.item.ItemScythe;
import cofh.thermalcultivation.item.ItemSeedBag;
import cofh.thermalcultivation.item.ItemWateringCan;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class TCItems {

	public static final TCItems INSTANCE = new TCItems();

	private TCItems() {

	}

	public static void preInit() {

		itemWateringCan = new ItemWateringCan();
		itemSeedBag = new ItemSeedBag();
		itemScythe = new ItemScythe();

		initList.add(itemWateringCan);
		initList.add(itemSeedBag);
		initList.add(itemScythe);

		for (IInitializer init : initList) {
			init.initialize();
		}
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}

	/* EVENT HANDLING */
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {

		for (IInitializer init : initList) {
			init.register();
		}
	}

	private static ArrayList<IInitializer> initList = new ArrayList<>();

	/* REFERENCES */
	public static ItemWateringCan itemWateringCan;
	public static ItemSeedBag itemSeedBag;
	public static ItemScythe itemScythe;

}

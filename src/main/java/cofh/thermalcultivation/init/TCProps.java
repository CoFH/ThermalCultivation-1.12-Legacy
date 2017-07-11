package cofh.thermalcultivation.init;

import cofh.core.gui.CreativeTabCore;
import cofh.thermalcultivation.ThermalCultivation;
import cofh.thermalfoundation.item.ItemFertilizer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TCProps {

	private TCProps() {

	}

	public static void preInit() {

		configCommon();
		configClient();
	}

	/* HELPERS */
	private static void configCommon() {

		String category;
		String comment;
	}

	private static void configClient() {

		/* CREATIVE TABS */
		ThermalCultivation.tabCommon = new CreativeTabCore("thermalcultivation") {

			@Override
			@SideOnly (Side.CLIENT)
			public ItemStack getIconItemStack() {

				return ItemFertilizer.fertilizerRich;
			}

		};
	}

}

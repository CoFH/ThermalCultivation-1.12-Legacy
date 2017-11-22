package cofh.thermalcultivation.init;

import cofh.core.gui.CreativeTabCore;
import cofh.thermalcultivation.ThermalCultivation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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

				ItemStack iconStack = new ItemStack(TCItems.itemWateringCan, 1, 1);
				iconStack.setTagCompound(new NBTTagCompound());
				iconStack.getTagCompound().setBoolean("CreativeTab", true);

				return iconStack;
			}

		};
	}

}

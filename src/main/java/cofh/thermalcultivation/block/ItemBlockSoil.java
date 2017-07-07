package cofh.thermalcultivation.block;

import cofh.core.block.ItemBlockCore;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalcultivation.block.BlockSoil.Type;
import net.minecraft.block.Block;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

public class ItemBlockSoil extends ItemBlockCore {

	public ItemBlockSoil(Block block) {

		super(block);
		setHasSubtypes(true);
		setMaxDamage(0);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {

		return "tile.thermalcultivation.soil." + Type.byMetadata(ItemHelper.getItemDamage(stack)).getName() + ".name";
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {

		return Type.byMetadata(ItemHelper.getItemDamage(stack)).getRarity();
	}

}
